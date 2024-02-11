/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Producer.cons;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;

public final class Prism<S, A> {

  private final PPrism<S, S, A, A> delegate;

  Prism(PPrism<S, S, A, A> delegate) {
    this.delegate = checkNonNull(delegate);
  }

  public static <S, A> Prism<S, A> of(Function1<S, Option<A>> getOption, Function1<A, S> reverseGet) {
    return new Prism<>(PPrism.of(
        target -> getOption.apply(target).fold(cons(Either.left(target)), Either::right), reverseGet));
  }

  public Option<A> getOption(S target) {
    return delegate.getOption(target);
  }

  public S reverseGet(A value) {
    return delegate.reverseGet(value);
  }

  public Either<S, A> getOrModify(S target) {
    return delegate.getOrModify(target);
  }

  public Operator1<S> modify(Operator1<A> mapper) {
    return delegate.modify(mapper)::apply;
  }

  public S modify(S target, Operator1<A> mapper) {
    return delegate.modify(target, mapper);
  }

  public Operator1<S> set(A value) {
    return delegate.set(value)::apply;
  }

  public S set(S target, A value) {
    return delegate.set(target, value);
  }

  public Function1<S, Option<S>> modifyOption(Operator1<A> mapper) {
    return delegate.modifyOption(mapper);
  }

  public Function1<S, Option<S>> setOption(A value) {
    return delegate.setOption(value);
  }

  public Optional<S, A> asOptional() {
    return new Optional<>(delegate.asOptional());
  }

  public <B> Prism<S, B> compose(Prism<A, B> other) {
    return new Prism<>(delegate.compose(other.delegate));
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
