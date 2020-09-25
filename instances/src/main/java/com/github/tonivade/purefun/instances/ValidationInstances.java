/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.type.ValidationOf.toValidation;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Pattern2;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Validation;
import com.github.tonivade.purefun.type.ValidationOf;
import com.github.tonivade.purefun.type.Validation_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bifunctor;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Selective;
import com.github.tonivade.purefun.typeclasses.Semigroup;

@SuppressWarnings("unchecked")
public interface ValidationInstances {

  static <E, T> Eq<Kind<Kind<Validation_, E>, T>> eq(Eq<E> errorEq, Eq<T> validEq) {
    return (a, b) -> Pattern2.<Validation<E, T>, Validation<E, T>, Boolean>build()
      .when((x, y) -> x.isInvalid() && y.isInvalid())
        .then((x, y) -> errorEq.eqv(x.getError(), y.getError()))
      .when((x, y) -> x.isValid() && y.isValid())
        .then((x, y) -> validEq.eqv(x.get(), y.get()))
      .otherwise()
        .returns(false)
      .apply(ValidationOf.narrowK(a), ValidationOf.narrowK(b));
  }

  static <E> Functor<Kind<Validation_, E>> functor() {
    return ValidationFunctor.INSTANCE;
  }

  static Bifunctor<Validation_> bifunctor() {
    return ValidationBifunctor.INSTANCE;
  }

  static <E> Applicative<Kind<Validation_, E>> applicative(Semigroup<E> semigroup) {
    return ValidationApplicative.instance(semigroup);
  }

  static <E> Selective<Kind<Validation_, E>> selective(Semigroup<E> semigroup) {
    return ValidationSelective.instance(semigroup);
  }

  static <E> Monad<Kind<Validation_, E>> monad() {
    return ValidationMonad.INSTANCE;
  }

  static <E> MonadError<Kind<Validation_, E>, E> monadError() {
    return ValidationMonadError.INSTANCE;
  }

  static MonadThrow<Kind<Validation_, Throwable>> monadThrow() {
    return ValidationMonadThrow.INSTANCE;
  }
}

interface ValidationFunctor<E> extends Functor<Kind<Validation_, E>> {

  @SuppressWarnings("rawtypes")
  ValidationFunctor INSTANCE = new ValidationFunctor() {};

  @Override
  default <T, R> Validation<E, R> map(Kind<Kind<Validation_, E>, ? extends T> value, 
      Function1<? super T, ? extends R> map) {
    return ValidationOf.narrowK(value).map(map);
  }
}

interface ValidationBifunctor extends Bifunctor<Validation_> {

  ValidationBifunctor INSTANCE = new ValidationBifunctor() {};

  @Override
  default <A, B, C, D> Validation<C, D> bimap(Kind<Kind<Validation_, A>, ? extends B> value,
      Function1<? super A, ? extends C> leftMap, Function1<? super B, ? extends D> rightMap) {
    return ValidationOf.narrowK(value).bimap(leftMap, rightMap);
  }
}

interface ValidationPure<E> extends Applicative<Kind<Validation_, E>> {

  @Override
  default <T> Validation<E, T> pure(T value) {
    return Validation.<E, T>valid(value);
  }
}

interface ValidationApplicative<E> extends ValidationPure<E>, Applicative<Kind<Validation_, E>> {

  static <E> ValidationApplicative<E> instance(Semigroup<E> semigroup) {
    return () -> semigroup;
  }

  Semigroup<E> semigroup();

  @Override
  default <T, R> Validation<E, R> ap(Kind<Kind<Validation_, E>, ? extends T> value,
                                     Kind<Kind<Validation_, E>, ? extends Function1<? super T, ? extends R>> apply) {
    Validation<E, T> validation = value.fix(ValidationOf::narrowK);
    Validation<E, Function1<? super T, ? extends R>> validationF = apply.fix(ValidationOf::narrowK);

    if (validation.isValid() && validationF.isValid()) {
      return Validation.<E, R>valid(validationF.get().apply(validation.get()));
    } else if (validation.isInvalid() && validationF.isValid()) {
      return Validation.<E, R>invalid(validation.getError());
    } else if (validation.isValid() && validationF.isInvalid()) {
      return Validation.<E, R>invalid(validationF.getError());
    }

    return Validation.<E, R>invalid(semigroup().combine(validation.getError(), validationF.getError()));
  }
}

interface ValidationSelective<E> extends ValidationApplicative<E>, Selective<Kind<Validation_, E>> {

  static <E> ValidationSelective<E> instance(Semigroup<E> semigroup) {
    return () -> semigroup;
  }

  @Override
  default <A, B> Validation<E, B> select(Kind<Kind<Validation_, E>, Either<A, B>> value,
                                         Kind<Kind<Validation_, E>, Function1<? super A, ? extends B>> apply) {
    return Validation.select(value.fix(toValidation()), apply.fix(toValidation()));
  }
}

interface ValidationMonad<E> extends ValidationPure<E>, Monad<Kind<Validation_, E>> {

  @SuppressWarnings("rawtypes")
  ValidationMonad INSTANCE = new ValidationMonad() {};

  @Override
  default <T, R> Validation<E, R> flatMap(Kind<Kind<Validation_, E>, ? extends T> value,
      Function1<? super T, ? extends Kind<Kind<Validation_, E>, ? extends R>> map) {
    return ValidationOf.narrowK(value).flatMap(map.andThen(ValidationOf::narrowK));
  }
}

interface ValidationMonadError<E> extends ValidationMonad<E>, MonadError<Kind<Validation_, E>, E> {

  @SuppressWarnings("rawtypes")
  ValidationMonadError INSTANCE = new ValidationMonadError() {};

  @Override
  default <A> Validation<E, A> raiseError(E error) {
    return Validation.<E, A>invalid(error);
  }

  @Override
  default <A> Validation<E, A> handleErrorWith(Kind<Kind<Validation_, E>, A> value,
      Function1<? super E, ? extends Kind<Kind<Validation_, E>, ? extends A>> handler) {
    return ValidationOf.narrowK(value).fold(handler.andThen(ValidationOf::narrowK), Validation::<E, A>valid);
  }
}

interface ValidationMonadThrow
    extends ValidationMonadError<Throwable>,
            MonadThrow<Kind<Validation_, Throwable>> {

  ValidationMonadThrow INSTANCE = new ValidationMonadThrow() {};
}

