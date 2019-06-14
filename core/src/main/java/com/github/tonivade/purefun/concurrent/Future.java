/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import static com.github.tonivade.purefun.Function1.cons;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.github.tonivade.purefun.CheckedProducer;
import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Filterable;
import com.github.tonivade.purefun.FlatMap1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Holder;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

public interface Future<T> extends FlatMap1<Future.µ, T>, Holder<T>, Filterable<T> {

  Executor DEFAULT_EXECUTOR = Executors.newCachedThreadPool();

  final class µ implements Kind {}

  Try<T> await();
  Try<T> await(Duration timeout);

  void cancel(boolean mayInterruptThread);

  boolean isCompleted();
  boolean isCancelled();

  default boolean isSuccess() {
    return await().isSuccess();
  }

  default boolean isFailure() {
    return await().isFailure();
  }

  Future<T> onSuccess(Consumer1<T> callback);
  Future<T> onFailure(Consumer1<Throwable> callback);
  Future<T> onComplete(Consumer1<Try<T>> callback);

  @Override
  <R> Future<R> map(Function1<T, R> mapper);

  @Override
  <R> Future<R> flatMap(Function1<T, ? extends Higher1<Future.µ, R>> mapper);

  <R> Future<R> andThen(Future<R> next);

  @Override
  Future<T> filter(Matcher1<T> matcher);

  Future<T> orElse(Future<T> other);

  @Override
  default T get() {
    return await().getOrElseThrow(NoSuchElementException::new);
  }

  default T getOrElse(T value) {
    return getOrElse(Producer.cons(value));
  }

  default T getOrElse(Producer<T> value) {
    return await().getOrElse(value);
  }

  default <X extends Throwable> T getOrElseThrow(Producer<X> producer) throws X {
    return await().getOrElseThrow(producer);
  }

  default Throwable getCause() {
    return await().getCause();
  }

  @Override
  @SuppressWarnings("unchecked")
  default <V> Future<V> flatten() {
    return flatMap(value -> {
      try {
        return (Future<V>) value;
      } catch (ClassCastException e) {
        return Future.failure(new UnsupportedOperationException(e));
      }
    });
  }

  Future<T> recover(Function1<Throwable, T> mapper);

  <X extends Throwable> Future<T> recoverWith(Class<X> type, Function1<X, T> mapper);

  <U> Future<U> fold(Function1<Throwable, U> failureMapper, Function1<T, U> successMapper);

  default CompletableFuture<T> toCompletableFuture() {
    CompletableFuture<T> completableFuture = new CompletableFuture<>();
    onSuccess(completableFuture::complete);
    onFailure(completableFuture::completeExceptionally);
    return completableFuture;
  }

  FutureModule getModule();

  static <T> Future<T> success(T value) {
    return success(DEFAULT_EXECUTOR, value);
  }

  static <T> Future<T> success(Executor executor, T value) {
    return FutureImpl.sync(executor, () -> Try.success(value));
  }

  static <T> Future<T> failure(Throwable error) {
    return failure(DEFAULT_EXECUTOR, error);
  }

  static <T> Future<T> failure(Executor executor, Throwable error) {
    return FutureImpl.sync(executor, () -> Try.failure(error));
  }

  static <T> Future<T> from(Callable<T> callable) {
    return run(callable::call);
  }

  static <T> Future<T> from(java.util.concurrent.Future<T> future) {
    return run(future::get);
  }

  static <T> Future<T> run(CheckedProducer<T> task) {
    return run(DEFAULT_EXECUTOR, task);
  }

  static <T> Future<T> run(Executor executor, CheckedProducer<T> task) {
    return FutureImpl.async(executor, task.liftTry());
  }

  static Future<Unit> exec(CheckedRunnable task) {
    return exec(DEFAULT_EXECUTOR, task);
  }

  static Future<Unit> exec(Executor executor, CheckedRunnable task) {
    return run(executor, () -> { task.run(); return Unit.unit(); });
  }

  static <T> Future<T> delay(Duration timeout, CheckedProducer<T> producer) {
    return delay(DEFAULT_EXECUTOR, timeout, producer);
  }

  static <T> Future<T> delay(Executor executor, Duration timeout, CheckedProducer<T> producer) {
    return run(executor, () -> { MILLISECONDS.sleep(timeout.toMillis()); return producer.get(); });
  }

  static <T> Future<T> narrowK(Higher1<Future.µ, T> hkt) {
    return (Future<T>) hkt;
  }

  static Future<Unit> unit() {
    return FutureModule.UNIT;
  }

  final class FutureImpl<T> implements Future<T> {

    private final Executor executor;
    private final AsyncValue<T> value = new AsyncValue<>();

    private FutureImpl(Executor executor, Consumer1<AsyncValue<T>> task) {
      this.executor = requireNonNull(executor);
      requireNonNull(task).accept(value);
    }

    @Override
    public <R> Future<R> map(Function1<T, R> mapper) {
      return transform(executor, this, value -> value.map(mapper));
    }

    @Override
    public <R> Future<R> flatMap(Function1<T, ? extends Higher1<Future.µ, R>> mapper) {
      return follow(executor, this,
          value -> value.fold(Future::failure, mapper.andThen(Future::narrowK)));
    }

    @Override
    public <R> Future<R> andThen(Future<R> next) {
      return flatMap(ignore -> next);
    }

    @Override
    public Future<T> filter(Matcher1<T> matcher) {
      return transform(executor, this, value -> value.filter(matcher));
    }

    @Override
    public Future<T> orElse(Future<T> other) {
      return follow(executor, this, value -> value.fold(cons(other), Future::success));
    }

    @Override
    public Future<T> recover(Function1<Throwable, T> mapper) {
      return transform(executor, this, value -> value.recover(mapper));
    }

    @Override
    public <X extends Throwable> Future<T> recoverWith(Class<X> type, Function1<X, T> mapper) {
      return transform(executor, this, value -> value.recoverWith(type, mapper));
    }

    @Override
    public <U> Future<U> fold(Function1<Throwable, U> failureMapper, Function1<T, U> successMapper) {
      return transform(executor, this, value -> Try.success(value.fold(failureMapper, successMapper)));
    }

    @Override
    public Try<T> await() {
      return value.get();
    }

    @Override
    public Try<T> await(Duration timeout) {
      return value.get(timeout);
    }

    @Override
    public void cancel(boolean mayInterruptThread) {
      value.cancel(mayInterruptThread);
    }

    @Override
    public boolean isCompleted() {
      return value.isCompleted();
    }

    @Override
    public boolean isCancelled() {
      return value.isCancelled();
    }

    @Override
    public Future<T> onSuccess(Consumer1<T> callback) {
      value.onComplete(value -> value.onSuccess(callback));
      return this;
    }

    @Override
    public Future<T> onFailure(Consumer1<Throwable> callback) {
      value.onComplete(value -> value.onFailure(callback));
      return this;
    }

    @Override
    public Future<T> onComplete(Consumer1<Try<T>> callback) {
      value.onComplete(callback);
      return this;
    }

    @Override
    public FutureModule getModule() {
      throw new UnsupportedOperationException();
    }

    static <T> Future<T> sync(Executor executor, Producer<Try<T>> producer) {
      return new FutureImpl<>(executor, value -> value.set(producer.get()));
    }

    static <T, R> Future<R> transform(Executor executor, Future<T> current, Function1<Try<T>, Try<R>> mapper) {
      return new FutureImpl<>(executor,
          next -> current.onComplete(value -> next.set(mapper.apply(value))));
    }

    static <T, R> Future<R> follow(Executor executor, Future<T> current, Function1<Try<T>, Future<R>> mapper) {
      return new FutureImpl<>(executor,
          next -> current.onComplete(value -> mapper.apply(value).onComplete(next::set)));
    }

    static <T> Future<T> async(Executor executor, Producer<Try<T>> producer) {
      return new FutureImpl<>(executor,
          value -> executor.execute(() -> { value.begin(); value.set(producer.get()); }));
    }
  }
}

interface FutureModule {
  Future<Unit> UNIT = Future.success(Unit.unit());
}

final class AsyncValue<T> {

  private final State state = new State();
  private final Queue<Consumer1<Try<T>>> consumers = new LinkedList<>();
  private final AtomicReference<Try<T>> reference = new AtomicReference<>();

  void begin() {
    synchronized (state) {
      state.thread = Thread.currentThread();
      state.thread.setUncaughtExceptionHandler((t, ex) -> set(Try.failure(ex)));
    }
  }

  void onComplete(Consumer1<Try<T>> consumer) {
    tryOnComplete(consumer).ifPresent(consumer);
  }

  void cancel(boolean mayInterruptThread) {
    if (isEmpty()) {
      synchronized (state) {
        if (isEmpty()) {
          state.cancelled = true;
          if (mayInterruptThread) {
            state.interrupt();
          }
          setValue(Try.failure(new CancellationException()));
          state.notifyAll();
        }
      }
    }
  }

  void set(Try<T> value) {
    if (isEmpty()) {
      synchronized (state) {
        if (isEmpty()) {
          state.completed = true;
          setValue(value);
          state.notifyAll();
        }
      }
    }
  }

  Try<T> get() {
    if (isEmpty()) {
      synchronized (state) {
        if (isEmpty()) {
          try {
            state.wait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Try.failure(e);
          }
        }
      }
    }
    return requireNonNull(reference.get());
  }

  Try<T> get(Duration timeout) {
    if (isEmpty()) {
      synchronized (state) {
        if (isEmpty()) {
          try {
            state.wait(timeout.toMillis());
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Try.failure(e);
          }
        }
      }
    }
    return Option.of(reference::get).getOrElse(Try.failure(new TimeoutException()));
  }

  boolean isCancelled() {
    synchronized (state) {
      return state.cancelled;
    }
  }

  boolean isCompleted() {
    synchronized (state) {
      return state.completed;
    }
  }

  private Option<Try<T>> tryOnComplete(Consumer1<Try<T>> consumer) {
    Try<T> current = reference.get();
    if (isNull(current)) {
      synchronized (state) {
        current = reference.get();
        if (isNull(current)) {
          consumers.add(consumer);
        }
      }
    }
    return Option.of(current);
  }

  private void setValue(Try<T> value) {
    reference.set(value);
    consumers.forEach(consumer -> consumer.accept(value));
  }

  private boolean isEmpty() {
    return isNull(reference.get());
  }

  private static final class State {
    boolean cancelled = false;
    boolean completed = false;
    Thread thread = null;

    void interrupt() {
      if (nonNull(thread)) {
        thread.interrupt();
      }
    }
  }
}