/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.type.ValidationOf.toValidation;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Eq;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.free.Trampoline;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Validation;
import com.github.tonivade.purefun.type.ValidationOf;
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

  static <E, T> Eq<Kind<Kind<Validation<?, ?>, E>, T>> eq(Eq<E> errorEq, Eq<T> validEq) {
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

  static <E> Functor<Kind<Validation<?, ?>, E>> functor() {
    return ValidationFunctor.INSTANCE;
  }

  static Bifunctor<Validation<?, ?>> bifunctor() {
    return ValidationBifunctor.INSTANCE;
  }

  static <E> Applicative<Kind<Validation<?, ?>, E>> applicative(Semigroup<E> semigroup) {
    return ValidationApplicative.instance(semigroup);
  }

  static <E> Selective<Kind<Validation<?, ?>, E>> selective(Semigroup<E> semigroup) {
    return ValidationSelective.instance(semigroup);
  }

  static <E> Monad<Kind<Validation<?, ?>, E>> monad() {
    return ValidationMonad.INSTANCE;
  }

  static <E> MonadError<Kind<Validation<?, ?>, E>, E> monadError() {
    return ValidationMonadError.INSTANCE;
  }

  static MonadThrow<Kind<Validation<?, ?>, Throwable>> monadThrow() {
    return ValidationMonadThrow.INSTANCE;
  }
}

interface ValidationFunctor<E> extends Functor<Kind<Validation<?, ?>, E>> {

  @SuppressWarnings("rawtypes")
  ValidationFunctor INSTANCE = new ValidationFunctor() {};

  @Override
  default <T, R> Validation<E, R> map(Kind<Kind<Validation<?, ?>, E>, ? extends T> value,
      Function1<? super T, ? extends R> map) {
    return ValidationOf.narrowK(value).map(map);
  }
}

interface ValidationBifunctor extends Bifunctor<Validation<?, ?>> {

  ValidationBifunctor INSTANCE = new ValidationBifunctor() {};

  @Override
  default <A, B, C, D> Validation<C, D> bimap(Kind<Kind<Validation<?, ?>, A>, ? extends B> value,
      Function1<? super A, ? extends C> leftMap, Function1<? super B, ? extends D> rightMap) {
    return ValidationOf.narrowK(value).bimap(leftMap, rightMap);
  }
}

interface ValidationPure<E> extends Applicative<Kind<Validation<?, ?>, E>> {

  @Override
  default <T> Validation<E, T> pure(T value) {
    return Validation.valid(value);
  }
}

interface ValidationApplicative<E> extends ValidationPure<E>, Applicative<Kind<Validation<?, ?>, E>> {

  static <E> ValidationApplicative<E> instance(Semigroup<E> semigroup) {
    return () -> semigroup;
  }

  Semigroup<E> semigroup();

  @Override
  default <T, R> Validation<E, R> ap(Kind<Kind<Validation<?, ?>, E>, ? extends T> value,
                                     Kind<Kind<Validation<?, ?>, E>, ? extends Function1<? super T, ? extends R>> apply) {
    Validation<E, T> validation = value.fix(ValidationOf::narrowK);
    Validation<E, Function1<? super T, ? extends R>> validationF = apply.fix(ValidationOf::narrowK);

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

interface ValidationSelective<E> extends ValidationApplicative<E>, Selective<Kind<Validation<?, ?>, E>> {

  static <E> ValidationSelective<E> instance(Semigroup<E> semigroup) {
    return () -> semigroup;
  }

  @Override
  default <A, B> Validation<E, B> select(Kind<Kind<Validation<?, ?>, E>, Either<A, B>> value,
                                         Kind<Kind<Validation<?, ?>, E>, Function1<? super A, ? extends B>> apply) {
    return Validation.select(value.fix(toValidation()), apply.fix(toValidation()));
  }
}

interface ValidationMonad<E> extends ValidationPure<E>, Monad<Kind<Validation<?, ?>, E>> {

  @SuppressWarnings("rawtypes")
  ValidationMonad INSTANCE = new ValidationMonad() {};

  @Override
  default <T, R> Validation<E, R> flatMap(Kind<Kind<Validation<?, ?>, E>, ? extends T> value,
      Function1<? super T, ? extends Kind<Kind<Validation<?, ?>, E>, ? extends R>> map) {
    return ValidationOf.narrowK(value).flatMap(map.andThen(ValidationOf::narrowK));
  }

  @Override
  default <T, R> Kind<Kind<Validation<?, ?>, E>, R> tailRecM(T value,
      Function1<T, ? extends Kind<Kind<Validation<?, ?>, E>, Either<T, R>>> map) {
    return loop(value, map).run();
  }

  private <T, R> Trampoline<Kind<Kind<Validation<?, ?>, E>, R>> loop(T value, Function1<T, ? extends Kind<Kind<Validation<?, ?>, E>, Either<T, R>>> map) {
    return switch (map.andThen(ValidationOf::narrowK).apply(value)) {
      case Validation.Invalid<E, Either<T, R>>(var error) -> Trampoline.done(Validation.invalid(error));
      case Validation.Valid<E, Either<T, R>>(Either.Right<T, R>(var right)) -> Trampoline.done(Validation.valid(right));
      case Validation.Valid<E, Either<T, R>>(Either.Left<T, R>(var left)) -> Trampoline.more(() -> loop(left, map));
    };
  }
}

interface ValidationMonadError<E> extends ValidationMonad<E>, MonadError<Kind<Validation<?, ?>, E>, E> {

  @SuppressWarnings("rawtypes")
  ValidationMonadError INSTANCE = new ValidationMonadError() {};

  @Override
  default <A> Validation<E, A> raiseError(E error) {
    return Validation.invalid(error);
  }

  @Override
  default <A> Validation<E, A> handleErrorWith(Kind<Kind<Validation<?, ?>, E>, A> value,
      Function1<? super E, ? extends Kind<Kind<Validation<?, ?>, E>, ? extends A>> handler) {
    return ValidationOf.narrowK(value).fold(handler.andThen(ValidationOf::narrowK), Validation::valid);
  }
}

interface ValidationMonadThrow
    extends ValidationMonadError<Throwable>,
            MonadThrow<Kind<Validation<?, ?>, Throwable>> {

  ValidationMonadThrow INSTANCE = new ValidationMonadThrow() {};
}

