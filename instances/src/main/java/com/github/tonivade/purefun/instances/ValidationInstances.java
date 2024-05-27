/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Eq;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Trampoline;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Validation;
import com.github.tonivade.purefun.type.ValidationOf;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Selective;
import com.github.tonivade.purefun.typeclasses.Semigroup;

@SuppressWarnings("unchecked")
public interface ValidationInstances {

  static <E, T> Eq<Kind<Validation<E, ?>, T>> eq(Eq<E> errorEq, Eq<T> validEq) {
    return (a, b) -> {
      if (a instanceof Validation.Invalid<E, T>(var invalidA) && b instanceof Validation.Invalid<E, T>(var invalidB)) {
        return errorEq.eqv(invalidA, invalidB);
      }
      if (a instanceof Validation.Valid<E, T>(var validA) && b instanceof Validation.Valid<E, T>(var validB)) {
        return validEq.eqv(validA, validB);
      }
      return false;
    };
  }

  static <E> Functor<Validation<E, ?>> functor() {
    return ValidationFunctor.INSTANCE;
  }

  static <E> Applicative<Validation<E, ?>> applicative(Semigroup<E> semigroup) {
    return ValidationApplicative.instance(semigroup);
  }

  static <E> Selective<Validation<E, ?>> selective(Semigroup<E> semigroup) {
    return ValidationSelective.instance(semigroup);
  }

  static <E> Monad<Validation<E, ?>> monad() {
    return ValidationMonad.INSTANCE;
  }

  static <E> MonadError<Validation<E, ?>, E> monadError() {
    return ValidationMonadError.INSTANCE;
  }

  static MonadThrow<Validation<Throwable, ?>> monadThrow() {
    return ValidationMonadThrow.INSTANCE;
  }
}

interface ValidationFunctor<E> extends Functor<Validation<E, ?>> {

  @SuppressWarnings("rawtypes")
  ValidationFunctor INSTANCE = new ValidationFunctor() {};

  @Override
  default <T, R> Validation<E, R> map(Kind<Validation<E, ?>, ? extends T> value,
      Function1<? super T, ? extends R> map) {
    return ValidationOf.toValidation(value).map(map);
  }
}

interface ValidationPure<E> extends Applicative<Validation<E, ?>> {

  @Override
  default <T> Validation<E, T> pure(T value) {
    return Validation.valid(value);
  }
}

interface ValidationApplicative<E> extends ValidationPure<E>, Applicative<Validation<E, ?>> {

  static <E> ValidationApplicative<E> instance(Semigroup<E> semigroup) {
    return () -> semigroup;
  }

  Semigroup<E> semigroup();

  @Override
  default <T, R> Validation<E, R> ap(Kind<Validation<E, ?>, ? extends T> value,
                                     Kind<Validation<E, ?>, ? extends Function1<? super T, ? extends R>> apply) {
    Validation<E, T> validation = value.fix(ValidationOf::toValidation);
    Validation<E, Function1<? super T, ? extends R>> validationF = apply.fix(ValidationOf::toValidation);

    if (validation.isValid() && validationF.isValid()) {
      return Validation.valid(validationF.get().apply(validation.get()));
    } else if (validation.isInvalid() && validationF.isValid()) {
      return Validation.invalid(validation.getError());
    } else if (validation.isValid() && validationF.isInvalid()) {
      return Validation.invalid(validationF.getError());
    }

    return Validation.invalid(semigroup().combine(validation.getError(), validationF.getError()));
  }
}

interface ValidationSelective<E> extends ValidationApplicative<E>, Selective<Validation<E, ?>> {

  static <E> ValidationSelective<E> instance(Semigroup<E> semigroup) {
    return () -> semigroup;
  }

  @Override
  default <A, B> Validation<E, B> select(Kind<Validation<E, ?>, Either<A, B>> value,
                                         Kind<Validation<E, ?>, Function1<? super A, ? extends B>> apply) {
    return Validation.select(value.fix(ValidationOf::toValidation), apply.fix(ValidationOf::toValidation));
  }
}

interface ValidationMonad<E> extends ValidationPure<E>, Monad<Validation<E, ?>> {

  @SuppressWarnings("rawtypes")
  ValidationMonad INSTANCE = new ValidationMonad() {};

  @Override
  default <T, R> Validation<E, R> flatMap(Kind<Validation<E, ?>, ? extends T> value,
      Function1<? super T, ? extends Kind<Validation<E, ?>, ? extends R>> map) {
    return ValidationOf.toValidation(value).flatMap(map.andThen(ValidationOf::toValidation));
  }

  @Override
  default <T, R> Kind<Validation<E, ?>, R> tailRecM(T value,
      Function1<T, ? extends Kind<Validation<E, ?>, Either<T, R>>> map) {
    return loop(value, map).run();
  }

  private <T, R> Trampoline<Kind<Validation<E, ?>, R>> loop(T value, Function1<T, ? extends Kind<Validation<E, ?>, Either<T, R>>> map) {
    return switch (map.andThen(ValidationOf::toValidation).apply(value)) {
      case Validation.Invalid<E, Either<T, R>>(var error) -> Trampoline.done(Validation.invalid(error));
      case Validation.Valid<E, Either<T, R>>(Either.Right<T, R>(var right)) -> Trampoline.done(Validation.valid(right));
      case Validation.Valid<E, Either<T, R>>(Either.Left<T, R>(var left)) -> Trampoline.more(() -> loop(left, map));
    };
  }
}

interface ValidationMonadError<E> extends ValidationMonad<E>, MonadError<Validation<E, ?>, E> {

  @SuppressWarnings("rawtypes")
  ValidationMonadError INSTANCE = new ValidationMonadError() {};

  @Override
  default <A> Validation<E, A> raiseError(E error) {
    return Validation.invalid(error);
  }

  @Override
  default <A> Validation<E, A> handleErrorWith(Kind<Validation<E, ?>, A> value,
      Function1<? super E, ? extends Kind<Validation<E, ?>, ? extends A>> handler) {
    return ValidationOf.toValidation(value).fold(handler.andThen(ValidationOf::toValidation), Validation::valid);
  }
}

interface ValidationMonadThrow
    extends ValidationMonadError<Throwable>,
            MonadThrow<Validation<Throwable, ?>> {

  ValidationMonadThrow INSTANCE = new ValidationMonadThrow() {};
}

