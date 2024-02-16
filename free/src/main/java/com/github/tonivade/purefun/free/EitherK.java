/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Contravariant;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Functor;

@HigherKind
public final class EitherK<F extends Witness, G extends Witness, T> implements EitherKOf<F, G, T>, Serializable {

  @Serial
  private static final long serialVersionUID = -2305737717835278018L;

  private static final Equal<EitherK<?, ?, ?>> EQUAL =
      Equal.<EitherK<?, ?, ?>>of().comparing(eitherK -> eitherK.either);

  private final Either<Kind<F, T>, Kind<G, T>> either;

  private EitherK(Either<Kind<F, T>, Kind<G, T>> either) {
    this.either = checkNonNull(either);
  }

  public <R> EitherK<F, G, R> map(Functor<F> functorF, Functor<G> functorG, Function1<? super T, ? extends R> mapper) {
    return new EitherK<>(either.bimap(functorF.lift(mapper), functorG.lift(mapper)));
  }

  public <X extends Witness> EitherK<F, X, T> mapK(FunctionK<G, X> mapper) {
    return new EitherK<>(either.map(mapper::apply));
  }

  public <X extends Witness> EitherK<X, G, T> mapLeftK(FunctionK<F, X> mapper) {
    return new EitherK<>(either.mapLeft(mapper::apply));
  }

  public <R extends Witness> Kind<R, T> foldK(FunctionK<F, R> left, FunctionK<G, R> right) {
    return either.fold(left::apply, right::apply);
  }

  public <R> EitherK<F, G, R> coflatMap(
      Comonad<F> comonadF, Comonad<G> comonadG, 
      Function1<? super EitherK<F, G, ? extends T>, ? extends R> mapper) {
    return new EitherK<>(either.bimap(
        a -> comonadF.coflatMap(a, x -> mapper.apply(left(x))),
        a -> comonadG.coflatMap(a, x -> mapper.apply(right(x)))
    ));
  }

  public T extract(Comonad<F> comonadF, Comonad<G> comonadG) {
    return either.fold(comonadF::extract, comonadG::extract);
  }

  public <R> EitherK<F, G, R> contramap(Contravariant<F> contravariantF, 
      Contravariant<G> contravariantG, Function1<? super R, ? extends T> contramap) {
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

  public Kind<F, T> getLeft() {
    return either.getLeft();
  }

  public Kind<G, T> getRight() {
    return either.getRight();
  }

  public static <F extends Witness, G extends Witness, T> EitherK<F, G, T> left(Kind<F, T> left) {
    return new EitherK<>(Either.left(left));
  }

  public static <F extends Witness, G extends Witness, T> EitherK<F, G, T> right(Kind<G, T> right) {
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
