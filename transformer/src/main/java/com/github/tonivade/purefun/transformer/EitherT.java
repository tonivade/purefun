/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Producer.cons;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Transformer;

@HigherKind
public interface EitherT<F extends Kind, L, R> {

  Monad<F> monad();
  Higher1<F, Either<L, R>> value();

  default <V> EitherT<F, L, V> map(Function1<R, V> map) {
    return EitherT.of(monad(), monad().map(value(), v -> v.map(map)));
  }

  default <V> EitherT<F, L, V> flatMap(Function1<R, EitherT<F, L, V>> map) {
    return EitherT.of(monad(), flatMapF(v -> map.apply(v).value()));
  }

  default <T, V> EitherT<F, T, V> bimap(Function1<L, T> leftMapper, Function1<R, V> rightMapper) {
    return EitherT.of(monad(), monad().map(value(), v -> v.bimap(leftMapper, rightMapper)));
  }

  default <T> EitherT<F, T, R> mapLeft(Function1<L, T> leftMapper) {
    return EitherT.of(monad(), monad().map(value(), v -> v.mapLeft(leftMapper)));
  }

  default <V> Higher1<F, V> fold(Function1<L, V> leftMapper, Function1<R, V> rightMapper) {
    return monad().map(value(), v -> v.fold(leftMapper, rightMapper));
  }

  default <G extends Kind> EitherT<G, L, R> mapK(Monad<G> other, Transformer<F, G> transformer) {
    return EitherT.of(other, transformer.apply(value()));
  }

  default EitherT<F, L, R> filterOrElse(Matcher1<R> filter, Producer<Either<L, R>> orElse) {
    return EitherT.of(monad(), monad().map(value(), v -> v.filterOrElse(filter, orElse)));
  }

  default EitherT<F, R, L> swap() {
    return EitherT.of(monad(), monad().map(value(), Either::swap));
  }

  default Higher1<F, Boolean> isRight() {
    return monad().map(value(), Either::isRight);
  }

  default Higher1<F, Boolean> isLeft() {
    return monad().map(value(), Either::isLeft);
  }

  default Higher1<F, L> getLeft() {
    return monad().map(value(), Either::getLeft);
  }

  default Higher1<F, R> getRight() {
    return monad().map(value(), Either::getRight);
  }

  default Higher1<F, R> get() {
    return getRight();
  }

  default Higher1<F, R> getOrElse(R orElse) {
    return getOrElse(cons(orElse));
  }

  default Higher1<F, R> getOrElse(Producer<R> orElse) {
    return fold(left -> orElse.get(), identity());
  }

  default OptionT<F, R> toOption() {
    return OptionT.of(monad(), monad().map(value(), Either::toOption));
  }

  static <F extends Kind, L, R> EitherT<F, L, R> lift(Monad<F> monad, Either<L, R> either) {
    return of(monad, monad.pure(either));
  }

  static <F extends Kind, L, R> EitherT<F, L, R> of(Monad<F> monad, Higher1<F, Either<L, R>> value) {
    requireNonNull(monad);
    requireNonNull(value);
    return new EitherT<F, L, R>() {

      @Override
      public Monad<F> monad() { return monad; }

      @Override
      public Higher1<F, Either<L, R>> value() { return value; }
    };
  }

  static <F extends Kind, L, R> EitherT<F, L, R> right(Monad<F> monad, R right) {
    return lift(monad, Either.right(right));
  }

  static <F extends Kind, L, R> EitherT<F, L, R> left(Monad<F> monad, L left) {
    return lift(monad, Either.left(left));
  }

  static <F extends Kind, R> EitherT<F, Throwable, R> fromOption(Monad<F> monad, Option<R> value) {
    return lift(monad, value.toEither());
  }

  static <F extends Kind, R> EitherT<F, Throwable, R> fromTry(Monad<F> monad, Try<R> value) {
    return lift(monad, value.toEither());
  }

  default <V> Higher1<F, Either<L, V>> flatMapF(Function1<R, Higher1<F, Either<L, V>>> map) {
   return monad().flatMap(value(), v -> v.fold(left -> monad().pure(Either.left(left)), map));
  }
}