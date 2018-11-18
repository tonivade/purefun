package com.github.tonivade.purefun.type;

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
  
  Executor DEFAULT_EXECUTOR = ForkJoinPool.commonPool();
  
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
    return new FutureImpl<>(DEFAULT_EXECUTOR, task);
  }
  
  final class FutureImpl<T> implements Future<T> {
    
    private volatile Try<T> result;
    private Consumer1<T> onSuccess = Consumer1.noop();
    private Consumer1<Throwable> onFailure = Consumer1.noop();
    
    private final Object mutex = new Object();

    private FutureImpl(Executor executor, CheckedProducer<T> task) {
      executor.execute(() -> {
        synchronized (mutex) {
          result = task.liftTry().get().onSuccess(onSuccess).onFailure(onFailure);
        }
      });
    }
    
    @Override
    public T get() {
      return getValue().orElseThrow(IllegalStateException::new);
    }
    
    @Override
    public Try<T> getValue() {
      synchronized (mutex) {
        return result;
      }
    }

    @Override
    public boolean isCompleted() {
      return result != null;
    }

    @Override
    public Future<T> onSuccess(Consumer1<T> callback) {
      synchronized (mutex) {
        if (result != null) {
          result.onSuccess(callback);
        } else {
          onSuccess = callback;
        }
      }
      return this;
    }

    @Override
    public Future<T> onFailure(Consumer1<Throwable> callback) {
      synchronized (mutex) {
        if (result != null) {
          result.onFailure(callback);
        } else {
          onFailure = callback;
        }
      }
      return this;
    }
  }
}
