/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import static com.github.tonivade.purefun.Producer.cons;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;

public final class Prism<T, R> {

  private final Function1<T, Option<R>> getOption;
  private final Function1<R, T> reverseGet;

  private Prism(Function1<T, Option<R>> getOption, Function1<R, T> reverseGet) {
    this.getOption = requireNonNull(getOption);
    this.reverseGet = requireNonNull(reverseGet);
  }

  public static <T, R> Prism<T, R> of(Function1<T, Option<R>> getOption, Function1<R, T> reverseGet) {
    return new Prism<>(getOption, reverseGet);
  }

  public Option<R> getOption(T target) {
    return getOption.apply(target);
  }

  public T reverseGet(R value) {
    return reverseGet.apply(value);
  }

  public Either<T, R> getOrModify(T target) {
    return getOption(target).fold(cons(Either.left(target)), Either::right);
  }

  public Operator1<T> modify(Operator1<R> mapper) {
    return target -> modifyOption(mapper).apply(target).getOrElse(target);
  }

  public Operator1<T> set(R value) {
    return modify(ignore -> value);
  }

  public Function1<T, Option<T>> modifyOption(Operator1<R> mapper) {
    return target -> getOption(target).map(mapper).map(reverseGet);
  }

  public Function1<T, Option<T>> setOption(R value) {
    return modifyOption(ignore -> value);
  }

  public <V> Prism<T, V> compose(Prism<R, V> other) {
    return new Prism<>(
        target -> this.getOption(target).flatMap(other::getOption),
        value -> this.reverseGet(other.reverseGet(value)));
  }
}
