package com.github.tonivade.zeromock.core;

import static tonivade.equalizer.Equalizer.equalizer;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class Try<T> {
  
  private Try() { }
  
  public static <T> Try<T> success(T value) {
    return new Success<>(value);
  }
  
  public static <T> Try<T> failure(Throwable cause) {
    return new Failure<>(cause);
  }
  
  public static <T> Try<T> of(Supplier<T> supplier) {
    try {
      return new Success<T>(supplier.get());
    } catch(Throwable error) {
      return new Failure<T>(error);
    }
  }
  
  public abstract <R> Try<R> map(Handler1<T, R> map);
  public abstract <R> Try<R> flatMap(Handler1<T, Try<R>> map);
  public abstract Try<T> onFailure(Consumer<Throwable> consumer);
  public abstract Try<T> onSuccess(Consumer<T> consumer);
  public abstract Try<T> filter(Predicate<T> predicate);
  public abstract T get();
  public abstract Throwable getCause();
  public abstract boolean isSuccess();
  public abstract boolean isFailure();
  public abstract T orElse(Supplier<T> supplier);
  
  public static class Success<T> extends Try<T> {
    private final T value;
    
    public Success(T value) {
      this.value = value;
    }
    
    @Override
    public <R> Try<R> map(Handler1<T, R> map) {
      return new Success<>(map.handle(value));
    }
    
    @Override
    public <R> Try<R> flatMap(Handler1<T, Try<R>> map) {
      return map.handle(value);
    }
    
    @Override
    public Try<T> onFailure(Consumer<Throwable> consumer) {
      return this;
    }
    
    @Override
    public Try<T> onSuccess(Consumer<T> consumer) {
      consumer.accept(get());
      return this;
    }
    
    @Override
    public Try<T> filter(Predicate<T> predicate) {
      if (predicate.test(value)) {
        return this;
      }
      return new Failure<>(new Exception());
    }
    
    @Override
    public T orElse(Supplier<T> supplier) {
      return value;
    }
    
    @Override
    public boolean isFailure() {
      return false;
    }
    
    @Override
    public boolean isSuccess() {
      return true;
    }
    
    @Override
    public T get() {
      return value;
    }
    
    @Override
    public Throwable getCause() {
      throw new IllegalStateException();
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
      return equalizer(this).append((a, b) -> Objects.equals(a.value, b.value)).applyTo(obj);
    }
  }
  
  public static class Failure<T> extends Try<T> {
    private final Throwable cause;
    
    public Failure(Throwable cause) {
      this.cause = cause;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <R> Try<R> map(Handler1<T, R> map) {
      return (Try<R>) this;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <R> Try<R> flatMap(Handler1<T, Try<R>> map) {
      return (Try<R>) this;
    }
    
    @Override
    public Try<T> onFailure(Consumer<Throwable> consumer) {
      consumer.accept(getCause());
      return this;
    }
    
    @Override
    public Try<T> onSuccess(Consumer<T> consumer) {
      return this;
    }
    
    @Override
    public Try<T> filter(Predicate<T> predicate) {
      return this;
    }
    
    @Override
    public T orElse(Supplier<T> supplier) {
      return supplier.get();
    }
    
    @Override
    public boolean isFailure() {
      return true;
    }
    
    @Override
    public boolean isSuccess() {
      return false;
    }
    
    @Override
    public T get() {
      throw new IllegalStateException();
    }
    
    @Override
    public Throwable getCause() {
      return cause;
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(cause);
    }

    @Override
    public boolean equals(Object obj) {
      return equalizer(this).append((a, b) -> Objects.equals(a.cause, b.cause)).applyTo(obj);
    }
  }
}
