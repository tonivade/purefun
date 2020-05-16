/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.free.Trampoline;
import com.github.tonivade.purefun.free.Trampoline_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface TrampolineInstances {

  static Functor<Trampoline_> functor() {
    return TrampolineFunctor.instance();
  }

  static Applicative<Trampoline_> applicative() {
    return TrampolineApplicative.instance();
  }

  static Monad<Trampoline_> monad() {
    return TrampolineMonad.instance();
  }

  static Defer<Trampoline_> defer() {
    return TrampolineDefer.instance();
  }
}

@Instance
interface TrampolineFunctor extends Functor<Trampoline_> {

  @Override
  default <T, R> Higher1<Trampoline_, R> map(Higher1<Trampoline_, T> value, Function1<T, R> mapper) {
    return Trampoline_.narrowK(value).map(mapper);
  }
}

interface TrampolinePure extends Applicative<Trampoline_> {

  @Override
  default <T> Higher1<Trampoline_, T> pure(T value) {
    return Trampoline.done(value);
  }
}

@Instance
interface TrampolineApplicative extends TrampolinePure {

  @Override
  default <T, R> Higher1<Trampoline_, R> ap(Higher1<Trampoline_, T> value, Higher1<Trampoline_, Function1<T, R>> apply) {
    return Trampoline_.narrowK(value).flatMap(t -> Trampoline_.narrowK(apply).map(f -> f.apply(t)));
  }
}

@Instance
interface TrampolineMonad extends TrampolinePure, Monad<Trampoline_> {

  @Override
  default <T, R> Higher1<Trampoline_, R> flatMap(Higher1<Trampoline_, T> value,
      Function1<T, ? extends Higher1<Trampoline_, R>> map) {
    return Trampoline_.narrowK(value).flatMap(map.andThen(Trampoline_::narrowK));
  }
}

@Instance
interface TrampolineDefer extends Defer<Trampoline_> {

  @Override
  default <A> Higher1<Trampoline_, A> defer(Producer<Higher1<Trampoline_, A>> defer) {
    return Trampoline.more(() -> defer.get().fix1(Trampoline_::narrowK));
  }
}
