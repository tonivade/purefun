/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Nothing.nothing;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.monad.OptionT;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Eq;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;

public interface OptionTInstances {

  static <F extends Kind, T> Eq<Higher2<OptionT.µ, F, T>> eq(Eq<Higher1<F, Option<T>>> eq) {
    return (a, b) -> eq.eqv(OptionT.narrowK(a).value(), OptionT.narrowK(b).value());
  }

  static <F extends Kind> Monad<Higher1<OptionT.µ, F>> monad(Monad<F> monadF) {
    requireNonNull(monadF);
    return new OptionTMonad<F>() {

      @Override
      public Monad<F> monadF() { return monadF; }
    };
  }

  static <F extends Kind> MonadError<Higher1<OptionT.µ, F>, Nothing> monadError(Monad<F> monadF) {
    requireNonNull(monadF);
    return new OptionTMonadErrorFromMonad<F>() {

      @Override
      public Monad<F> monadF() { return monadF; }
    };
  }

  static <F extends Kind, E> MonadError<Higher1<OptionT.µ, F>, E> monadError(MonadError<F, E> monadErrorF) {
    requireNonNull(monadErrorF);
    return new OptionTMonadErrorFromMonadError<F, E>() {

      @Override
      public MonadError<F, E> monadF() { return monadErrorF; }
    };
  }

  static <F extends Kind> Defer<Higher1<OptionT.µ, F>> defer(Monad<F> monadF, Defer<F> deferF) {
    requireNonNull(monadF);
    requireNonNull(deferF);
    return new OptionTDefer<F>() {

      @Override
      public Monad<F> monadF() { return monadF; }

      @Override
      public Defer<F> deferF() { return deferF; }
    };
  }

}

interface OptionTMonad<F extends Kind> extends Monad<Higher1<OptionT.µ, F>> {

  Monad<F> monadF();

  @Override
  default <T> OptionT<F, T> pure(T value) {
    return OptionT.some(monadF(), value);
  }

  @Override
  default <T, R> OptionT<F, R> flatMap(Higher1<Higher1<OptionT.µ, F>, T> value,
      Function1<T, ? extends Higher1<Higher1<OptionT.µ, F>, R>> map) {
    return OptionT.narrowK(value).flatMap(map.andThen(OptionT::narrowK));
  }
}

interface OptionTMonadErrorFromMonad<F extends Kind> 
    extends MonadError<Higher1<OptionT.µ, F>, Nothing>, OptionTMonad<F> {

  @Override
  default <A> OptionT<F, A> raiseError(Nothing error) {
    return OptionT.none(monadF());
  }

  @Override
  default <A> OptionT<F, A> handleErrorWith(Higher1<Higher1<OptionT.µ, F>, A> value,
      Function1<Nothing, ? extends Higher1<Higher1<OptionT.µ, F>, A>> handler) {
    return OptionT.of(monadF(),
        monadF().flatMap(OptionT.narrowK(value).value(),
            option -> option.fold(() -> handler.andThen(OptionT::narrowK).apply(nothing()).value(),
                a -> monadF().pure(Option.some(a)))));
  }
}

interface OptionTMonadErrorFromMonadError<F extends Kind, E> 
    extends MonadError<Higher1<OptionT.µ, F>, E>, OptionTMonad<F> {

  @Override
  MonadError<F, E> monadF();

  @Override
  default <A> OptionT<F, A> raiseError(E error) {
    return OptionT.of(monadF(), monadF().raiseError(error));
  }

  @Override
  default <A> OptionT<F, A> handleErrorWith(Higher1<Higher1<OptionT.µ, F>, A> value,
      Function1<E, ? extends Higher1<Higher1<OptionT.µ, F>, A>> handler) {
    return OptionT.of(monadF(), monadF().handleErrorWith(OptionT.narrowK(value).value(),
        error -> handler.andThen(OptionT::narrowK).apply(error).value()));
  }
}

interface OptionTDefer<F extends Kind> extends Defer<Higher1<OptionT.µ, F>> {

  Monad<F> monadF();
  Defer<F> deferF();

  @Override
  default <A> OptionT<F, A> defer(Producer<Higher1<Higher1<OptionT.µ, F>, A>> defer) {
    return OptionT.of(monadF(), deferF().defer(() -> defer.andThen(OptionT::narrowK).get().value()));
  }
}
