package com.github.tonivade.purefun;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.data.ImmutableArray;
import com.github.tonivade.purefun.type.Option;

public interface PartialFunction1<T, R> {
  
  R apply(T value);

  boolean isDefinedAt(T value);
  
  default Function1<T, Option<R>> lift() {
    return value -> isDefinedAt(value) ? Option.some(apply(value)) : Option.none();
  }

  default <V> PartialFunction1<T, V> andThen(Function1<R, V> after) {
    return of(value -> after.apply(apply(value)), this::isDefinedAt);
  }

  default <V> Function1<V, R> compose(Function1<V, T> before) {
    return value -> apply(before.apply(value));
  }

  default <V> PartialFunction1<T, R> orElse(PartialFunction1<T, R> other) {
    final PartialFunction1<T, R> self = PartialFunction1.this;
    return of(value -> self.isDefinedAt(value) ? self.apply(value) : other.apply(value),
              value -> self.isDefinedAt(value) || other.isDefinedAt(value));
  }

  static <T, R> PartialFunction1<T, R> of(Function1<T, R> apply, Matcher1<T> isDefined) {
    return new DefaultPartialFunction1<>(apply, isDefined);
  }

  static <R> PartialFunction1<Integer, R> from(ImmutableArray<R> array) {
    return of(position -> array.get(position), 
              position -> position > 0 && position < array.size());
  }
}

class DefaultPartialFunction1<T, R> implements PartialFunction1<T, R> {
  
  private final Function1<T, R> apply;
  private final Matcher1<T> isDefined;

  DefaultPartialFunction1(Function1<T, R> apply, Matcher1<T> isDefined) {
    this.apply = requireNonNull(apply);
    this.isDefined = requireNonNull(isDefined);
  }

  @Override
  public R apply(T value) {
    return apply.apply(value);
  }

  @Override
  public boolean isDefinedAt(T value) {
    return isDefined.match(value);
  }
}