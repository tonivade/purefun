/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Producer.cons;
import static com.github.tonivade.purefun.core.Unit.unit;
import static com.github.tonivade.purefun.type.OptionOf.toOption;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Eq;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.EvalOf;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.OptionOf;
import com.github.tonivade.purefun.type.Option_;
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

  static <T> Eq<Kind<Option_, T>> eq(Eq<T> eqSome) {
    return (a, b) -> {
      if (a instanceof Option.Some<T>(var valueA) && b instanceof Option.Some<T>(var valueB)) {
        return eqSome.eqv(valueA, valueB);
      }
      return a instanceof Option.None && b instanceof Option.None;
    };
  }

  static Functor<Option_> functor() {
    return OptionFunctor.INSTANCE;
  }

  static Applicative<Option_> applicative() {
    return OptionApplicative.INSTANCE;
  }

  static Alternative<Option_> alternative() {
    return OptionAlternative.INSTANCE;
  }

  static Monad<Option_> monad() {
    return OptionMonad.INSTANCE;
  }

  static MonadError<Option_, Unit> monadError() {
    return OptionMonadError.INSTANCE;
  }

  static Traverse<Option_> traverse() {
    return OptionTraverse.INSTANCE;
  }

  static Semigroupal<Option_> semigroupal() {
    return OptionSemigroupal.INSTANCE;
  }

  static Foldable<Option_> foldable() {
    return OptionFoldable.INSTANCE;
  }
}

interface OptionFunctor extends Functor<Option_> {

  OptionFunctor INSTANCE = new OptionFunctor() {};

  @Override
  default <T, R> Kind<Option_, R> map(Kind<Option_, ? extends T> value, Function1<? super T, ? extends R> mapper) {
    return OptionOf.narrowK(value).map(mapper);
  }
}

interface OptionPure extends Applicative<Option_> {

  @Override
  default <T> Kind<Option_, T> pure(T value) {
    return Option.some(value);
  }
}

interface OptionApplicative extends OptionPure {

  OptionApplicative INSTANCE = new OptionApplicative() {};

  @Override
  default <T, R> Kind<Option_, R> ap(Kind<Option_, ? extends T> value,
      Kind<Option_, ? extends Function1<? super T, ? extends R>> apply) {
    return value.fix(toOption()).flatMap(t -> OptionOf.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface OptionMonad extends OptionPure, Monad<Option_> {

  OptionMonad INSTANCE = new OptionMonad() {};

  @Override
  default <T, R> Kind<Option_, R> flatMap(Kind<Option_, ? extends T> value,
      Function1<? super T, ? extends Kind<Option_, ? extends R>> map) {
    return value.fix(toOption()).flatMap(map.andThen(OptionOf::narrowK));
  }
}

interface OptionSemigroupK extends SemigroupK<Option_> {

  OptionSemigroupK INSTANCE = new OptionSemigroupK() {};

  @Override
  default <T> Kind<Option_, T> combineK(Kind<Option_, ? extends T> t1, Kind<Option_, ? extends T> t2) {
    return OptionOf.narrowK(t1).fold(cons(OptionOf.narrowK(t2)), Option::some);
  }
}

interface OptionMonoidK extends OptionSemigroupK, MonoidK<Option_> {

  OptionMonoidK INSTANCE = new OptionMonoidK() {};

  @Override
  default <T> Kind<Option_, T> zero() {
    return Option.none();
  }
}

interface OptionAlternative extends OptionMonoidK, OptionApplicative, Alternative<Option_> {

  OptionAlternative INSTANCE = new OptionAlternative() {};
}

interface OptionMonadError extends OptionMonad, MonadError<Option_, Unit> {

  OptionMonadError INSTANCE = new OptionMonadError() {};

  @Override
  default <A> Kind<Option_, A> raiseError(Unit error) {
    return Option.none();
  }

  @Override
  default <A> Kind<Option_, A> handleErrorWith(Kind<Option_, A> value,
      Function1<? super Unit, ? extends Kind<Option_, ? extends A>> handler) {
    return OptionOf.narrowK(value).fold(() -> OptionOf.narrowK(handler.apply(unit())), this::pure);
  }
}

interface OptionFoldable extends Foldable<Option_> {

  OptionFoldable INSTANCE = new OptionFoldable() {};

  @Override
  default <A, B> B foldLeft(Kind<Option_, ? extends A> value, B initial, Function2<? super B, ? super A, ? extends B> mapper) {
    return OptionOf.narrowK(value).fold(cons(initial), a -> mapper.apply(initial, a));
  }

  @Override
  default <A, B> Eval<B> foldRight(Kind<Option_, ? extends A> value, Eval<? extends B> initial,
      Function2<? super A, ? super Eval<? extends B>, ? extends Eval<? extends B>> mapper) {
    return OptionOf.<A>narrowK(value).fold(
        cons(initial).andThen(EvalOf::<B>narrowK), a -> mapper.andThen(EvalOf::<B>narrowK).apply(a, initial));
  }
}

interface OptionTraverse extends Traverse<Option_>, OptionFoldable {

  OptionTraverse INSTANCE = new OptionTraverse() {};

  @Override
  default <G extends Witness, T, R> Kind<G, Kind<Option_, R>> traverse(
      Applicative<G> applicative, Kind<Option_, T> value,
      Function1<? super T, ? extends Kind<G, ? extends R>> mapper) {
    return value.fix(toOption()).fold(
        () -> applicative.pure(Option.<R>none().kind()),
        t -> {
          Kind<G, ? extends R> apply = mapper.apply(t);
          return applicative.map(apply, Option::some);
        });
  }
}

interface OptionSemigroupal extends Semigroupal<Option_> {

  OptionSemigroupal INSTANCE = new OptionSemigroupal() {};

  @Override
  default <A, B> Kind<Option_, Tuple2<A, B>> product(Kind<Option_, ? extends A> fa, Kind<Option_, ? extends B> fb) {
    return OptionOf.narrowK(fa).flatMap(a -> OptionOf.narrowK(fb).map(b -> Tuple.of(a, b)));
  }
}
