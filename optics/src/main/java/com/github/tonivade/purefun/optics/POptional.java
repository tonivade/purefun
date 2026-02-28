/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;

public final class POptional<S, T, A, B> {

  private final Function1<S, Function1<B, T>> set;
  private final Function1<S, Either<T, A>> getOrModify;

  POptional(Function1<S, Function1<B, T>> set, Function1<S, Either<T, A>> getOrModify) {
    this.set = checkNonNull(set);
    this.getOrModify = checkNonNull(getOrModify);
  }

  public static <S, T, A, B> POptional<S, T, A, B> of(Function2<S, B, T> set, Function1<S, Either<T, A>> getOrModify) {
    return new POptional<>(set.curried(), getOrModify);
  }

  public Function1<B, T> set(S target) {
    return set.apply(target);
  }

  public T set(S target, B value) {
    return set(target).apply(value);
  }

  public Either<T, A> getOrModify(S target) {
    return getOrModify.apply(target);
  }

  public Option<A> getOption(S target) {
    return getOrModify(target).toOption();
  }

  public Function1<S, T> lift(Function1<A, B> mapper) {
    return target -> modify(target, mapper);
  }

  public T modify(S target, Function1<A, B> mapper) {
    return getOrModify(target).fold(identity(), value -> set(target, mapper.apply(value)));
  }

  public Option<T> modifyOption(S target, Function1<A, B> mapper) {
    return getOption(target).map(value -> set(target, mapper.apply(value)));
  }

  public <C, D> POptional<S, T, C, D> compose(POptional<A, B, C, D> other) {
    return new POptional<>(
        target -> value -> this.modify(target, a -> other.set(a, value)),
        target -> this.getOrModify(target).flatMap(a -> other.getOrModify(a).bimap(b -> this.set(target, b), identity())) );
  }

  public <C, D> POptional<S, T, C, D> compose(PIso<A, B, C, D> other) {
    return compose(other.asOptional());
  }

  public <C, D> POptional<S, T, C, D> compose(PPrism<A, B, C, D> other) {
    return compose(other.asOptional());
  }

  public <C, D> POptional<S, T, C, D> compose(PLens<A, B, C, D> other) {
    return compose(other.asOptional());
  }
}
