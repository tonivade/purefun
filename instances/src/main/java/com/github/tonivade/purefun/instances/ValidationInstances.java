/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Pattern2;
import com.github.tonivade.purefun.type.Validation;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bifunctor;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;

public interface ValidationInstances {

  static <E, T> Eq<Higher2<Validation.µ, E, T>> eq(Eq<E> errorEq, Eq<T> validEq) {
    return (a, b) -> Pattern2.<Validation<E, T>, Validation<E, T>, Boolean>build()
      .when((x, y) -> x.isInvalid() && y.isInvalid())
        .then((x, y) -> errorEq.eqv(x.getError(), y.getError()))
      .when((x, y) -> x.isValid() && y.isValid())
        .then((x, y) -> validEq.eqv(x.get(), y.get()))
      .otherwise()
        .returns(false)
      .apply(Validation.narrowK(a), Validation.narrowK(b));
  }

  static <E> Functor<Higher1<Validation.µ, E>> functor() {
    return new ValidationFunctor<E>() {};
  }

  static Bifunctor<Validation.µ> bifunctor() {
    return new ValidationBifunctor() {};
  }

  static <E> Applicative<Higher1<Validation.µ, E>> applicative() {
    return new ValidationApplicative<E>() {};
  }

  static <E> Monad<Higher1<Validation.µ, E>> monad() {
    return new ValidationMonad<E>() {};
  }

  static <E> MonadError<Higher1<Validation.µ, E>, E> monadError() {
    return new ValidationMonadError<E>() {};
  }

  static MonadThrow<Higher1<Validation.µ, Throwable>> monadThrow() {
    return new ValidationMonadThrow() {};
  }
}

@Instance
interface ValidationFunctor<E> extends Functor<Higher1<Validation.µ, E>> {

  @Override
  default <T, R> Validation<E, R> map(Higher1<Higher1<Validation.µ, E>, T> value, Function1<T, R> map) {
    return Validation.narrowK(value).map(map);
  }
}

@Instance
interface ValidationBifunctor extends Bifunctor<Validation.µ> {

  @Override
  default <A, B, C, D> Validation<C, D> bimap(Higher2<Validation.µ, A, B> value,
      Function1<A, C> leftMap, Function1<B, D> rightMap) {
    return Validation.narrowK(value).mapError(leftMap).map(rightMap);
  }
}

@Instance
interface ValidationPure<E> extends Applicative<Higher1<Validation.µ, E>> {

  @Override
  default <T> Validation<E, T> pure(T value) {
    return Validation.valid(value);
  }
}

@Instance
interface ValidationApplicative<E> extends ValidationPure<E> {

  @Override
  default <T, R> Validation<E, R> ap(Higher1<Higher1<Validation.µ, E>, T> value,
      Higher1<Higher1<Validation.µ, E>, Function1<T, R>> apply) {
    return Validation.narrowK(value).flatMap(t -> Validation.narrowK(apply).map(f -> f.apply(t)));
  }
}

@Instance
interface ValidationMonad<E> extends ValidationPure<E>, Monad<Higher1<Validation.µ, E>> {

  @Override
  default <T, R> Validation<E, R> flatMap(Higher1<Higher1<Validation.µ, E>, T> value,
      Function1<T, ? extends Higher1<Higher1<Validation.µ, E>, R>> map) {
    return Validation.narrowK(value).flatMap(map.andThen(Validation::narrowK));
  }
}

@Instance
interface ValidationMonadError<E> extends ValidationMonad<E>, MonadError<Higher1<Validation.µ, E>, E> {

  @Override
  default <A> Validation<E, A> raiseError(E error) {
    return Validation.invalid(error);
  }

  @Override
  default <A> Validation<E, A> handleErrorWith(Higher1<Higher1<Validation.µ, E>, A> value,
      Function1<E, ? extends Higher1<Higher1<Validation.µ, E>, A>> handler) {
    return Validation.narrowK(value).fold(handler.andThen(Validation::narrowK), Validation::valid);
  }
}

@Instance
interface ValidationMonadThrow
    extends ValidationMonadError<Throwable>,
            MonadThrow<Higher1<Validation.µ, Throwable>> { }