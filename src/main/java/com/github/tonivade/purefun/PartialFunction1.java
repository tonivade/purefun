package com.github.tonivade.purefun;

import java.util.Objects;

import com.github.tonivade.purefun.data.ImmutableArray;

public interface PartialFunction1<T, R> extends Function1<T, R> {

  boolean isDefinedAt(T value);

  default <V> PartialFunction1<T, R> orElse(PartialFunction1<T, R> orElse) {
    final PartialFunction1<T, R> self = PartialFunction1.this;
    return new PartialFunction1<T, R>() {
      @Override
      public R apply(T value) {
        if (self.isDefinedAt(value)) {
          return self.apply(value);
        }
        return orElse.apply(value);
      }

      @Override
      public boolean isDefinedAt(T value) {
        return self.isDefinedAt(value)
          || orElse.isDefinedAt(value);
      }
    };
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
    this.apply = Objects.requireNonNull(apply);
    this.isDefined = Objects.requireNonNull(isDefined);
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