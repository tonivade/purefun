/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.effect.EIO_;
import com.github.tonivade.purefun.effect.Task_;
import com.github.tonivade.purefun.effect.UIO_;
import com.github.tonivade.purefun.effect.ZIO_;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.stream.Stream;
import com.github.tonivade.purefun.stream.StreamOf;
import com.github.tonivade.purefun.stream.Stream_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface StreamInstances {

  static Stream.StreamOf<IO_> ofIO() {
    return Stream.of(IOInstances.monadDefer());
  }

  static <R> Stream.StreamOf<Higher1<Higher1<ZIO_, R>, Throwable>> ofZIO() {
    return Stream.of(ZIOInstances.monadDefer());
  }

  static Stream.StreamOf<UIO_> ofUIO() {
    return Stream.of(UIOInstances.monadDefer());
  }

  static Stream.StreamOf<Higher1<EIO_, Throwable>> ofEIO() {
    return Stream.of(EIOInstances.monadDefer());
  }

  static Stream.StreamOf<Task_> ofTask() {
    return Stream.of(TaskInstances.monadDefer());
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind> Functor<Higher1<Stream_, F>> functor() {
    return StreamFunctor.INSTANCE;
  }

  static <F extends Kind> Applicative<Higher1<Stream_, F>> applicative(Stream.StreamOf<F> streamOf) {
    return StreamApplicative.instance(checkNonNull(streamOf));
  }

  static <F extends Kind> Monad<Higher1<Stream_, F>> monad(Stream.StreamOf<F> streamOf) {
    return StreamMonad.instance(checkNonNull(streamOf));
  }
}

interface StreamFunctor<F extends Kind> extends Functor<Higher1<Stream_, F>> {

  @SuppressWarnings("rawtypes")
  StreamFunctor INSTANCE = new StreamFunctor() {};

  @Override
  default <T, R> Higher2<Stream_, F, R> map(Higher1<Higher1<Stream_, F>, T> value, Function1<T, R> mapper) {
    return StreamOf.narrowK(value).map(mapper);
  }
}

interface StreamPure<F extends Kind> extends Applicative<Higher1<Stream_, F>> {

  Stream.StreamOf<F> streamOf();

  @Override
  default <T> Higher2<Stream_, F, T> pure(T value) {
    return streamOf().pure(value);
  }
}

interface StreamApplicative<F extends Kind> extends StreamPure<F> {

  static <F extends Kind> StreamApplicative<F> instance(Stream.StreamOf<F> streamOf) {
    return () -> streamOf;
  }

  @Override
  default <T, R> Higher2<Stream_, F, R> ap(Higher1<Higher1<Stream_, F>, T> value,
      Higher1<Higher1<Stream_, F>, Function1<T, R>> apply) {
    return StreamOf.narrowK(value).flatMap(t -> StreamOf.narrowK(apply).map(f -> f.apply(t)));
  }
}

interface StreamMonad<F extends Kind> extends Monad<Higher1<Stream_, F>>, StreamPure<F> {

  static <F extends Kind> StreamMonad<F> instance(Stream.StreamOf<F> streamOf) {
    return () -> streamOf;
  }

  @Override
  default <T, R> Higher2<Stream_, F, R> flatMap(Higher1<Higher1<Stream_, F>, T> value,
      Function1<T, ? extends Higher1<Higher1<Stream_, F>, R>> mapper) {
    return StreamOf.narrowK(value).flatMap(mapper.andThen(StreamOf::narrowK));
  }
}
