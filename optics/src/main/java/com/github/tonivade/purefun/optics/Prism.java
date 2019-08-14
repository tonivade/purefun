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

public final class Prism<S, A> {

  private final Function1<S, Option<A>> getOption;
  private final Function1<A, S> reverseGet;

  private Prism(Function1<S, Option<A>> getOption, Function1<A, S> reverseGet) {
    this.getOption = requireNonNull(getOption);
    this.reverseGet = requireNonNull(reverseGet);
  }

  public static <S, A> Prism<S, A> of(Function1<S, Option<A>> getOption, Function1<A, S> reverseGet) {
    return new Prism<>(getOption, reverseGet);
  }

  public Option<A> getOption(S target) {
    return getOption.apply(target);
  }

  public S reverseGet(A value) {
    return reverseGet.apply(value);
  }

  public Either<S, A> getOrModify(S target) {
    return getOption(target).fold(cons(Either.left(target)), Either::right);
  }

  public Operator1<S> modify(Operator1<A> mapper) {
    return target -> modifyOption(mapper).apply(target).getOrElse(target);
  }

  public S modify(S target, Operator1<A> mapper) {
    return modify(mapper).apply(target);
  }

  public Operator1<S> set(A value) {
    return modify(ignore -> value);
  }

  public S set(S target, A value) {
    return set(value).apply(target);
  }

  public Function1<S, Option<S>> modifyOption(Operator1<A> mapper) {
    return target -> getOption(target).map(mapper).map(reverseGet);
  }

  public Function1<S, Option<S>> setOption(A value) {
    return modifyOption(ignore -> value);
  }

  public Optional<S, A> asOptional() {
    return Optional.of(this::set, this::getOrModify);
  }

  public <B> Prism<S, B> compose(Prism<A, B> other) {
    return new Prism<>(
        target -> this.getOption(target).flatMap(other::getOption),
        value -> this.reverseGet(other.reverseGet(value)));
  }

  public <B> Optional<S, B> compose(Optional<A, B> other) {
    return asOptional().compose(other);
  }

  public <B> Prism<S, B> compose(Iso<A, B> other) {
    return compose(other.asPrism());
  }

  public <B> Optional<S, B> compose(Lens<A, B> other) {
    return asOptional().compose(other);
  }
}
