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
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.Eq;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.Transformer;

public final class EitherT<W extends Kind, L, R> implements FlatMap3<EitherT.µ, W, L, R> {

  public static final class µ implements Kind {}

  private final Monad<W> monad;
  private final Higher1<W, Either<L, R>> value;

  private EitherT(Monad<W> monad, Higher1<W, Either<L, R>> value) {
    this.monad = requireNonNull(monad);
    this.value = requireNonNull(value);
  }

  @Override
  public <V> EitherT<W, L, V> map(Function1<R, V> map) {
    return EitherT.of(monad, monad.map(value, v -> v.map(map)));
  }

  @Override
  public <V> EitherT<W, L, V> flatMap(Function1<R, ? extends Higher3<EitherT.µ, W, L, V>> map) {
    return EitherT.of(monad, flatMapF(v -> EitherT.narrowK(map.apply(v)).value));
  }

  public <T, V> EitherT<W, T, V> bimap(Function1<L, T> leftMapper, Function1<R, V> rightMapper) {
    return EitherT.of(monad, monad.map(value, v -> v.bimap(leftMapper, rightMapper)));
  }

  public <T> EitherT<W, T, R> mapLeft(Function1<L, T> leftMapper) {
    return EitherT.of(monad, monad.map(value, v -> v.mapLeft(leftMapper)));
  }

  public <V> Higher1<W, V> fold(Function1<L, V> leftMapper, Function1<R, V> rightMapper) {
    return monad.map(value, v -> v.fold(leftMapper, rightMapper));
  }

  public <F extends Kind> EitherT<F, L, R> mapK(Monad<F> other, Transformer<W, F> transformer) {
    return EitherT.of(other, transformer.apply(value));
  }

  public EitherT<W, L, R> filterOrElse(Matcher1<R> filter, Producer<Either<L, R>> orElse) {
    return EitherT.of(monad, monad.map(value, v -> v.filterOrElse(filter, orElse)));
  }

  public EitherT<W, R, L> swap() {
    return EitherT.of(monad, monad.map(value, Either::swap));
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
    return OptionT.of(monad, monad.map(value, Either::toOption));
  }

  Higher1<W, Either<L, R>> value() {
    return value;
  }

  public static <W extends Kind, L, R> EitherT<W, L, R> lift(Monad<W> monad, Either<L, R> either) {
    return of(monad, monad.pure(either));
  }

  public static <W extends Kind, L, R> EitherT<W, L, R> of(Monad<W> monad, Higher1<W, Either<L, R>> either) {
    return new EitherT<>(monad, either);
  }

  public static <W extends Kind, L, R> EitherT<W, L, R> right(Monad<W> monad, R right) {
    return lift(monad, Either.right(right));
  }

  public static <W extends Kind, L, R> EitherT<W, L, R> left(Monad<W> monad, L left) {
    return lift(monad, Either.left(left));
  }

  public static <F extends Kind, L, R> Eq<Higher3<EitherT.µ, F, L, R>> eq(Eq<Higher1<F, Either<L, R>>> eq) {
    return (a, b) -> eq.eqv(narrowK(a).value, narrowK(b).value);
  }

  public static <F extends Kind, L> Monad<Higher1<Higher1<EitherT.µ, F>, L>> monad(Monad<F> monadF) {
    return new Monad<Higher1<Higher1<EitherT.µ, F>, L>>() {

      @Override
      public <T> EitherT<F, L, T> pure(T value) {
        return right(monadF, value);
      }

      @Override
      public <T, R> EitherT<F, L, R> flatMap(Higher1<Higher1<Higher1<EitherT.µ, F>, L>, T> value,
          Function1<T, ? extends Higher1<Higher1<Higher1<EitherT.µ, F>, L>, R>> map) {
        return narrowK(value).flatMap(map.andThen(EitherT::narrowK));
      }
    };
  }

  public static <F extends Kind, E> MonadError<Higher1<Higher1<EitherT.µ, F>, E>, E> monadError(Monad<F> monadF) {
    return new MonadError<Higher1<Higher1<EitherT.µ, F>, E>, E>() {

      @Override
      public <A> EitherT<F, E, A> raiseError(E error) {
        return left(monadF, error);
      }

      @Override
      public <T> EitherT<F, E, T> pure(T value) {
        return right(monadF, value);
      }

      @Override
      public <A> EitherT<F, E, A> handleErrorWith(Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A> value,
          Function1<E, ? extends Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A>> handler) {
        return EitherT.of(monadF,
            monadF.flatMap(EitherT.narrowK(value).value,
                either -> either.fold(e -> handler.andThen(EitherT::narrowK).apply(e).value,
                    a -> monadF.pure(Either.right(a)))));
      }

      @Override
      public <T, R> EitherT<F, E, R> flatMap(Higher1<Higher1<Higher1<EitherT.µ, F>, E>, T> value,
          Function1<T, ? extends Higher1<Higher1<Higher1<EitherT.µ, F>, E>, R>> map) {
        return narrowK(value).flatMap(map.andThen(EitherT::narrowK));
      }
    };
  }

  public static <F extends Kind, E> MonadError<Higher1<Higher1<EitherT.µ, F>, E>, E> monadError(MonadError<F, E> monadErrorF) {
    return new MonadError<Higher1<Higher1<EitherT.µ, F>, E>, E>() {

      @Override
      public <A> EitherT<F, E, A> raiseError(E error) {
        return EitherT.of(monadErrorF, monadErrorF.raiseError(error));
      }

      @Override
      public <T> EitherT<F, E, T> pure(T value) {
        return right(monadErrorF, value);
      }

      @Override
      public <A> EitherT<F, E, A> handleErrorWith(Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A> value,
          Function1<E, ? extends Higher1<Higher1<Higher1<EitherT.µ, F>, E>, A>> handler) {
        return EitherT.of(monadErrorF, monadErrorF.handleErrorWith(EitherT.narrowK(value).value,
            error -> handler.andThen(EitherT::narrowK).apply(error).value));
      }

      @Override
      public <T, R> EitherT<F, E, R> flatMap(Higher1<Higher1<Higher1<EitherT.µ, F>, E>, T> value,
          Function1<T, ? extends Higher1<Higher1<Higher1<EitherT.µ, F>, E>, R>> map) {
        return narrowK(value).flatMap(map.andThen(EitherT::narrowK));
      }
    };
  }

  public static <W extends Kind, L, R> EitherT<W, L, R> narrowK(Higher3<EitherT.µ, W, L, R> hkt) {
    return (EitherT<W, L, R>) hkt;
  }

  public static <W extends Kind, S, A> EitherT<W, S, A> narrowK(Higher2<Higher1<EitherT.µ, W>, S, A> hkt) {
    return (EitherT<W, S, A>) hkt;
  }

  @SuppressWarnings("unchecked")
  public static <W extends Kind, S, A> EitherT<W, S, A> narrowK(Higher1<Higher1<Higher1<EitherT.µ, W>, S>, A> hkt) {
    // XXX: I don't know why, but compiler says here there's an unsafe cast
    return (EitherT<W, S, A>) hkt;
  }

  private <V> Higher1<W, Either<L, V>> flatMapF(Function1<R, Higher1<W, Either<L, V>>> map) {
   return monad.flatMap(value, v -> v.fold(left -> monad.pure(Either.left(left)), map));
  }
}
