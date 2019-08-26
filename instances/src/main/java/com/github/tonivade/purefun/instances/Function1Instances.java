/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Conested;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Contravariant;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Profunctor;

public interface Function1Instances {

  static <T> Functor<Higher1<Function1.µ, T>> functor() {
    return new Function1Functor<T>() {};
  }

  static <T> Applicative<Higher1<Function1.µ, T>> applicative() {
    return new Function1Applicative<T>() {};
  }

  static <T> Monad<Higher1<Function1.µ, T>> monad() {
    return new Function1Monad<T>() {};
  }

  static <T> Contravariant<Conested<Function1.µ, T>> contravariant() {
    return new Function1Contravariant<T>() {};
  }

  static Profunctor<Function1.µ> profunctor() {
    return new Function1Profunctor() {};
  }
}

@Instance
interface Function1Functor<T> extends Functor<Higher1<Function1.µ, T>> {
  @Override
  default <A, R> Function1<T, R> map(Higher1<Higher1<Function1.µ, T>, A> value, Function1<A, R> map) {
    Function1<T, A> function = Higher2.narrowK(value).fix2(Function1::narrowK);
    return function.andThen(map);
  }
}

interface Function1Pure<T> extends Applicative<Higher1<Function1.µ, T>> {
  @Override
  default <A> Function1<T, A> pure(A value) {
    return Function1.cons(value);
  }
}

@Instance
interface Function1Applicative<T> extends Function1Pure<T> {
  @Override
  default <A, R> Function1<T, R> ap(Higher1<Higher1<Function1.µ, T>, A> value, Higher1<Higher1<Function1.µ, T>, Function1<A, R>> apply) {
    Function1<T, A> function = Higher2.narrowK(value).fix2(Function1::narrowK);
    Function1<T, Function1<A, R>> map = Higher2.narrowK(apply).fix2(Function1::narrowK);
    return function.flatMap(a -> map.andThen(f -> f.apply(a)));
  }
}

@Instance
interface Function1Monad<T> extends Function1Pure<T>, Monad<Higher1<Function1.µ, T>> {
  @Override
  default <A, R> Function1<T, R> flatMap(Higher1<Higher1<Function1.µ, T>, A> value, Function1<A, ? extends Higher1<Higher1<Function1.µ, T>, R>> map) {
    Function1<T, A> function = Higher2.narrowK(value).fix2(Function1::narrowK);
    return function.flatMap(map.andThen(Function1::narrowK));
  }
}

@Instance
interface Function1Contravariant<R> extends Contravariant<Conested<Function1.µ, R>> {
  @Override
  default <A, B> Higher1<Conested<Function1.µ, R>, B> contramap(Higher1<Conested<Function1.µ, R>, A> value, Function1<B, A> map) {
    Function1<A, R> function = Conested.<Function1.µ, A, R>counnest(value).fix1(Function1::narrowK);
    return Conested.<Function1.µ, B, R>conest(function.compose(map));
  }
}

@Instance
interface Function1Profunctor extends Profunctor<Function1.µ> {
  @Override
  default <A, B, C, D> Function1<C, D> dimap(Higher2<Function1.µ, A, B> value, Function1<C, A> contramap, Function1<B, D> map) {
    Function1<A, B> function = value.fix2(Function1::narrowK);
    return function.compose(contramap).andThen(map);
  }
}
