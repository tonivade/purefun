/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.typeclasses.Monad;

@HigherKind
public non-sealed interface Kleisli<F extends Witness, Z, A> extends KleisliOf<F, Z, A>, Bindable<Kind<Kind<Kleisli_, F>, Z>, A> {

  Monad<F> monad();
  Kind<F, A> run(Z value);

  @Override
  default <R> Kleisli<F, Z, R> map(Function1<? super A, ? extends R> map) {
    return Kleisli.of(monad(), value -> monad().map(run(value), map));
  }

  @Override
  default <R> Kleisli<F, Z, R> flatMap(Function1<? super A, ? extends Kind<Kind<Kind<Kleisli_, F>, Z>, ? extends R>> map) {
    return Kleisli.of(monad(), value -> monad().flatMap(run(value), a -> map.andThen(KleisliOf::<F, Z, R>narrowK).apply(a).run(value)));
  }

  default <B> Kleisli<F, Z, B> compose(Kleisli<F, A, B> other) {
    return Kleisli.of(monad(), value -> monad().flatMap(run(value), other::run));
  }

  default <X> Kleisli<F, X, A> local(Function1<? super X, ? extends Z> map) {
    return Kleisli.of(monad(), map.andThen(this::run)::apply);
  }

  static <F extends Witness, A, B> Kleisli<F, A, B> lift(Monad<F> monad, Function1<? super A, ? extends B> map) {
    return Kleisli.of(monad, map.andThen(monad::<B>pure)::apply);
  }

  static <F extends Witness, Z> Kleisli<F, Z, Z> env(Monad<F> monad) {
    return Kleisli.of(monad, monad::<Z>pure);
  }

  static <F extends Witness, A, B> Kleisli<F, A, B> pure(Monad<F> monad, B value) {
    return Kleisli.of(monad, a -> monad.pure(value));
  }

  static <F extends Witness, A, B> Kleisli<F, A, B> of(Monad<F> monad, 
      Function1<? super A, ? extends Kind<F, ? extends B>> run) {
    checkNonNull(monad);
    checkNonNull(run);
    return new Kleisli<>() {

      @Override
      public Monad<F> monad() { return monad; }

      @Override
      public Kind<F, B> run(A value) { return run.andThen(Kind::<F, B>narrowK).apply(value); }
    };
  }
}