/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import static com.github.tonivade.purefun.core.Function1.cons;
import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;

public final class PPrism<S, T, A, B> {

  private final Function1<S, Either<T, A>> getOrModify;
  private final Function1<B, T> reverseGet;

  PPrism(Function1<S, Either<T, A>> getOrModify, Function1<B, T> reverseGet) {
    this.getOrModify = checkNonNull(getOrModify);
    this.reverseGet = checkNonNull(reverseGet);
  }

  public static <S, T, A, B> PPrism<S, T, A, B> of(Function1<S, Either<T, A>> getOrModify, Function1<B, T> reverseGet) {
    return new PPrism<>(getOrModify, reverseGet);
  }

  public Option<A> getOption(S target) {
    return getOrModify.apply(target).toOption();
  }

  public T reverseGet(B value) {
    return reverseGet.apply(value);
  }

  public Either<T, A> getOrModify(S target) {
    return getOrModify.apply(target);
  }

  public Function1<S, T> modify(Function1<A, B> mapper) {
    return target -> getOrModify(target).fold(identity(), a -> reverseGet(mapper.apply(a)));
  }

  public T modify(S target, Function1<A, B> mapper) {
    return modify(mapper).apply(target);
  }

  public Function1<S, T> set(B value) {
    return modify(cons(value));
  }

  public T set(S target, B value) {
    return set(value).apply(target);
  }

  public Function1<S, Option<T>> modifyOption(Function1<A, B> mapper) {
    return target -> getOption(target).map(mapper).map(reverseGet);
  }

  public Function1<S, Option<T>> setOption(B value) {
    return modifyOption(ignore -> value);
  }

  public POptional<S, T, A, B> asOptional() {
    return POptional.of(this::set, this::getOrModify);
  }

  public <C, D> PPrism<S, T, C, D> compose(PPrism<A, B, C, D> other) {
    return new PPrism<>(
        target -> getOrModify(target).flatMap(a -> other.getOrModify(a).bimap(b -> set(target, b), identity())),
        value -> this.reverseGet(other.reverseGet(value)));
  }

  public <C, D> POptional<S, T, C, D> compose(POptional<A, B, C, D> other) {
    return asOptional().compose(other);
  }

  public <C, D> PPrism<S, T, C, D> compose(PIso<A, B, C, D> other) {
    return compose(other.asPrism());
  }

  public <C, D> POptional<S, T, C, D> compose(PLens<A, B, C, D> other) {
    return asOptional().compose(other);
  }
}
