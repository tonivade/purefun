/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.typeclasses.Conested.conest;
import static com.github.tonivade.purefun.typeclasses.Conested.counnest;

import com.github.tonivade.purefun.core.Function1Of;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Conested;
import com.github.tonivade.purefun.typeclasses.Contravariant;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Profunctor;

@SuppressWarnings("unchecked")
public interface Function1Instances {

  static <T> Functor<Kind<Function1<?, ?>, T>> functor() {
    return Function1Functor.INSTANCE;
  }

  static <T> Applicative<Kind<Function1<?, ?>, T>> applicative() {
    return Function1Applicative.INSTANCE;
  }

  static <T> Monad<Kind<Function1<?, ?>, T>> monad() {
    return Function1Monad.INSTANCE;
  }

  static <T> Contravariant<Conested<Function1<?, ?>, T>> contravariant() {
    return Function1Contravariant.INSTANCE;
  }

  static Profunctor<Function1<?, ?>> profunctor() {
    return Function1Profunctor.INSTANCE;
  }
}

interface Function1Functor<T> extends Functor<Kind<Function1<?, ?>, T>> {

  @SuppressWarnings("rawtypes")
  Function1Functor INSTANCE = new Function1Functor() {};

  @Override
  default <A, R> Function1<T, R> map(Kind<Kind<Function1<?, ?>, T>, ? extends A> value,
      Function1<? super A, ? extends R> map) {
    Function1<T, A> function = value.fix(Function1Of::toFunction1);
    return function.andThen(map);
  }
}

interface Function1Pure<T> extends Applicative<Kind<Function1<?, ?>, T>> {

  @Override
  default <A> Function1<T, A> pure(A value) {
    return Function1.cons(value);
  }
}

interface Function1Applicative<T> extends Function1Pure<T> {

  @SuppressWarnings("rawtypes")
  Function1Applicative INSTANCE = new Function1Applicative() {};

  @Override
  default <A, R> Function1<T, R> ap(Kind<Kind<Function1<?, ?>, T>, ? extends A> value,
      Kind<Kind<Function1<?, ?>, T>, ? extends Function1<? super A, ? extends R>> apply) {
    return value.fix(Function1Of::toFunction1)
        .flatMap(a -> apply.fix(Function1Of::toFunction1).andThen(f -> f.apply(a)));
  }
}

interface Function1Monad<T> extends Function1Pure<T>, Monad<Kind<Function1<?, ?>, T>> {

  @SuppressWarnings("rawtypes")
  Function1Monad INSTANCE = new Function1Monad() {};

  @Override
  default <A, R> Function1<T, R> flatMap(Kind<Kind<Function1<?, ?>, T>, ? extends A> value,
      Function1<? super A, ? extends Kind<Kind<Function1<?, ?>, T>, ? extends R>> map) {
    Function1<T, A> function = value.fix(Function1Of::toFunction1);
    return function.flatMap(map.andThen(Function1Of::toFunction1));
  }
}

interface Function1Contravariant<R> extends Contravariant<Conested<Function1<?, ?>, R>> {

  @SuppressWarnings("rawtypes")
  Function1Contravariant INSTANCE = new Function1Contravariant() {};

  @Override
  default <A, B> Kind<Conested<Function1<?, ?>, R>, B> contramap(Kind<Conested<Function1<?, ?>, R>, ? extends A> value, Function1<? super B, ? extends A> map) {
    Kind<Kind<Function1<?, ?>, A>, R> counnest = counnest(value);
    Function1<A, R> function = counnest.fix(Function1Of::toFunction1);
    return conest(function.compose(map));
  }
}

interface Function1Profunctor extends Profunctor<Function1<?, ?>> {

  Function1Profunctor INSTANCE = new Function1Profunctor() {};

  @Override
  default <A, B, C, D> Function1<C, D> dimap(Kind<Kind<Function1<?, ?>, A>, ? extends B> value, Function1<? super C, ? extends A> contramap, Function1<? super B, ? extends D> map) {
    return value.fix(Function1Of::<A, B>toFunction1).dimap(contramap, map);
  }
}
