/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Operator1;

import static java.util.Objects.requireNonNull;

public final class Iso<S, A> {

  private final Function1<S, A> get;
  private final Function1<A, S> reverseGet;

  private Iso(Function1<S, A> get, Function1<A, S> reverseGet) {
    this.get = requireNonNull(get);
    this.reverseGet = requireNonNull(reverseGet);
  }

  public static <S, A> Iso<S, A> of(Function1<S, A> get, Function1<A, S> reverseGet) {
    return new Iso<>(get, reverseGet);
  }

  public static <S> Iso<S, S> identity() {
    return new Iso<>(Function1.identity(), Function1.identity());
  }

  public Iso<A, S> reverse() {
    return new Iso<>(reverseGet, get);
  }

  public A get(S target) {
    return get.apply(target);
  }

  public S set(A value) {
    return reverseGet.apply(value);
  }

  public S modify(S target, Operator1<A> mapper) {
    return lift(mapper).apply(target);
  }

  public Operator1<S> lift(Operator1<A> mapper) {
    return mapper.compose(get).andThen(reverseGet)::apply;
  }

  public <B> Iso<S, B> compose(Iso<A, B> other) {
    return new Iso<>(
        this.get.andThen(other.get),
        this.reverseGet.compose(other.reverseGet));
  }
}
