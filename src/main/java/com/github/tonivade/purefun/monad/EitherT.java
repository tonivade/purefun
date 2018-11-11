/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Producer.unit;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.FlatMap3;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.algebra.Monad;
import com.github.tonivade.purefun.algebra.Transformer;
import com.github.tonivade.purefun.type.Either;

public final class EitherT<W extends Kind, L, R> implements FlatMap3<EitherT.µ, W, L, R> {

  public static final class µ implements Kind {}

  private final Monad<W> monad;
  private final Higher1<W, Either<L, R>> value;

  protected EitherT(Monad<W> monad, Higher1<W, Either<L, R>> value) {
    this.monad = requireNonNull(monad);
    this.value = requireNonNull(value);
  }

  @Override
  public <V> EitherT<W, L, V> map(Function1<R, V> map) {
    return new EitherT<>(monad, monad.map(value, v -> v.map(map)));
  }

  @Override
  public <V> EitherT<W, L, V> flatMap(Function1<R, ? extends Higher3<EitherT.µ, W, L, V>> map) {
    return new EitherT<>(monad, flatMapF(v -> EitherT.narrowK(map.apply(v)).value));
  }

  public <T, V> EitherT<W, T, V> bimap(Function1<L, T> leftMapper, Function1<R, V> rightMapper) {
    return new EitherT<>(monad, monad.map(value, v -> v.bimap(leftMapper, rightMapper)));
  }

  public <T> EitherT<W, T, R> mapLeft(Function1<L, T> leftMapper) {
    return new EitherT<>(monad, monad.map(value, v -> v.mapLeft(leftMapper)));
  }

  public <V> Higher1<W, V> fold(Function1<L, V> leftMapper, Function1<R, V> rightMapper) {
    return monad.map(value, v -> v.fold(leftMapper, rightMapper));
  }

  public <F extends Kind> EitherT<F, L, R> mapK(Monad<F> other, Transformer<W, F> transformer) {
    return new EitherT<>(other, transformer.apply(value));
  }

  public EitherT<W, L, R> filterOrElse(Matcher1<R> filter, Producer<Either<L, R>> orElse) {
    return new EitherT<>(monad, monad.map(value, v -> v.filterOrElse(filter, orElse)));
  }

  public EitherT<W, R, L> swap() {
    return new EitherT<>(monad, monad.map(value, Either::swap));
  }

  public Higher1<W, Boolean> isRight() {
    return monad.map(value, Either::isRight);
  }

  public Higher1<W, Boolean> isLeft() {
    return monad.map(value, Either::isLeft);
  }

  public Higher1<W, L> getLeft() {
    return monad.map(value, Either::getLeft);
  }

  public Higher1<W, R> getRight() {
    return monad.map(value, Either::getRight);
  }

  public Higher1<W, R> get() {
    return getRight();
  }

  public Higher1<W, R> orElse(R orElse) {
    return orElse(unit(orElse));
  }

  public Higher1<W, R> orElse(Producer<R> orElse) {
    return fold(left -> orElse.get(), identity());
  }

  public OptionT<W, R> toOption() {
    return new OptionT<>(monad, monad.map(value, Either::toOption));
  }

  public static <W extends Kind, L, R> EitherT<W, L, R> lift(Monad<W> monad, Either<L, R> either) {
    return new EitherT<>(monad, monad.pure(either));
  }

  public static <W extends Kind, L, R> EitherT<W, L, R> right(Monad<W> monad, R right) {
    return lift(monad, Either.right(right));
  }

  public static <W extends Kind, L, R> EitherT<W, L, R> left(Monad<W> monad, L left) {
    return lift(monad, Either.left(left));
  }

  public static <W extends Kind, L, R> EitherT<W, L, R> narrowK(Higher3<EitherT.µ, W, L, R> hkt) {
    return (EitherT<W, L, R>) hkt;
  }

  private <V> Higher1<W, Either<L, V>> flatMapF(Function1<R, Higher1<W, Either<L, V>>> map) {
   return monad.flatMap(value, v -> v.fold(left -> monad.pure(Either.left(left)), map));
  }
}
