package com.github.tonivade.purefun.type;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

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
    
    private final BlockingQueue<Try<T>> queue = new ArrayBlockingQueue<>(1);
    
    private FutureImpl(Executor executor, CheckedProducer<T> task) {
      executor.execute(() -> queue.offer(task.liftTry().get()));
    }
    
    @Override
    public T get() {
      return getValue().orElseThrow(IllegalStateException::new);
    }
    
    @Override
    public Try<T> getValue() {
      return CheckedProducer.of(queue::peek).unchecked().get();
    }

    @Override
    public boolean isCompleted() {
      return !queue.isEmpty();
    }

    @Override
    public Future<T> onSuccess(Consumer1<T> callback) {
      getValue().onSuccess(callback);
      return this;
    }

    @Override
    public Future<T> onFailure(Consumer1<Throwable> callback) {
      getValue().onFailure(callback);
      return this;
    }
  }
}

interface FutureModule {

  Executor DEFAULT_EXECUTOR = ForkJoinPool.commonPool();
}