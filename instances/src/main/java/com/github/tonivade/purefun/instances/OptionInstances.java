/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Producer.cons;
import static com.github.tonivade.purefun.core.Unit.unit;
import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Eq;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Trampoline;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.EvalOf;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.OptionOf;
import com.github.tonivade.purefun.typeclasses.Alternative;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonoidK;
import com.github.tonivade.purefun.typeclasses.SemigroupK;
import com.github.tonivade.purefun.typeclasses.Semigroupal;
import com.github.tonivade.purefun.typeclasses.Traverse;

public interface OptionInstances {

  static <T> Eq<Kind<Option<?>, T>> eq(Eq<T> eqSome) {
    return (a, b) -> {
      if (a instanceof Option.Some<T>(var valueA) && b instanceof Option.Some<T>(var valueB)) {
        return eqSome.eqv(valueA, valueB);
      }
      return a instanceof Option.None && b instanceof Option.None;
    };
  }

  static Functor<Option<?>> functor() {
    return OptionFunctor.INSTANCE;
  }

  static Applicative<Option<?>> applicative() {
    return OptionApplicative.INSTANCE;
  }

  static Alternative<Option<?>> alternative() {
    return OptionAlternative.INSTANCE;
  }

  static Monad<Option<?>> monad() {
    return OptionMonad.INSTANCE;
  }

  static MonadError<Option<?>, Unit> monadError() {
    return OptionMonadError.INSTANCE;
  }

  static Traverse<Option<?>> traverse() {
    return OptionTraverse.INSTANCE;
  }

  static Semigroupal<Option<?>> semigroupal() {
    return OptionSemigroupal.INSTANCE;
  }

  static Foldable<Option<?>> foldable() {
    return OptionFoldable.INSTANCE;
  }
}

interface OptionFunctor extends Functor<Option<?>> {

  OptionFunctor INSTANCE = new OptionFunctor() {};

  @Override
  default <T, R> Kind<Option<?>, R> map(Kind<Option<?>, ? extends T> value, Function1<? super T, ? extends R> mapper) {
    return OptionOf.toOption(value).map(mapper);
  }
}

interface OptionPure extends Applicative<Option<?>> {

  @Override
  default <T> Kind<Option<?>, T> pure(T value) {
    return Option.some(value);
  }
}

interface OptionApplicative extends OptionPure {

  OptionApplicative INSTANCE = new OptionApplicative() {};

  @Override
  default <T, R> Kind<Option<?>, R> ap(Kind<Option<?>, ? extends T> value,
      Kind<Option<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return value.fix(OptionOf::toOption).flatMap(t -> OptionOf.toOption(apply).map(f -> f.apply(t)));
  }
}

interface OptionMonad extends OptionPure, Monad<Option<?>> {

  OptionMonad INSTANCE = new OptionMonad() {};

  @Override
  default <T, R> Kind<Option<?>, R> flatMap(Kind<Option<?>, ? extends T> value,
      Function1<? super T, ? extends Kind<Option<?>, ? extends R>> map) {
    return value.fix(OptionOf::toOption).flatMap(map.andThen(OptionOf::toOption));
  }

  @Override
  default <T, R> Kind<Option<?>, R> tailRecM(T value, Function1<T, ? extends Kind<Option<?>, Either<T, R>>> map) {
    return loop(value, map).run();
  }

  private <T, R> Trampoline<Kind<Option<?>, R>> loop(T value, Function1<T, ? extends Kind<Option<?>, Either<T, R>>> map) {
    return switch (map.andThen(OptionOf::toOption).apply(value)) {
      case Option.None<Either<T, R>> n -> Trampoline.done(Option.none());
      case Option.Some<Either<T, R>>(Either.Right<T, R>(var right)) -> Trampoline.done(Option.some(right));
      case Option.Some<Either<T, R>>(Either.Left<T, R>(var left)) -> Trampoline.more(() -> loop(left, map));
    };
  }
}

interface OptionSemigroupK extends SemigroupK<Option<?>> {

  OptionSemigroupK INSTANCE = new OptionSemigroupK() {};

  @Override
  default <T> Kind<Option<?>, T> combineK(Kind<Option<?>, ? extends T> t1, Kind<Option<?>, ? extends T> t2) {
    return OptionOf.toOption(t1).fold(cons(OptionOf.toOption(t2)), Option::some);
  }
}

interface OptionMonoidK extends OptionSemigroupK, MonoidK<Option<?>> {

  OptionMonoidK INSTANCE = new OptionMonoidK() {};

  @Override
  default <T> Kind<Option<?>, T> zero() {
    return Option.none();
  }
}

interface OptionAlternative extends OptionMonoidK, OptionApplicative, Alternative<Option<?>> {

  OptionAlternative INSTANCE = new OptionAlternative() {};
}

interface OptionMonadError extends OptionMonad, MonadError<Option<?>, Unit> {

  OptionMonadError INSTANCE = new OptionMonadError() {};

  @Override
  default <A> Kind<Option<?>, A> raiseError(Unit error) {
    return Option.none();
  }

  @Override
  default <A> Kind<Option<?>, A> handleErrorWith(Kind<Option<?>, A> value,
      Function1<? super Unit, ? extends Kind<Option<?>, ? extends A>> handler) {
    return OptionOf.toOption(value).fold(() -> OptionOf.toOption(handler.apply(unit())), this::pure);
  }
}

interface OptionFoldable extends Foldable<Option<?>> {

  OptionFoldable INSTANCE = new OptionFoldable() {};

  @Override
  default <A, B> B foldLeft(Kind<Option<?>, ? extends A> value, B initial, Function2<? super B, ? super A, ? extends B> mapper) {
    return OptionOf.toOption(value).fold(cons(initial), a -> mapper.apply(initial, a));
  }

  @Override
  default <A, B> Eval<B> foldRight(Kind<Option<?>, ? extends A> value, Eval<? extends B> initial,
      Function2<? super A, ? super Eval<? extends B>, ? extends Eval<? extends B>> mapper) {
    return OptionOf.<A>toOption(value).fold(
        cons(initial).andThen(EvalOf::<B>toEval), a -> mapper.andThen(EvalOf::<B>toEval).apply(a, initial));
  }
}

interface OptionTraverse extends Traverse<Option<?>>, OptionFoldable {

  OptionTraverse INSTANCE = new OptionTraverse() {};

  @Override
  default <G extends Kind<G, ?>, T, R> Kind<G, Kind<Option<?>, R>> traverse(
      Applicative<G> applicative, Kind<Option<?>, T> value,
      Function1<? super T, ? extends Kind<G, ? extends R>> mapper) {
    return value.fix(OptionOf::toOption).fold(
        () -> applicative.pure(Option.<R>none().kind()),
        t -> {
          Kind<G, ? extends R> apply = mapper.apply(t);
          return applicative.map(apply, Option::some);
        });
  }
}

interface OptionSemigroupal extends Semigroupal<Option<?>> {

  OptionSemigroupal INSTANCE = new OptionSemigroupal() {};

  @Override
  default <A, B> Kind<Option<?>, Tuple2<A, B>> product(Kind<Option<?>, ? extends A> fa, Kind<Option<?>, ? extends B> fb) {
    return OptionOf.toOption(fa).flatMap(a -> OptionOf.toOption(fb).map(b -> Tuple.of(a, b)));
  }
}
