/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function1Of;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

@SuppressWarnings("unchecked")
public interface Function1Instances {

  static <T> Functor<Function1<T, ?>> functor() {
    return Function1Functor.INSTANCE;
  }

  static <T> Applicative<Function1<T, ?>> applicative() {
    return Function1Applicative.INSTANCE;
  }

  static <T> Monad<Function1<T, ?>> monad() {
    return Function1Monad.INSTANCE;
  }
}

interface Function1Functor<T> extends Functor<Function1<T, ?>> {

  @SuppressWarnings("rawtypes")
  Function1Functor INSTANCE = new Function1Functor() {};

  @Override
  default <A, R> Function1<T, R> map(Kind<Function1<T, ?>, ? extends A> value,
      Function1<? super A, ? extends R> map) {
    Function1<T, A> function = value.fix(Function1Of::toFunction1);
    return function.andThen(map);
  }
}

interface Function1Pure<T> extends Applicative<Function1<T, ?>> {

  @Override
  default <A> Function1<T, A> pure(A value) {
    return Function1.cons(value);
  }
}

interface Function1Applicative<T> extends Function1Pure<T> {

  @SuppressWarnings("rawtypes")
  Function1Applicative INSTANCE = new Function1Applicative() {};

  @Override
  default <A, R> Function1<T, R> ap(Kind<Function1<T, ?>, ? extends A> value,
      Kind<Function1<T, ?>, ? extends Function1<? super A, ? extends R>> apply) {
    return value.fix(Function1Of::toFunction1)
        .flatMap(a -> apply.fix(Function1Of::toFunction1).andThen(f -> f.apply(a)));
  }
}

interface Function1Monad<T> extends Function1Pure<T>, Monad<Function1<T, ?>> {

  @SuppressWarnings("rawtypes")
  Function1Monad INSTANCE = new Function1Monad() {};

  @Override
  default <A, R> Function1<T, R> flatMap(Kind<Function1<T, ?>, ? extends A> value,
      Function1<? super A, ? extends Kind<Function1<T, ?>, ? extends R>> map) {
    Function1<T, A> function = value.fix(Function1Of::toFunction1);
    return function.flatMap(map.andThen(Function1Of::toFunction1));
  }
}
