package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Function1.identity;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
    return new FutureImpl<>(executor, requireNonNull(task));
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
    private final BlockingQueue<Try<T>> queue = new BlockingQueue<>();
    
    private FutureImpl(Executor executor, Producer<Try<T>> task) {
      this.executor = requireNonNull(executor);
      executor.execute(() -> queue.offer(task.get()));
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
    @SuppressWarnings("unchecked")
    public <V> Future<V> flatten() {
      try {
        return ((Future<Future<V>>) this).flatMap(identity());
      } catch (ClassCastException e) {
        throw new UnsupportedOperationException("cannot be flattened");
      }
    }
    
    @Override
    public Try<T> await() {
      return CheckedProducer.of(queue::take).unchecked().get();
    }

    @Override
    public boolean isCompleted() {
      return !queue.isEmpty();
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

  Executor DEFAULT_EXECUTOR = ForkJoinPool.commonPool();
} 

final class BlockingQueue<T> {

  private T value;

  private final ReentrantLock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();

  void offer(T value) {
    lock.lock();
    try {
      if (nonNull(this.value)) {
        throw new IllegalStateException("queue full");
      }
      this.value = value;
      condition.signal();
    } finally {
      lock.unlock();
    }
  }

  T take() throws InterruptedException {
    lock.lock();
    try {
      while (isNull(value)) {
        condition.await();
      }
      return value;
    } finally {
      lock.unlock();
    }
  }

  boolean isEmpty() {
    lock.lock();
    try {
      return isNull(value);
    } finally {
      lock.unlock();
    }
  }
}