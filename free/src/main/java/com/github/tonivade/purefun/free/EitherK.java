/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Contravariant;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Functor;

import java.io.Serializable;
import java.util.Objects;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

@HigherKind
public final class EitherK<F extends Kind, G extends Kind, T> implements Higher3<EitherK_, F, G, T>, Serializable {

  private static final long serialVersionUID = -2305737717835278018L;

  private static final Equal<EitherK<?, ?, ?>> EQUAL =
      Equal.<EitherK<?, ?, ?>>of().comparing(eitherK -> eitherK.either);

  private final Either<Higher1<F, T>, Higher1<G, T>> either;

  private EitherK(Either<Higher1<F, T>, Higher1<G, T>> either) {
    this.either = checkNonNull(either);
  }

  public <R> EitherK<F, G, R> map(Functor<F> functorF, Functor<G> functorG, Function1<T, R> mapper) {
    return new EitherK<>(either.bimap(functorF.lift(mapper), functorG.lift(mapper)));
  }

  public <X extends Kind> EitherK<F, X, T> mapK(FunctionK<G, X> mapper) {
    return new EitherK<>(either.map(mapper::apply));
  }

  public <X extends Kind> EitherK<X, G, T> mapLeftK(FunctionK<F, X> mapper) {
    return new EitherK<>(either.mapLeft(mapper::apply));
  }

  public <R extends Kind> Higher1<R, T> foldK(FunctionK<F, R> left, FunctionK<G, R> right) {
    return either.fold(left::apply, right::apply);
  }

  public <R> EitherK<F, G, R> coflatMap(
      Comonad<F> comonadF, Comonad<G> comonadG, Function1<EitherK<F, G, T>, R> mapper) {
    return new EitherK<>(either.bimap(
        a -> comonadF.coflatMap(a, x -> mapper.apply(left(x))),
        a -> comonadG.coflatMap(a, x -> mapper.apply(right(x)))
    ));
  }

  public T extract(Comonad<F> comonadF, Comonad<G> comonadG) {
    return either.fold(comonadF::extract, comonadG::extract);
  }

  public <R> EitherK<F, G, R> contramap(Contravariant<F> contravariantF, Contravariant<G> contravariantG, Function1<R, T> contramap) {
    return new EitherK<>(either.bimap(
        x -> contravariantF.contramap(x, contramap),
        x -> contravariantG.contramap(x, contramap))
    );
  }

  public EitherK<G, F, T> swap() {
    return new EitherK<>(either.swap());
  }

  public boolean isLeft() {
    return either.isLeft();
  }

  public boolean isRight() {
    return either.isRight();
  }

  public Higher1<F, T> getLeft() {
    return either.getLeft();
  }

  public Higher1<G, T> getRight() {
    return either.getRight();
  }

  public static <F extends Kind, G extends Kind, T> EitherK<F, G, T> left(Higher1<F, T> left) {
    return new EitherK<>(Either.left(left));
  }

  public static <F extends Kind, G extends Kind, T> EitherK<F, G, T> right(Higher1<G, T> right) {
    return new EitherK<>(Either.right(right));
  }

  @Override
  public int hashCode() {
    return Objects.hash(either);
  }

  @Override
  public boolean equals(Object obj) {
    return EQUAL.applyTo(this, obj);
  }

  @Override
  public String toString() {
    return "EitherK(" + either + ')';
  }
}
