/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.effect.EIO_;
import com.github.tonivade.purefun.effect.RIO_;
import com.github.tonivade.purefun.effect.Task_;
import com.github.tonivade.purefun.effect.UIO_;
import com.github.tonivade.purefun.effect.URIO_;
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

  static <R> Stream.StreamOf<Kind<Kind<ZIO_, R>, Throwable>> ofZIO() {
    return Stream.of(ZIOInstances.monadDefer());
  }

  static Stream.StreamOf<UIO_> ofUIO() {
    return Stream.of(UIOInstances.monadDefer());
  }

  static Stream.StreamOf<Kind<EIO_, Throwable>> ofEIO() {
    return Stream.of(EIOInstances.monadDefer());
  }

  static Stream.StreamOf<Task_> ofTask() {
    return Stream.of(TaskInstances.monadDefer());
  }

  static <R> Stream.StreamOf<Kind<URIO_, R>> ofURIO() {
    return Stream.of(URIOInstances.monadDefer());
  }

  static <R> Stream.StreamOf<Kind<RIO_, R>> ofRIO() {
    return Stream.of(RIOInstances.monadDefer());
  }

  @SuppressWarnings("unchecked")
  static <F extends Witness> Functor<Kind<Stream_, F>> functor() {
    return StreamFunctor.INSTANCE;
  }

  static <F extends Witness> Monad<Kind<Stream_, F>> monad(Stream.StreamOf<F> streamOf) {
    return StreamMonad.instance(checkNonNull(streamOf));
  }
}

interface StreamFunctor<F extends Witness> extends Functor<Kind<Stream_, F>> {

  @SuppressWarnings("rawtypes")
  StreamFunctor INSTANCE = new StreamFunctor() {};

  @Override
  default <T, R> Stream<F, R> map(Kind<Kind<Stream_, F>, T> value, Function1<T, R> mapper) {
    return StreamOf.narrowK(value).map(mapper);
  }
}

interface StreamPure<F extends Witness> extends Applicative<Kind<Stream_, F>> {

  Stream.StreamOf<F> streamOf();

  @Override
  default <T> Stream<F, T> pure(T value) {
    return streamOf().pure(value);
  }
}

interface StreamMonad<F extends Witness> extends Monad<Kind<Stream_, F>>, StreamPure<F> {

  static <F extends Witness> StreamMonad<F> instance(Stream.StreamOf<F> streamOf) {
    return () -> streamOf;
  }

  @Override
  default <T, R> Stream<F, R> flatMap(Kind<Kind<Stream_, F>, T> value,
      Function1<T, ? extends Kind<Kind<Stream_, F>, R>> mapper) {
    return StreamOf.narrowK(value).flatMap(mapper.andThen(StreamOf::narrowK));
  }
}
