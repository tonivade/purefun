/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.util.function.Supplier;

public interface EitherHandler<T, L, R> extends Handler1<T, Either<L, R>>{

  default <V> EitherHandler<T, L, V> map(Handler1<R, V> mapper) {
    return value -> handle(value).map(mapper::handle);
  }
  
  default <V> EitherHandler<T, V, R> mapLeft(Handler1<L, V> mapper) {
    return value -> handle(value).mapLeft(mapper::handle);
  }
  
  default <V> EitherHandler<T, L, V> flatMap(Handler1<R, Either<L, V>> mapper) {
    return value -> handle(value).flatMap(mapper::handle);
  }
  
  default OptionHandler<T, Either<L, R>> filter(Matcher<R> predicate) {
    return value -> handle(value).filter(predicate);
  }
  
  default EitherHandler<T, L, R> orElse(Supplier<Either<L, R>> supplier) {
    return value -> handle(value).orElse(supplier);
  }
  
  static <T, L, R> EitherHandler<T, L, R> adapt(Handler1<T, Either<L, R>> handler) {
    return handler::handle;
  }
  
  static <T, L, R> EitherHandler<T, L, R> adapt(Supplier<Either<L, R>> supplier) {
    return value -> supplier.get();
  }
}
