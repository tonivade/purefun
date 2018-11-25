/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.github.tonivade.purefun.CheckedProducer;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Filterable;
import com.github.tonivade.purefun.FlatMap1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Holder;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.algebra.Monad;

public interface Future<T> extends FlatMap1<Future.µ, T>, Holder<T>, Filterable<T> {

  final class µ implements Kind {}

  Try<T> await();
  Try<T> await(Duration timeout);

  boolean isCompleted();

  Future<T> onSuccess(Consumer1<T> callback);
  Future<T> onFailure(Consumer1<Throwable> callback);

  @Override
  <R> Future<R> map(Function1<T, R> mapper);
  @Override
  <R> Future<R> flatMap(Function1<T, ? extends Higher1<Future.µ, R>> mapper);
  @Override
  Future<T> filter(Matcher1<T> matcher);
  @Override
  <V> Future<V> flatten();

  static <T> Future<T> success(T value) {
    return success(FutureModule.DEFAULT_EXECUTOR, value);
  }

  static <T> Future<T> success(Executor executor, T value) {
    return runTry(executor, () -> Try.success(value));
  }

  static <T> Future<T> failure(Throwable error) {
    return failure(FutureModule.DEFAULT_EXECUTOR, error);
  }

  static <T> Future<T> failure(Executor executor, Throwable error) {
    return runTry(executor, () -> Try.failure(error));
  }

  static <T> Future<T> from(Callable<T> callable) {
    return run(callable::call);
  }

  static <T> Future<T> from(java.util.concurrent.Future<T> future) {
    return run(future::get);
  }

  static <T> Future<T> run(CheckedProducer<T> task) {
    return run(FutureModule.DEFAULT_EXECUTOR, task);
  }

  static <T> Future<T> run(Executor executor, CheckedProducer<T> task) {
    return runTry(executor, task.liftTry());
  }

  static <T> Future<T> runTry(Producer<Try<T>> task) {
    return runTry(FutureModule.DEFAULT_EXECUTOR, task);
  }

  static <T> Future<T> runTry(Executor executor, Producer<Try<T>> task) {
    return new FutureImpl<>(requireNonNull(executor), requireNonNull(task));
  }

  static <T> Future<T> narrowK(Higher1<Future.µ, T> hkt) {
    return (Future<T>) hkt;
  }

  static Monad<Future.µ> monad() {
    return new Monad<Future.µ>() {

      @Override
      public <T> Future<T> pure(T value) {
        return Future.success(value);
      }

      @Override
      public <T, R> Future<R> flatMap(Higher1<Future.µ, T> value,
                                      Function1<T, ? extends Higher1<Future.µ, R>> mapper) {
        return Future.narrowK(value).flatMap(mapper);
      }
    };
  }

  final class FutureImpl<T> implements Future<T> {

    private final Executor executor;
    private final AsyncValue<Try<T>> value = new AsyncValue<>();

    private FutureImpl(Executor executor, Producer<Try<T>> task) {
      this.executor = executor;
      executor.execute(() -> value.set(task.get()));
    }

    @Override
    public T get() {
      return await().orElseThrow(IllegalStateException::new);
    }

    @Override
    public <R> Future<R> map(Function1<T, R> mapper) {
      return runTry(executor, () -> await().map(mapper));
    }

    @Override
    public <R> Future<R> flatMap(Function1<T, ? extends Higher1<Future.µ, R>> mapper) {
      return runTry(executor,
          () -> await().flatMap(value -> mapper.andThen(Future::narrowK).apply(value).await()));
    }

    @Override
    public Future<T> filter(Matcher1<T> matcher) {
      return runTry(executor, () -> await().filter(matcher));
    }

    @Override
    public <V> Future<V> flatten() {
      throw new UnsupportedOperationException("cannot be flattened");
    }

    @Override
    public Try<T> await() {
      return CheckedProducer.of(value::get).recover(Try::failure).get();
    }

    @Override
    public Try<T> await(Duration timeout) {
      return CheckedProducer.of(() -> value.get(timeout)).recover(Try::failure).get();
    }

    @Override
    public boolean isCompleted() {
      return !value.isEmpty();
    }

    @Override
    public Future<T> onSuccess(Consumer1<T> callback) {
      executor.execute(() -> await().onSuccess(callback));
      return this;
    }

    @Override
    public Future<T> onFailure(Consumer1<Throwable> callback) {
      executor.execute(() -> await().onFailure(callback));
      return this;
    }
  }
}

interface FutureModule {

  Executor DEFAULT_EXECUTOR = Executors.newCachedThreadPool();
}

final class AsyncValue<T> {

  private final AtomicReference<T> reference = new AtomicReference<>();
  private final CountDownLatch latch = new CountDownLatch(1);

  void set(T value) {
    if (reference.compareAndSet(null, requireNonNull(value))) {
      latch.countDown();
    }
    else throw new IllegalStateException("already setted");
  }

  T get() throws InterruptedException {
    latch.await();
    return reference.get();
  }

  T get(Duration timeout) throws InterruptedException {
    latch.await(requireNonNull(timeout).toMillis(), MILLISECONDS);
    return reference.get();
  }

  boolean isEmpty() {
    return isNull(reference.get());
  }
}