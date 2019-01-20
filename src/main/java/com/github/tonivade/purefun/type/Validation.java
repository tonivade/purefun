/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.FlatMap2;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Function3;
import com.github.tonivade.purefun.Function4;
import com.github.tonivade.purefun.Function5;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Holder;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Pattern2;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.BiFunctor;
import com.github.tonivade.purefun.typeclasses.Eq;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;

public interface Validation<E, T> extends Holder<T>, FlatMap2<Validation.µ, E, T> {

  final class µ implements Kind {}

  static <E, T> Validation<E, T> valid(T value) {
    return new Valid<>(value);
  }

  static <E, T> Validation<E, T> invalid(E error) {
    return new Invalid<>(error);
  }

  static <E, T> Validation<E, T> narrowK(Higher2<Validation.µ, E, T> hkt) {
    return (Validation<E, T>) hkt;
  }

  static <E, T> Validation<E, T> narrowK(Higher1<Higher1<Validation.µ, E>, T> hkt) {
    return (Validation<E, T>) hkt;
  }

  boolean isValid();
  boolean isInvalid();

  E getError();

  @Override
  default <R> Validation<E, R> map(Function1<T, R> mapper) {
    if (isValid()) {
      return valid(mapper.apply(get()));
    }
    return invalid(getError());
  }

  default <U> Validation<U, T> mapError(Function1<E, U> mapper) {
    if (isInvalid()) {
      return invalid(mapper.apply(getError()));
    }
    return valid(get());
  }

  @Override
  default <R> Validation<E, R> flatMap(Function1<T, ? extends Higher2<Validation.µ, E, R>> mapper) {
    if (isValid()) {
      return mapper.andThen(Validation::narrowK).apply(get());
    }
    return invalid(getError());
  }

  default Option<Validation<E, T>> filter(Matcher1<T> matcher) {
    if (isInvalid() || matcher.match(get())) {
      return Option.some(this);
    }
    return Option.none();
  }

  default Validation<E, T> filterOrElse(Matcher1<T> matcher, Producer<Validation<E, T>> orElse) {
    if (isInvalid() || matcher.match(get())) {
      return this;
    }
    return orElse.get();
  }

  default Validation<E, T> orElse(Validation<E, T> orElse) {
    if (isInvalid()) {
      return orElse;
    }
    return this;
  }

  default T getOrElse(T value) {
    return getOrElse(Producer.cons(value));
  }

  default T getOrElse(Producer<T> orElse) {
    if (isValid()) {
      return get();
    }
    return orElse.get();
  }

  default <U> U fold(Function1<E, U> invalidMap, Function1<T, U> validMap) {
    if (isValid()) {
      return validMap.apply(get());
    }
    return invalidMap.apply(getError());
  }

  default <R> Validation<Sequence<E>, R> ap(Validation<Sequence<E>, Function1<T, R>> other) {
    if (this.isValid() && other.isValid()) {
      return valid(other.get().apply(get()));
    }
    if (this.isInvalid() && other.isInvalid()) {
      return invalid(other.getError().append(getError()));
    }
    if (this.isInvalid() && other.isValid()) {
      return invalid(listOf(getError()));
    }
    return invalid(other.getError());
  }

  default Either<E, T> toEither() {
    if (isValid()) {
      return Either.right(get());
    }
    return Either.left(getError());
  }

  @Override
  @SuppressWarnings("unchecked")
  default <V> Validation<E, V> flatten() {
    try {
      return ((Validation<E, Validation<E, V>>) this).flatMap(identity());
    } catch (ClassCastException e) {
      throw new UnsupportedOperationException("cannot be flattened");
    }
  }

  static <E, T> Eq<Higher2<Validation.µ, E, T>> eq(Eq<E> errorEq, Eq<T> validEq) {
    return (a, b) -> Pattern2.<Validation<E, T>, Validation<E, T>, Boolean>build()
      .when((x, y) -> x.isInvalid() && y.isInvalid())
        .then((x, y) -> errorEq.eqv(x.getError(), y.getError()))
      .when((x, y) -> x.isValid() && y.isValid())
        .then((x, y) -> validEq.eqv(x.get(), y.get()))
      .otherwise()
        .returns(false)
      .apply(narrowK(a), narrowK(b));
  }

  static <E> Functor<Higher1<Validation.µ, E>> functor() {
    return new ValidationFunctor<E>() {};
  }

  static BiFunctor<Validation.µ> bifunctor() {
    return new ValidationBiFunctor() {};
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

  ValidationModule module();

  static <E, T1, T2, R> Validation<Sequence<E>, R> map2(Validation<E, T1> validation1,
                                                        Validation<E, T2> validation2,
                                                        Function2<T1, T2, R> mapper) {
    return validation2.ap(validation1.ap(valid(mapper.curried())));
  }

  static <E, T1, T2, T3, R> Validation<Sequence<E>, R> map3(Validation<E, T1> validation1,
                                                            Validation<E, T2> validation2,
                                                            Validation<E, T3> validation3,
                                                            Function3<T1, T2, T3, R> mapper) {
    return validation3.ap(map2(validation1, validation2, (t1, t2) -> mapper.curried().apply(t1).apply(t2)));
  }

  static <E, T1, T2, T3, T4, R> Validation<Sequence<E>, R> map4(Validation<E, T1> validation1,
                                                                Validation<E, T2> validation2,
                                                                Validation<E, T3> validation3,
                                                                Validation<E, T4> validation4,
                                                                Function4<T1, T2, T3, T4, R> mapper) {
    return validation4.ap(map3(validation1, validation2, validation3,
        (t1, t2, t3) -> mapper.curried().apply(t1).apply(t2).apply(t3)));
  }

  static <E, T1, T2, T3, T4, T5, R> Validation<Sequence<E>, R> map5(Validation<E, T1> validation1,
                                                                    Validation<E, T2> validation2,
                                                                    Validation<E, T3> validation3,
                                                                    Validation<E, T4> validation4,
                                                                    Validation<E, T5> validation5,
                                                                    Function5<T1, T2, T3, T4, T5, R> mapper) {
    return validation5.ap(map4(validation1, validation2, validation3, validation4,
        (t1, t2, t3, t4) -> mapper.curried().apply(t1).apply(t2).apply(t3).apply(t4)));
  }

  final class Valid<E, T> implements Validation<E, T>, Serializable {

    private static final long serialVersionUID = -4276395187736455243L;

    private final T value;

    private Valid(T value) {
      this.value = requireNonNull(value);
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public boolean isInvalid() {
      return false;
    }

    @Override
    public T get() {
      return value;
    }

    @Override
    public E getError() {
      throw new NoSuchElementException("valid value");
    }

    @Override
    public ValidationModule module() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
      return Equal.of(this)
          .comparing(Valid::get)
          .applyTo(obj);
    }

    @Override
    public String toString() {
      return "Valid(" + value + ")";
    }
  }

  final class Invalid<E, T> implements Validation<E, T>, Serializable {

    private static final long serialVersionUID = -5116403366555721062L;

    private final E error;

    private Invalid(E error) {
      this.error = requireNonNull(error);
    }

    @Override
    public boolean isValid() {
      return false;
    }

    @Override
    public boolean isInvalid() {
      return true;
    }

    @Override
    public T get() {
      throw new NoSuchElementException("invalid value");
    }

    @Override
    public E getError() {
      return error;
    }

    @Override
    public ValidationModule module() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
      return Objects.hash(error);
    }

    @Override
    public boolean equals(Object obj) {
      return Equal.of(this)
          .comparing(Invalid::getError)
          .applyTo(obj);
    }

    @Override
    public String toString() {
      return "Invalid(" + error + ")";
    }
  }
}

interface ValidationModule {}

interface ValidationFunctor<E> extends Functor<Higher1<Validation.µ, E>> {

  @Override
  default <T, R> Validation<E, R> map(Higher1<Higher1<Validation.µ, E>, T> value, Function1<T, R> map) {
    return Validation.narrowK(value).map(map);
  }
}

interface ValidationBiFunctor extends BiFunctor<Validation.µ> {

  @Override
  default <A, B, C, D> Validation<C, D> bimap(Higher2<Validation.µ, A, B> value,
      Function1<A, C> leftMap, Function1<B, D> rightMap) {
    return Validation.narrowK(value).mapError(leftMap).map(rightMap);
  }
}

interface ValidationPure<E> extends Applicative<Higher1<Validation.µ, E>> {

  @Override
  default <T> Validation<E, T> pure(T value) {
    return Validation.valid(value);
  }
}

interface ValidationApply<E> extends Applicative<Higher1<Validation.µ, E>> {

  @Override
  default <T, R> Validation<E, R> ap(Higher1<Higher1<Validation.µ, E>, T> value,
      Higher1<Higher1<Validation.µ, E>, Function1<T, R>> apply) {
    return Validation.narrowK(value).flatMap(t -> Validation.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface ValidationApplicative<E> extends ValidationPure<E>, ValidationApply<E> { }

interface ValidationMonad<E> extends ValidationPure<E>, Monad<Higher1<Validation.µ, E>> {

  @Override
  default <T, R> Validation<E, R> flatMap(Higher1<Higher1<Validation.µ, E>, T> value,
      Function1<T, ? extends Higher1<Higher1<Validation.µ, E>, R>> map) {
    return Validation.narrowK(value).flatMap(map.andThen(Validation::narrowK));
  }
}

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