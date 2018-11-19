package com.github.tonivade.purefun.type;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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

public interface Future<T> extends FlatMap1<Future.µ, T>, Holder<T>, Filterable<T> {
  
  final class µ implements Kind {}
  
  Try<T> getValue();
  
  boolean isCompleted();
  
  Future<T> onSuccess(Consumer1<T> callback);
  Future<T> onFailure(Consumer1<Throwable> callback);

  @Override
  default <R> Future<R> map(Function1<T, R> map) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  default <R> Future<R> flatMap(Function1<T, ? extends Higher1<µ, R>> map) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  default Filterable<T> filter(Matcher1<T> matcher) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  default <V> Future<V> flatten() {
    // TODO Auto-generated method stub
    return null;
  }
  
  static <T> Future<T> of(CheckedProducer<T> task) {
    return new FutureImpl<>(FutureModule.DEFAULT_EXECUTOR, task);
  }
  
  final class FutureImpl<T> implements Future<T> {
    
    private final Executor executor;
    private final FutureModule.BlockingQueue<Try<T>> queue = new FutureModule.BlockingQueue<>();
    
    private FutureImpl(Executor executor, CheckedProducer<T> task) {
      this.executor = executor;
      executor.execute(() -> queue.offer(task.liftTry().get()));
    }
    
    @Override
    public T get() {
      return getValue().orElseThrow(IllegalStateException::new);
    }
    
    @Override
    public Try<T> getValue() {
      return CheckedProducer.of(queue::take).unchecked().get();
    }

    @Override
    public boolean isCompleted() {
      return !queue.isEmpty();
    }

    @Override
    public Future<T> onSuccess(Consumer1<T> callback) {
      executor.execute(() -> getValue().onSuccess(callback));
      return this;
    }

    @Override
    public Future<T> onFailure(Consumer1<Throwable> callback) {
      executor.execute(() -> getValue().onFailure(callback));
      return this;
    }
  }
}

interface FutureModule {

  Executor DEFAULT_EXECUTOR = ForkJoinPool.commonPool();
  
  final class BlockingQueue<T> {
    private T value;
    
    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    public void offer(T value) {
      lock.lock();
      try {
        if (nonNull(this.value)) {
          throw new IllegalStateException("queue full");
        }
        this.value = value;
        condition.signal();
      }
      finally {
        lock.unlock();
      }
    }
    
    public T take() throws InterruptedException {
      lock.lock();
      try {
        while (isNull(value)) {
          condition.await();
        }
        return value;
      }
      finally {
        lock.unlock();
      }
    }
    
    public boolean isEmpty() {
      lock.lock();
      try {
        return isNull(value);
      }
      finally {
        lock.unlock();
      }
    }
  }
}