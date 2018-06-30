package com.github.tonivade.zeromock.core;

import static java.util.Objects.requireNonNull;

public final class Lens<T, R> {

  private final Function1<T, R> getter;
  private final Function2<T, R, T> setter;

  private Lens(Function1<T, R> getter, Function2<T, R, T> setter) {
    this.getter = requireNonNull(getter);
    this.setter = requireNonNull(setter);
  }

  public static <T, R> Lens<T, R> of(Function1<T, R> getter, Function2<T, R, T> setter) {
    return new Lens<>(getter, setter);
  }

  public R get(T target) {
    return getter.apply(target);
  }

  public T set(T target, R value) {
    return set(target).apply(value);
  }

  public Function1<R, T> set(T target) {
    return setter.curried().apply(target);
  }

  public Operator1<T> modify(Operator1<R> mapper) {
    return target -> set(target).apply(mapper.apply(getter.apply(target)));
  }

  public Operator1<T> modify(R newValue) {
    return modify(ignore -> newValue);
  }

  public <V> Lens<T, V> compose(Lens<R, V> other) {
    return new Lens<>(
        target -> other.get(this.get(target)),
        (target, value) -> this.set(target).apply(other.modify(value).apply(this.get(target))));
  }
}
