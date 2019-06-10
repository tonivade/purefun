/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import static com.github.tonivade.purefun.Function1.cons;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
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

  ExecutorService DEFAULT_EXECUTOR = Executors.newCachedThreadPool();

  final class µ implements Kind {}

  Try<T> await();
  Try<T> await(Duration timeout);

  void cancel();

  boolean isCompleted();
  boolean isCanceled();

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

  <U> Future<U> fold(Function1<Throwable, U> failureMapper, Function1<T, U> successMapper);

  FutureModule getModule();

  static <T> Future<T> success(T value) {
    return success(DEFAULT_EXECUTOR, value);
  }

  static <T> Future<T> success(ExecutorService executor, T value) {
    return FutureImpl.sync(executor, () -> Try.success(value));
  }

  static <T> Future<T> failure(Throwable error) {
    return failure(DEFAULT_EXECUTOR, error);
  }

  static <T> Future<T> failure(ExecutorService executor, Throwable error) {
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

  static <T> Future<T> run(ExecutorService executor, CheckedProducer<T> task) {
    return FutureImpl.async(executor, task.liftTry());
  }

  static Future<Unit> exec(CheckedRunnable task) {
    return exec(DEFAULT_EXECUTOR, task);
  }

  static Future<Unit> exec(ExecutorService executor, CheckedRunnable task) {
    return run(executor, () -> { task.run(); return Unit.unit(); });
  }

  static <T> Future<T> delay(Duration timeout, CheckedProducer<T> producer) {
    return delay(DEFAULT_EXECUTOR, timeout, producer);
  }

  static <T> Future<T> delay(ExecutorService executor, Duration timeout, CheckedProducer<T> producer) {
    return run(executor, () -> { MILLISECONDS.sleep(timeout.toMillis()); return producer.get(); });
  }

  static <T> Future<T> narrowK(Higher1<Future.µ, T> hkt) {
    return (Future<T>) hkt;
  }

  static Future<Unit> unit() {
    return FutureModule.UNIT;
  }

  final class FutureImpl<T> implements Future<T> {

    private final ExecutorService executor;
    private final AsyncValue<Try<T>> value = new AsyncValue<>();

    private FutureImpl(ExecutorService executor, Consumer1<AsyncValue<Try<T>>> task) {
      this.executor = requireNonNull(executor);
      requireNonNull(task).accept(value);
    }

    @Override
    public <R> Future<R> map(Function1<T, R> mapper) {
      return transform(executor, this, value -> value.map(mapper));
    }

    @Override
    public <R> Future<R> flatMap(Function1<T, ? extends Higher1<Future.µ, R>> mapper) {
      return run(executor, this, 
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
      return run(executor, this, value -> value.fold(cons(other), Future::success));
    }

    @Override
    public Future<T> recover(Function1<Throwable, T> mapper) {
      return transform(executor, this, value -> value.recover(mapper));
    }

    @Override
    public <U> Future<U> fold(Function1<Throwable, U> failureMapper, Function1<T, U> successMapper) {
      return transform(executor, this, value -> Try.success(value.fold(failureMapper, successMapper)));
    }

    @Override
    public Try<T> await() {
      return result().recover(Try::failure).get();
    }

    @Override
    public Try<T> await(Duration timeout) {
      return result(timeout).recover(Try::failure).get();
    }

    @Override
    public void cancel() {
      // TODO:
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCompleted() {
      return !value.isEmpty();
    }

    @Override
    public boolean isCanceled() {
      // TODO:
      return false;
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
    
    static <T> Future<T> sync(ExecutorService executor, Producer<Try<T>> producer) {
      return new FutureImpl<T>(executor, value -> value.set(producer.get()));
    }
    
    static <T> Future<T> async(ExecutorService executor, Producer<Try<T>> producer) {
      return new FutureImpl<T>(executor, value -> executor.submit(() -> value.set(producer.get())));
    }
    
    static <T, R> Future<R> transform(ExecutorService executor, Future<T> current, Function1<Try<T>, Try<R>> mapper) {
      return new FutureImpl<>(executor, 
          next -> current.onComplete(value -> next.set(mapper.apply(value))));
    }
    
    static <T, R> Future<R> run(ExecutorService executor, Future<T> current, Function1<Try<T>, Future<R>> mapper) {
      return new FutureImpl<>(executor, 
          next -> executor.submit(() -> current.onComplete(
              value -> mapper.apply(value).onComplete(next::set))));
    }

    private CheckedProducer<Try<T>> result() {
      return () -> value.get().getOrElse(Try.failure(new NoSuchElementException()));
    }

    private CheckedProducer<Try<T>> result(Duration timeout) {
      return () -> value.get(timeout).getOrElse(Try.failure(new NoSuchElementException()));
    }
  }
}

interface FutureModule {
  Future<Unit> UNIT = Future.success(Unit.unit());
}

final class AsyncValue<T> {

  private final Queue<Consumer1<T>> consumers = new LinkedBlockingQueue<>();
  private final AtomicReference<Option<T>> reference = new AtomicReference<>(Option.none());
  private final CountDownLatch latch = new CountDownLatch(1);
  
  void onComplete(Consumer1<T> consumer) {
    if (isEmpty()) {
      consumers.add(consumer);
    } else reference.get().ifPresent(consumer);
  }

  void set(T value) {
    if (reference.compareAndSet(Option.none(), Option.some(value))) {
      latch.countDown();
      consumers.forEach(consumer -> consumer.accept(value));
    }
    else throw new IllegalStateException("already setted: " + reference.get());
  }

  Option<T> get() throws InterruptedException {
    latch.await();
    return reference.get();
  }

  Option<T> get(Duration timeout) throws InterruptedException, TimeoutException {
    await(timeout);
    return reference.get();
  }

  boolean isEmpty() {
    return reference.get().equals(Option.none());
  }

  private void await(Duration timeout) throws InterruptedException, TimeoutException {
    if (!latch.await(timeout.toMillis(), MILLISECONDS)) {
      throw new TimeoutException();
    }
  }
}