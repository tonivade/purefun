package com.github.tonivade.zeromock.core;

public interface Applicative<T> extends Functor<T>, Holder<T> {
  
  <V> Applicative<V> unit(V value);
  
  default <V> Handler1<Applicative<T>, Applicative<V>> apply(Applicative<Handler1<T, V>> applicativeH) {
    return aplicativeT -> unit(applicativeH.get().handle(aplicativeT.get()));
  }
  
  @Override
  default <R> Applicative<R> map(Handler1<T, R> map) {
    return apply(unit(map)).handle(this);
  }

}

class OptionApplicative<T> implements Applicative<T> {
  
  private Option<T> value;
  
  public OptionApplicative(T value) {
    this.value = Option.some(value);
  }

  @Override
  public T get() {
    return value.get();
  }

  @Override
  public <V> OptionApplicative<V> unit(V value) {
    return new OptionApplicative<>(value);
  }
  
}