/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Unit.unit;
import static java.util.Objects.requireNonNull;

import java.util.NoSuchElementException;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.transformer.OptionT;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;

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

  static <F extends Kind> MonadError<Higher1<OptionT.µ, F>, Unit> monadError(Monad<F> monadF) {
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

  static <F extends Kind> MonadThrow<Higher1<OptionT.µ, F>> monadThrow(MonadThrow<F> monadThrowF) {
    requireNonNull(monadThrowF);
    return new OptionTMonadThrow<F>() {

      @Override
      public MonadError<F, Throwable> monadF() { return monadThrowF; }
    };
  }

  static <F extends Kind> Defer<Higher1<OptionT.µ, F>> defer(MonadDefer<F> monadDeferF) {
    requireNonNull(monadDeferF);
    return new OptionTDefer<F>() {

      @Override
      public Monad<F> monadF() { return monadDeferF; }

      @Override
      public Defer<F> deferF() { return monadDeferF; }
    };
  }

  static <F extends Kind> MonadDefer<Higher1<OptionT.µ, F>> monadDefer(MonadDefer<F> monadDeferF) {
    return new OptionTMonadDefer<F>() {

      @Override
      public Defer<F> deferF() { return monadDeferF; }

      @Override
      public MonadThrow<F> monadF() { return monadDeferF; }

      @Override
      public Bracket<F> bracketF() { return monadDeferF; }
    };
  }

  static <F extends Kind, A> Reference<Higher1<OptionT.µ, F>, A> ref(MonadDefer<F> monadF, A value) {
    return Reference.of(monadDefer(monadF), value);
  }
}

@Instance
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

@Instance
interface OptionTMonadErrorFromMonad<F extends Kind>
    extends MonadError<Higher1<OptionT.µ, F>, Unit>, OptionTMonad<F> {

  @Override
  default <A> OptionT<F, A> raiseError(Unit error) {
    return OptionT.none(monadF());
  }

  @Override
  default <A> OptionT<F, A> handleErrorWith(Higher1<Higher1<OptionT.µ, F>, A> value,
      Function1<Unit, ? extends Higher1<Higher1<OptionT.µ, F>, A>> handler) {
    return OptionT.of(monadF(),
        monadF().flatMap(OptionT.narrowK(value).value(),
            option -> option.fold(() -> handler.andThen(OptionT::narrowK).apply(unit()).value(),
                a -> monadF().pure(Option.some(a)))));
  }
}

@Instance
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

@Instance
interface OptionTMonadThrow<F extends Kind>
    extends MonadThrow<Higher1<OptionT.µ, F>>,
            OptionTMonadErrorFromMonadError<F, Throwable> { }

@Instance
interface OptionTDefer<F extends Kind> extends Defer<Higher1<OptionT.µ, F>> {

  Monad<F> monadF();
  Defer<F> deferF();

  @Override
  default <A> OptionT<F, A> defer(Producer<Higher1<Higher1<OptionT.µ, F>, A>> defer) {
    return OptionT.of(monadF(), deferF().defer(() -> defer.andThen(OptionT::narrowK).get().value()));
  }
}

@Instance
interface OptionTBracket<F extends Kind> extends Bracket<Higher1<OptionT.µ, F>> {

  MonadThrow<F> monadF();
  Defer<F> deferF();
  Bracket<F> bracketF();

  @Override
  default <A, B> OptionT<F, B> bracket(Higher1<Higher1<OptionT.µ, F>, A> acquire,
                                       Function1<A, ? extends Higher1<Higher1<OptionT.µ, F>, B>> use,
                                       Consumer1<A> release) {
    Higher1<F, Option<B>> bracket =
        bracketF().bracket(
            acquire.fix1(OptionT::narrowK).value(),
            option -> option.fold(
                () -> monadF().raiseError(new NoSuchElementException("could not acquire resource")),
                value -> use.andThen(OptionT::narrowK).apply(value).value()),
            option -> option.fold(() -> unit(), release.asFunction()));
    return OptionT.of(monadF(), bracket);
  }
}

@Instance
interface OptionTMonadDefer<F extends Kind>
    extends OptionTMonadThrow<F>,
            OptionTDefer<F>,
            OptionTBracket<F>,
            MonadDefer<Higher1<OptionT.µ, F>> { }
