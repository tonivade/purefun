/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.github.tonivade.purefun.handler.EitherHandler;
import com.github.tonivade.purefun.handler.OptionHandler;
import com.github.tonivade.purefun.handler.OptionalHandler;
import com.github.tonivade.purefun.handler.StreamHandler;
import com.github.tonivade.purefun.handler.TryHandler;
import com.github.tonivade.purefun.type.Try;

@FunctionalInterface
public interface CheckedFunction1<T, R> {

  R apply(T value) throws Exception;

  default <V> CheckedFunction1<T, V> andThen(CheckedFunction1<R, V> after) {
    return (T value) -> after.apply(apply(value));
  }
  
  default <V> CheckedFunction1<V, R> compose(CheckedFunction1<V, T> before) {
    return (V value) -> apply(before.apply(value));
  }
  
  default OptionalHandler<T, R> liftOptional() {
    return liftTry().toOption().toOptional();
  }
  
  default OptionHandler<T, R> liftOption() {
    return liftTry().toOption();
  }
  
  default TryHandler<T, R> liftTry() {
    return value -> Try.of(() -> apply(value));
  }
  
  default EitherHandler<T, Throwable, R> liftEither() {
    return liftTry().toEither();
  }
  
  default StreamHandler<T, R> stream() {
    return liftTry().andThen(Try::stream)::apply;
  }
  
  static <T> CheckedFunction1<T, T> identity() {
    return value -> value;
  }
  
  static <T> CheckedFunction1<T, T> failure(String message) {
    return value -> { throw new Exception(message); };
  }
  
  static <T, R> CheckedFunction1<T, R> of(CheckedFunction1<T, R> reference) {
    return reference;
  }
}
