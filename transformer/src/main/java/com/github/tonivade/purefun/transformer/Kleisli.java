/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.typeclasses.Monad;

@HigherKind
public interface Kleisli<F extends Witness, Z, A> extends KleisliOf<F, Z, A> {

  Monad<F> monad();
  Kind<F, A> run(Z value);

  default <R> Kleisli<F, Z, R> map(Function1<A, R> map) {
    return Kleisli.of(monad(), value -> monad().map(run(value), map));
  }

  default <R> Kleisli<F, Z, R> flatMap(Function1<A, Kleisli<F, Z, R>> map) {
    return Kleisli.of(monad(), value -> monad().flatMap(run(value), a -> map.apply(a).run(value)));
  }

  default <B> Kleisli<F, Z, B> compose(Kleisli<F, A, B> other) {
    return Kleisli.of(monad(), value -> monad().flatMap(run(value), other::run));
  }

  default <X> Kleisli<F, X, A> local(Function1<X, Z> map) {
    return Kleisli.of(monad(), map.andThen(this::run)::apply);
  }

  static <F extends Witness, A, B> Kleisli<F, A, B> lift(Monad<F> monad, Function1<A, B> map) {
    return Kleisli.of(monad, map.andThen(monad::<B>pure)::apply);
  }

  static <F extends Witness, Z> Kleisli<F, Z, Z> env(Monad<F> monad) {
    return Kleisli.of(monad, monad::<Z>pure);
  }

  static <F extends Witness, A, B> Kleisli<F, A, B> pure(Monad<F> monad, B value) {
    return Kleisli.of(monad, a -> monad.pure(value));
  }

  static <F extends Witness, A, B> Kleisli<F, A, B> of(Monad<F> monad, Function1<A, Kind<F, B>> run) {
    checkNonNull(monad);
    checkNonNull(run);
    return new Kleisli<F, A, B>() {

      @Override
      public Monad<F> monad() { return monad; }

      @Override
      public Kind<F, B> run(A value) { return run.apply(value); }
    };
  }
}