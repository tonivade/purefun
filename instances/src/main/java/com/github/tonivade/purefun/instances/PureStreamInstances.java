/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;
import com.github.tonivade.purefun.effect.EIO_;
import com.github.tonivade.purefun.effect.RIO_;
import com.github.tonivade.purefun.effect.Task_;
import com.github.tonivade.purefun.effect.UIO_;
import com.github.tonivade.purefun.effect.URIO_;
import com.github.tonivade.purefun.effect.PureIO_;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.stream.PureStream;
import com.github.tonivade.purefun.stream.PureStreamOf;
import com.github.tonivade.purefun.stream.PureStream_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface PureStreamInstances {

  static PureStream.StreamOf<IO_> ofIO() {
    return PureStream.of(IOInstances.monadDefer());
  }

  static <R> PureStream.StreamOf<Kind<Kind<PureIO_, R>, Throwable>> ofPureIO() {
    return PureStream.of(PureIOInstances.monadDefer());
  }

  static PureStream.StreamOf<UIO_> ofUIO() {
    return PureStream.of(UIOInstances.monadDefer());
  }

  static PureStream.StreamOf<Kind<EIO_, Throwable>> ofEIO() {
    return PureStream.of(EIOInstances.monadDefer());
  }

  static PureStream.StreamOf<Task_> ofTask() {
    return PureStream.of(TaskInstances.monadDefer());
  }

  static <R> PureStream.StreamOf<Kind<URIO_, R>> ofURIO() {
    return PureStream.of(URIOInstances.monadDefer());
  }

  static <R> PureStream.StreamOf<Kind<RIO_, R>> ofRIO() {
    return PureStream.of(RIOInstances.monadDefer());
  }

  @SuppressWarnings("unchecked")
  static <F extends Witness> Functor<Kind<PureStream_, F>> functor() {
    return PureStreamFunctor.INSTANCE;
  }

  static <F extends Witness> Monad<Kind<PureStream_, F>> monad(PureStream.StreamOf<F> streamOf) {
    return PureStreamMonad.instance(checkNonNull(streamOf));
  }
}

interface PureStreamFunctor<F extends Witness> extends Functor<Kind<PureStream_, F>> {

  @SuppressWarnings("rawtypes")
  PureStreamFunctor INSTANCE = new PureStreamFunctor() {};

  @Override
  default <T, R> PureStream<F, R> map(Kind<Kind<PureStream_, F>, ? extends T> value, Function1<? super T, ? extends R> mapper) {
    return PureStreamOf.narrowK(value).map(mapper);
  }
}

interface PureStreamPure<F extends Witness> extends Applicative<Kind<PureStream_, F>> {

  PureStream.StreamOf<F> streamOf();

  @Override
  default <T> PureStream<F, T> pure(T value) {
    return streamOf().pure(value);
  }
}

interface PureStreamMonad<F extends Witness> extends Monad<Kind<PureStream_, F>>, PureStreamPure<F> {

  static <F extends Witness> PureStreamMonad<F> instance(PureStream.StreamOf<F> streamOf) {
    return () -> streamOf;
  }

  @Override
  default <T, R> PureStream<F, R> flatMap(Kind<Kind<PureStream_, F>, ? extends T> value,
      Function1<? super T, ? extends Kind<Kind<PureStream_, F>, ? extends R>> mapper) {
    return PureStreamOf.narrowK(value).flatMap(mapper.andThen(PureStreamOf::narrowK));
  }
}
