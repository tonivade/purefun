/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;

public final class Optional<S, A> {

  private final POptional<S, S, A, A> delegate;

  Optional(POptional<S, S, A, A> delegate) {
    this.delegate = checkNonNull(delegate);
  }

  public static <S, A> Optional<S, A> of(Function2<S, A, S> set, Function1<S, Option<A>> getOption) {
    return new Optional<>(POptional.of(set, target -> getOption.apply(target).fold(cons(Either.left(target)), Either::right)));
  }

  public Function1<A, S> set(S target) {
    return delegate.set(target);
  }

  public S set(S target, A value) {
    return delegate.set(target, value);
  }

  public Either<S, A> getOrModify(S target) {
    return delegate.getOrModify(target);
  }

  public Option<A> getOption(S target) {
    return delegate.getOption(target);
  }

  public Operator1<S> lift(Operator1<A> mapper) {
    return delegate.lift(mapper)::apply;
  }

  public S modify(S target, Operator1<A> mapper) {
    return delegate.modify(target, mapper);
  }

  public Option<S> modifyOption(S target, Operator1<A> mapper) {
    return delegate.modifyOption(target, mapper);
  }

  public <B> Optional<S, B> compose(Optional<A, B> other) {
    return new Optional<>(delegate.compose(other.delegate));
  }

  public <B> Optional<S, B> compose(Iso<A, B> other) {
    return compose(other.asOptional());
  }

  public <B> Optional<S, B> compose(Prism<A, B> other) {
    return compose(other.asOptional());
  }

  public <B> Optional<S, B> compose(Lens<A, B> other) {
    return compose(other.asOptional());
  }
}
