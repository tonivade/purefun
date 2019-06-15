/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.stream.Stream;
import com.github.tonivade.purefun.stream.Stream.StreamOf;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface StreamInstances {

  static StreamOf<IO.µ> ofIO() {
    return Stream.of(IOInstances.monadDefer());
  }
  
  static <F extends Kind> Functor<Higher1<Stream.µ, F>> functor() {
    return new StreamFunctor<F>() {};
  }
  
  static <F extends Kind> Applicative<Higher1<Stream.µ, F>> applicative(StreamOf<F> streamOf) {
    requireNonNull(streamOf);
    return new StreamApplicative<F>() {

      @Override
      public StreamOf<F> streamOf() { return streamOf; }
    };
  }
  
  static <F extends Kind> Monad<Higher1<Stream.µ, F>> monad(StreamOf<F> streamOf) {
    requireNonNull(streamOf);
    return new StreamMonad<F>() {

      @Override
      public StreamOf<F> streamOf() { return streamOf; }
    };
  }
}

interface StreamFunctor<F extends Kind> extends Functor<Higher1<Stream.µ, F>> {
  
  @Override
  default <T, R> Stream<F, R> map(Higher1<Higher1<Stream.µ, F>, T> value, Function1<T, R> mapper) {
    return Stream.narrowK(value).map(mapper);
  }
}

interface StreamPure<F extends Kind> extends Applicative<Higher1<Stream.µ, F>> {
  
  StreamOf<F> streamOf();

  @Override
  default <T> Stream<F, T> pure(T value) {
    return streamOf().pure(value);
  }
}

interface StreamApplicative<F extends Kind> extends StreamPure<F> {
  
  @Override
  default <T, R> Stream<F, R> ap(Higher1<Higher1<Stream.µ, F>, T> value,
      Higher1<Higher1<Stream.µ, F>, Function1<T, R>> apply) {
    return Stream.narrowK(value).flatMap(t -> Stream.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface StreamMonad<F extends Kind> extends Monad<Higher1<Stream.µ, F>>, StreamPure<F> {

  @Override
  default <T, R> Stream<F, R> flatMap(Higher1<Higher1<Stream.µ, F>, T> value,
      Function1<T, ? extends Higher1<Higher1<Stream.µ, F>, R>> mapper) {
    return Stream.narrowK(value).flatMap(mapper.andThen(Stream::narrowK));
  }
}
