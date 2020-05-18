/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Conested.conest;
import static com.github.tonivade.purefun.Conested.counnest;
import com.github.tonivade.purefun.Conested;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function1_;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Contravariant;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Profunctor;

@SuppressWarnings("unchecked")
public interface Function1Instances {

  static <T> Functor<Higher1<Function1_, T>> functor() {
    return Function1Functor.INSTANCE;
  }

  static <T> Applicative<Higher1<Function1_, T>> applicative() {
    return Function1Applicative.INSTANCE;
  }

  static <T> Monad<Higher1<Function1_, T>> monad() {
    return Function1Monad.INSTANCE;
  }

  static <T> Contravariant<Conested<Function1_, T>> contravariant() {
    return Function1Contravariant.INSTANCE;
  }

  static Profunctor<Function1_> profunctor() {
    return Function1Profunctor.INSTANCE;
  }
}

interface Function1Functor<T> extends Functor<Higher1<Function1_, T>> {

  @SuppressWarnings("rawtypes")
  Function1Functor INSTANCE = new Function1Functor() {};

  @Override
  default <A, R> Higher2<Function1_, T, R> map(Higher1<Higher1<Function1_, T>, A> value, Function1<A, R> map) {
    Function1<T, A> function = Higher2.narrowK(value).fix2(Function1_::narrowK);
    return function.andThen(map);
  }
}

interface Function1Pure<T> extends Applicative<Higher1<Function1_, T>> {

  @Override
  default <A> Higher2<Function1_, T, A> pure(A value) {
    return Function1.<T, A>cons(value);
  }
}

interface Function1Applicative<T> extends Function1Pure<T> {

  @SuppressWarnings("rawtypes")
  Function1Applicative INSTANCE = new Function1Applicative() {};

  @Override
  default <A, R> Higher2<Function1_, T, R> ap(Higher1<Higher1<Function1_, T>, A> value, Higher1<Higher1<Function1_, T>, Function1<A, R>> apply) {
    Function1<T, A> function = Higher2.narrowK(value).fix2(Function1_::narrowK);
    Function1<T, Function1<A, R>> map = Higher2.narrowK(apply).fix2(Function1_::narrowK);
    return function.flatMap(a -> map.andThen(f -> f.apply(a)));
  }
}

interface Function1Monad<T> extends Function1Pure<T>, Monad<Higher1<Function1_, T>> {

  @SuppressWarnings("rawtypes")
  Function1Monad INSTANCE = new Function1Monad() {};

  @Override
  default <A, R> Higher2<Function1_, T, R> flatMap(Higher1<Higher1<Function1_, T>, A> value, Function1<A, ? extends Higher1<Higher1<Function1_, T>, R>> map) {
    Function1<T, A> function = Higher2.narrowK(value).fix2(Function1_::narrowK);
    return function.flatMap(map.andThen(Function1_::narrowK));
  }
}

interface Function1Contravariant<R> extends Contravariant<Conested<Function1_, R>> {

  @SuppressWarnings("rawtypes")
  Function1Contravariant INSTANCE = new Function1Contravariant() {};

  @Override
  default <A, B> Higher1<Conested<Function1_, R>, B> contramap(Higher1<Conested<Function1_, R>, A> value, Function1<B, A> map) {
    Function1<A, R> function = counnest(value).fix1(Function1_::narrowK);
    return conest(function.compose(map));
  }
}

interface Function1Profunctor extends Profunctor<Function1_> {

  Function1Profunctor INSTANCE = new Function1Profunctor() {};

  @Override
  default <A, B, C, D> Higher2<Function1_, C, D> dimap(Higher2<Function1_, A, B> value, Function1<C, A> contramap, Function1<B, D> map) {
    Function1<A, B> function = value.fix2(Function1_::narrowK);
    return function.compose(contramap).andThen(map);
  }
}
