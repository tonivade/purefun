/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.time.Duration;
import java.util.NoSuchElementException;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.transformer.OptionT;
import com.github.tonivade.purefun.transformer.OptionT_;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;

public interface OptionTInstances {

  static <F extends Kind, T> Eq<Higher2<OptionT_, F, T>> eq(Eq<Higher1<F, Option<T>>> eq) {
    return (a, b) -> eq.eqv(OptionT_.narrowK(a).value(), OptionT_.narrowK(b).value());
  }

  static <F extends Kind> Monad<Higher1<OptionT_, F>> monad(Monad<F> monadF) {
    return OptionTMonad.instance(checkNonNull(monadF));
  }

  static <F extends Kind> MonadError<Higher1<OptionT_, F>, Unit> monadError(Monad<F> monadF) {
    return OptionTMonadErrorFromMonad.instance(checkNonNull(monadF));
  }

  static <F extends Kind, E> MonadError<Higher1<OptionT_, F>, E> monadError(MonadError<F, E> monadErrorF) {
    return OptionTMonadErrorFromMonadError.instance(checkNonNull(monadErrorF));
  }

  static <F extends Kind> MonadThrow<Higher1<OptionT_, F>> monadThrow(MonadThrow<F> monadThrowF) {
    return OptionTMonadThrow.instance(checkNonNull(checkNonNull(monadThrowF)));
  }

  static <F extends Kind> Defer<Higher1<OptionT_, F>> defer(MonadDefer<F> monadDeferF) {
    return OptionTDefer.instance(checkNonNull(monadDeferF));
  }

  static <F extends Kind> MonadDefer<Higher1<OptionT_, F>> monadDefer(MonadDefer<F> monadDeferF) {
    return OptionTMonadDefer.instance(checkNonNull(monadDeferF));
  }

  static <F extends Kind, A> Reference<Higher1<OptionT_, F>, A> ref(MonadDefer<F> monadF, A value) {
    return Reference.of(monadDefer(monadF), value);
  }
}

interface OptionTMonad<F extends Kind> extends Monad<Higher1<OptionT_, F>> {

  static <F extends Kind> OptionTMonad<F> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  Monad<F> monadF();

  @Override
  default <T> Higher2<OptionT_, F, T> pure(T value) {
    return OptionT.some(monadF(), value).kind2();
  }

  @Override
  default <T, R> Higher2<OptionT_, F, R> flatMap(Higher1<Higher1<OptionT_, F>, T> value,
      Function1<T, ? extends Higher1<Higher1<OptionT_, F>, R>> map) {
    return OptionT_.narrowK(value).flatMap(map.andThen(OptionT_::narrowK)).kind2();
  }
}

interface OptionTMonadErrorFromMonad<F extends Kind>
    extends MonadError<Higher1<OptionT_, F>, Unit>, OptionTMonad<F> {

  static <F extends Kind> OptionTMonadErrorFromMonad<F> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  @Override
  default <A> Higher2<OptionT_, F, A> raiseError(Unit error) {
    return OptionT.<F, A>none(monadF()).kind2();
  }

  @Override
  default <A> Higher2<OptionT_, F, A> handleErrorWith(Higher1<Higher1<OptionT_, F>, A> value,
      Function1<Unit, ? extends Higher1<Higher1<OptionT_, F>, A>> handler) {
    return OptionT.of(monadF(),
        monadF().flatMap(OptionT_.narrowK(value).value(),
            option -> option.fold(
              () -> handler.andThen(OptionT_::narrowK).apply(unit()).value(),
              a -> monadF().pure(Option.some(a))))).kind2();
  }
}

interface OptionTMonadErrorFromMonadError<F extends Kind, E>
    extends MonadError<Higher1<OptionT_, F>, E>, OptionTMonad<F> {

  static <F extends Kind, E> OptionTMonadErrorFromMonadError<F, E> instance(MonadError<F, E> monadF) {
    return () -> monadF;
  }

  @Override
  MonadError<F, E> monadF();

  @Override
  default <A> Higher2<OptionT_, F, A> raiseError(E error) {
    return OptionT.<F, A>of(monadF(), monadF().raiseError(error)).kind2();
  }

  @Override
  default <A> Higher2<OptionT_, F, A> handleErrorWith(Higher1<Higher1<OptionT_, F>, A> value,
      Function1<E, ? extends Higher1<Higher1<OptionT_, F>, A>> handler) {
    return OptionT.of(monadF(),
      monadF().handleErrorWith(
        OptionT_.narrowK(value).value(), error -> handler.andThen(OptionT_::narrowK).apply(error).value())).kind2();
  }
}

interface OptionTMonadThrow<F extends Kind>
    extends MonadThrow<Higher1<OptionT_, F>>,
            OptionTMonadErrorFromMonadError<F, Throwable> {

  static <F extends Kind> OptionTMonadThrow<F> instance(MonadThrow<F> monadThrowF) {
    return () -> monadThrowF;
  }
}

interface OptionTDefer<F extends Kind> extends Defer<Higher1<OptionT_, F>> {

  static <F extends Kind> OptionTDefer<F> instance(MonadDefer<F> monadDeferF) {
    return () -> monadDeferF;
  }

  MonadDefer<F> monadF();

  @Override
  default <A> Higher2<OptionT_, F, A> defer(Producer<Higher1<Higher1<OptionT_, F>, A>> defer) {
    return OptionT.of(monadF(), monadF().defer(() -> defer.map(OptionT_::narrowK).get().value())).kind2();
  }
}

interface OptionTBracket<F extends Kind> extends Bracket<Higher1<OptionT_, F>> {

  static <F extends Kind> OptionTBracket<F> instance(MonadDefer<F> monadDeferF) {
    return () -> monadDeferF;
  }

  MonadDefer<F> monadF();

  @Override
  default <A, B> Higher2<OptionT_, F, B> bracket(Higher1<Higher1<OptionT_, F>, A> acquire,
                                       Function1<A, ? extends Higher1<Higher1<OptionT_, F>, B>> use,
                                       Consumer1<A> release) {
    Higher1<F, Option<B>> bracket =
        monadF().bracket(
            acquire.fix1(OptionT_::narrowK).value(),
            option -> option.fold(
                () -> monadF().raiseError(new NoSuchElementException("could not acquire resource")),
                value -> use.andThen(OptionT_::narrowK).apply(value).value()),
            option -> option.fold(Unit::unit, release.asFunction()));
    return OptionT.of(monadF(), bracket).kind2();
  }
}

interface OptionTMonadDefer<F extends Kind>
    extends OptionTMonadThrow<F>,
            OptionTDefer<F>,
            OptionTBracket<F>,
            MonadDefer<Higher1<OptionT_, F>> {

  static <F extends Kind> OptionTMonadDefer<F> instance(MonadDefer<F> monadDeferF) {
    return () -> monadDeferF;
  }

  @Override
  default Higher2<OptionT_, F, Unit> sleep(Duration duration) {
    return OptionT.of(monadF(), monadF().map(monadF().sleep(duration), Option::some)).kind2();
  }
}
