/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Nothing.nothing;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Pattern2;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Option;
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

  static <T> Eq<Higher1<Option.µ, T>> eq(Eq<T> eqSome) {
    return (a, b) -> Pattern2.<Option<T>, Option<T>, Boolean>build()
      .when((x, y) -> x.isPresent() && y.isPresent())
        .then((x, y) -> eqSome.eqv(x.get(), y.get()))
      .when((x, y) -> x.isEmpty() && y.isEmpty())
        .returns(true)
      .otherwise()
        .returns(false)
      .apply(Option.narrowK(a), Option.narrowK(b));
  }

  static Functor<Option.µ> functor() {
    return new OptionFunctor() {};
  }

  static Applicative<Option.µ> applicative() {
    return new OptionApplicative() {};
  }

  static Alternative<Option.µ> alternative() {
    return new OptionAlternative() {};
  }

  static Monad<Option.µ> monad() {
    return new OptionMonad() {};
  }

  static MonadError<Option.µ, Nothing> monadError() {
    return new OptionMonadError() {};
  }

  static Traverse<Option.µ> traverse() {
    return new OptionTraverse() {};
  }

  static Semigroupal<Option.µ> semigroupal() {
    return new OptionSemigroupal() {};
  }

  static Foldable<Option.µ> foldable() {
    return new OptionFoldable() {};
  }
}

@Instance
interface OptionFunctor extends Functor<Option.µ> {

  @Override
  default <T, R> Higher1<Option.µ, R> map(Higher1<Option.µ, T> value, Function1<T, R> mapper) {
    return Option.narrowK(value).map(mapper).kind1();
  }
}

@Instance
interface OptionPure extends Applicative<Option.µ> {

  @Override
  default <T> Higher1<Option.µ, T> pure(T value) {
    return Option.some(value).kind1();
  }
}

@Instance
interface OptionApplicative extends OptionPure {

  @Override
  default <T, R> Higher1<Option.µ, R> ap(Higher1<Option.µ, T> value, Higher1<Option.µ, Function1<T, R>> apply) {
    return Option.narrowK(value).flatMap(t -> Option.narrowK(apply).map(f -> f.apply(t))).kind1();
  }
}

@Instance
interface OptionMonad extends OptionPure, Monad<Option.µ> {

  @Override
  default <T, R> Higher1<Option.µ, R> flatMap(Higher1<Option.µ, T> value,
      Function1<T, ? extends Higher1<Option.µ, R>> map) {
    return Option.narrowK(value).flatMap(map.andThen(Option::<R>narrowK)).kind1();
  }
}

@Instance
interface OptionSemigroupK extends SemigroupK<Option.µ> {

  @Override
  default <T> Higher1<Option.µ, T> combineK(Higher1<Option.µ, T> t1, Higher1<Option.µ, T> t2) {
    return Option.narrowK(t1).fold(cons(Option.narrowK(t2)), Option::<T>some).kind1();
  }
}

@Instance
interface OptionMonoidK extends OptionSemigroupK, MonoidK<Option.µ> {

  @Override
  default <T> Higher1<Option.µ, T> zero() {
    return Option.<T>none().kind1();
  }
}

@Instance
interface OptionAlternative extends OptionMonoidK, OptionApplicative, Alternative<Option.µ> { }

@Instance
interface OptionMonadError extends OptionMonad, MonadError<Option.µ, Nothing> {

  @Override
  default <A> Higher1<Option.µ, A> raiseError(Nothing error) {
    return Option.<A>none().kind1();
  }

  @Override
  default <A> Higher1<Option.µ, A> handleErrorWith(Higher1<Option.µ, A> value,
      Function1<Nothing, ? extends Higher1<Option.µ, A>> handler) {
    return Option.narrowK(value).fold(() -> Option.narrowK(handler.apply(nothing())).kind1(), this::pure);
  }
}

@Instance
interface OptionFoldable extends Foldable<Option.µ> {

  @Override
  default <A, B> B foldLeft(Higher1<Option.µ, A> value, B initial, Function2<B, A, B> mapper) {
    return Option.narrowK(value).fold(cons(initial), a -> mapper.apply(initial, a));
  }

  @Override
  default <A, B> Eval<B> foldRight(Higher1<Option.µ, A> value, Eval<B> initial,
      Function2<A, Eval<B>, Eval<B>> mapper) {
    return Option.narrowK(value).fold(cons(initial), a -> mapper.apply(a, initial));
  }
}

@Instance
interface OptionTraverse extends Traverse<Option.µ>, OptionFoldable {

  @Override
  default <G extends Kind, T, R> Higher1<G, Higher1<Option.µ, R>> traverse(
      Applicative<G> applicative, Higher1<Option.µ, T> value,
      Function1<T, ? extends Higher1<G, R>> mapper) {
    return Option.<T>narrowK(value).fold(
        () -> applicative.pure(Option.<R>none().kind1()),
        t -> applicative.map(mapper.apply(t), x -> Option.some(x).kind1()));
  }
}

@Instance
interface OptionSemigroupal extends Semigroupal<Option.µ> {

  @Override
  default <A, B> Higher1<Option.µ, Tuple2<A, B>> product(Higher1<Option.µ, A> fa, Higher1<Option.µ, B> fb) {
    return Option.narrowK(fa).flatMap(a -> Option.narrowK(fb).map(b -> Tuple.of(a, b))).kind1();
  }
}
