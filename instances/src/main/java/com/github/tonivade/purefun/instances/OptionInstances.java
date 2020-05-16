/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Pattern2;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Option;
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

import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Unit.unit;

public interface OptionInstances {

  static <T> Eq<Higher1<Option_, T>> eq(Eq<T> eqSome) {
    return (a, b) -> Pattern2.<Option<T>, Option<T>, Boolean>build()
      .when((x, y) -> x.isPresent() && y.isPresent())
        .then((x, y) -> eqSome.eqv(x.get(), y.get()))
      .when((x, y) -> x.isEmpty() && y.isEmpty())
        .returns(true)
      .otherwise()
        .returns(false)
      .apply(Option_.narrowK(a), Option_.narrowK(b));
  }

  static Functor<Option_> functor() {
    return OptionFunctor.instance();
  }

  static Applicative<Option_> applicative() {
    return OptionApplicative.instance();
  }

  static Alternative<Option_> alternative() {
    return OptionAlternative.instance();
  }

  static Monad<Option_> monad() {
    return OptionMonad.instance();
  }

  static MonadError<Option_, Unit> monadError() {
    return OptionMonadError.instance();
  }

  static Traverse<Option_> traverse() {
    return OptionTraverse.instance();
  }

  static Semigroupal<Option_> semigroupal() {
    return OptionSemigroupal.instance();
  }

  static Foldable<Option_> foldable() {
    return OptionFoldable.instance();
  }
}

@Instance
interface OptionFunctor extends Functor<Option_> {

  @Override
  default <T, R> Higher1<Option_, R> map(Higher1<Option_, T> value, Function1<T, R> mapper) {
    return Option_.narrowK(value).map(mapper);
  }
}

interface OptionPure extends Applicative<Option_> {

  @Override
  default <T> Higher1<Option_, T> pure(T value) {
    return Option.some(value);
  }
}

@Instance
interface OptionApplicative extends OptionPure {

  @Override
  default <T, R> Higher1<Option_, R> ap(Higher1<Option_, T> value, Higher1<Option_, Function1<T, R>> apply) {
    return Option_.narrowK(value).flatMap(t -> Option_.narrowK(apply).map(f -> f.apply(t)));
  }
}

@Instance
interface OptionMonad extends OptionPure, Monad<Option_> {

  @Override
  default <T, R> Higher1<Option_, R> flatMap(Higher1<Option_, T> value,
      Function1<T, ? extends Higher1<Option_, R>> map) {
    return Option_.narrowK(value).flatMap(map.andThen(Option_::narrowK));
  }
}

@Instance
interface OptionSemigroupK extends SemigroupK<Option_> {

  @Override
  default <T> Higher1<Option_, T> combineK(Higher1<Option_, T> t1, Higher1<Option_, T> t2) {
    return Option_.narrowK(t1).fold(cons(Option_.narrowK(t2)), Option::some);
  }
}

@Instance
interface OptionMonoidK extends OptionSemigroupK, MonoidK<Option_> {

  @Override
  default <T> Higher1<Option_, T> zero() {
    return Option.<T>none();
  }
}

@Instance
interface OptionAlternative extends OptionMonoidK, OptionApplicative, Alternative<Option_> { }

@Instance
interface OptionMonadError extends OptionMonad, MonadError<Option_, Unit> {

  @Override
  default <A> Higher1<Option_, A> raiseError(Unit error) {
    return Option.<A>none();
  }

  @Override
  default <A> Higher1<Option_, A> handleErrorWith(Higher1<Option_, A> value,
      Function1<Unit, ? extends Higher1<Option_, A>> handler) {
    return Option_.narrowK(value).fold(() -> Option_.narrowK(handler.apply(unit())), this::pure);
  }
}

@Instance
interface OptionFoldable extends Foldable<Option_> {

  @Override
  default <A, B> B foldLeft(Higher1<Option_, A> value, B initial, Function2<B, A, B> mapper) {
    return Option_.narrowK(value).fold(cons(initial), a -> mapper.apply(initial, a));
  }

  @Override
  default <A, B> Eval<B> foldRight(Higher1<Option_, A> value, Eval<B> initial,
      Function2<A, Eval<B>, Eval<B>> mapper) {
    return Option_.narrowK(value).fold(cons(initial), a -> mapper.apply(a, initial));
  }
}

@Instance
interface OptionTraverse extends Traverse<Option_>, OptionFoldable {

  @Override
  default <G extends Kind, T, R> Higher1<G, Higher1<Option_, R>> traverse(
      Applicative<G> applicative, Higher1<Option_, T> value,
      Function1<T, ? extends Higher1<G, R>> mapper) {
    return Option_.narrowK(value).fold(
        () -> applicative.pure(Option.<R>none()),
        t -> applicative.map(mapper.apply(t), x -> Option.some(x)));
  }
}

@Instance
interface OptionSemigroupal extends Semigroupal<Option_> {

  @Override
  default <A, B> Higher1<Option_, Tuple2<A, B>> product(Higher1<Option_, A> fa, Higher1<Option_, B> fb) {
    return Option_.narrowK(fa).flatMap(a -> Option_.narrowK(fb).map(b -> Tuple.of(a, b)));
  }
}
