/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface EitherHandler<T, L, R> extends Function1<T, Either<L, R>>{

  @Override
  default <V> EitherHandler<V, L, R> compose(Function1<V, T> before) {
    return value -> apply(before.apply(value));
  }

  default <V> EitherHandler<T, L, V> map(Function1<R, V> mapper) {
    return value -> apply(value).map(mapper::apply);
  }
  
  default <V> EitherHandler<T, V, R> mapLeft(Function1<L, V> mapper) {
    return value -> apply(value).mapLeft(mapper::apply);
  }
  
  default <V> EitherHandler<T, L, V> flatMap(EitherHandler<R, L, V> mapper) {
    return value -> apply(value).flatMap(mapper::apply);
  }
  
  default <V> EitherHandler<T, L, V> flatten() {
    return value -> apply(value).flatten();
  }
  
  default OptionHandler<T, Either<L, R>> filter(Matcher<R> matcher) {
    return value -> apply(value).filter(matcher);
  }
  
  default Function1<T, R> orElse(Producer<R> producer) {
    return value -> apply(value).orElse(producer);
  }

  static <L, R> Function1<Either<L, R>, Either<L, R>> identity() {
    return Function1.<Either<L, R>>identity()::apply;
  }
  
  static <T, L, R> EitherHandler<T, L, R> of(Function1<T, Either<L, R>> reference) {
    return reference::apply;
  }
}
