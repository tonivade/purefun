/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Unit.unit;
import java.time.Duration;
import java.util.NoSuchElementException;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Eq;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.transformer.OptionT;
import com.github.tonivade.purefun.transformer.OptionTOf;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;

public interface OptionTInstances {

  static <F, T> Eq<Kind<Kind<OptionT<?, ?>, F>, T>> eq(Eq<Kind<F, Option<T>>> eq) {
    return (a, b) -> eq.eqv(OptionTOf.toOptionT(a).value(), OptionTOf.toOptionT(b).value());
  }

  static <F> Monad<Kind<OptionT<?, ?>, F>> monad(Monad<F> monadF) {
    return OptionTMonad.instance(checkNonNull(monadF));
  }

  static <F> MonadError<Kind<OptionT<?, ?>, F>, Unit> monadError(Monad<F> monadF) {
    return OptionTMonadErrorFromMonad.instance(checkNonNull(monadF));
  }

  static <F, E> MonadError<Kind<OptionT<?, ?>, F>, E> monadError(MonadError<F, E> monadErrorF) {
    return OptionTMonadErrorFromMonadError.instance(checkNonNull(monadErrorF));
  }

  static <F> MonadThrow<Kind<OptionT<?, ?>, F>> monadThrow(MonadThrow<F> monadThrowF) {
    return OptionTMonadThrow.instance(checkNonNull(checkNonNull(monadThrowF)));
  }

  static <F> MonadDefer<Kind<OptionT<?, ?>, F>> monadDefer(MonadDefer<F> monadDeferF) {
    return OptionTMonadDefer.instance(checkNonNull(monadDeferF));
  }

  static <F, A> Reference<Kind<OptionT<?, ?>, F>, A> ref(MonadDefer<F> monadF, A value) {
    return Reference.of(monadDefer(monadF), value);
  }
}

interface OptionTMonad<F> extends Monad<Kind<OptionT<?, ?>, F>> {

  static <F> OptionTMonad<F> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  Monad<F> monadF();

  @Override
  default <T> OptionT<F, T> pure(T value) {
    return OptionT.some(monadF(), value);
  }

  @Override
  default <T, R> OptionT<F, R> flatMap(Kind<Kind<OptionT<?, ?>, F>, ? extends T> value,
      Function1<? super T, ? extends Kind<Kind<OptionT<?, ?>, F>, ? extends R>> map) {
    return OptionTOf.toOptionT(value).flatMap(map.andThen(OptionTOf::toOptionT));
  }
}

interface OptionTMonadErrorFromMonad<F>
    extends MonadError<Kind<OptionT<?, ?>, F>, Unit>, OptionTMonad<F> {

  static <F> OptionTMonadErrorFromMonad<F> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  @Override
  default <A> OptionT<F, A> raiseError(Unit error) {
    return OptionT.none(monadF());
  }

  @Override
  default <A> OptionT<F, A> handleErrorWith(Kind<Kind<OptionT<?, ?>, F>, A> value,
      Function1<? super Unit, ? extends Kind<Kind<OptionT<?, ?>, F>, ? extends A>> handler) {
    return OptionT.of(monadF(),
        monadF().flatMap(OptionTOf.toOptionT(value).value(),
            option -> option.fold(
              () -> handler.andThen(OptionTOf::<F, A>toOptionT).apply(unit()).value(),
              a -> monadF().pure(Option.some(a)))));
  }
}

interface OptionTMonadErrorFromMonadError<F, E>
    extends MonadError<Kind<OptionT<?, ?>, F>, E>, OptionTMonad<F> {

  static <F, E> OptionTMonadErrorFromMonadError<F, E> instance(MonadError<F, E> monadF) {
    return () -> monadF;
  }

  @Override
  MonadError<F, E> monadF();

  @Override
  default <A> OptionT<F, A> raiseError(E error) {
    return OptionT.of(monadF(), monadF().raiseError(error));
  }

  @Override
  default <A> OptionT<F, A> handleErrorWith(Kind<Kind<OptionT<?, ?>, F>, A> value,
      Function1<? super E, ? extends Kind<Kind<OptionT<?, ?>, F>, ? extends A>> handler) {
    return OptionT.of(monadF(),
      monadF().handleErrorWith(
        OptionTOf.toOptionT(value).value(), error -> handler.andThen(OptionTOf::<F, A>toOptionT).apply(error).value()));
  }
}

interface OptionTMonadThrow<F>
    extends MonadThrow<Kind<OptionT<?, ?>, F>>,
            OptionTMonadErrorFromMonadError<F, Throwable> {

  static <F> OptionTMonadThrow<F> instance(MonadThrow<F> monadThrowF) {
    return () -> monadThrowF;
  }
}

interface OptionTDefer<F> extends Defer<Kind<OptionT<?, ?>, F>> {

  MonadDefer<F> monadF();

  @Override
  default <A> OptionT<F, A> defer(Producer<? extends Kind<Kind<OptionT<?, ?>, F>, ? extends A>> defer) {
    return OptionT.of(monadF(), monadF().defer(() -> defer.map(OptionTOf::<F, A>toOptionT).get().value()));
  }
}

interface OptionTBracket<F> extends Bracket<Kind<OptionT<?, ?>, F>, Throwable> {

  Bracket<F, Throwable> monadF();

  @Override
  default <A, B> OptionT<F, B> bracket(
      Kind<Kind<OptionT<?, ?>, F>, ? extends A> acquire,
      Function1<? super A, ? extends Kind<Kind<OptionT<?, ?>, F>, ? extends B>> use,
      Function1<? super A, ? extends Kind<Kind<OptionT<?, ?>, F>, Unit>> release) {
    Kind<F, Option<B>> bracket =
        monadF().bracket(
            acquire.fix(OptionTOf::<F, A>toOptionT).value(),
            option -> option.fold(
                () -> monadF().raiseError(new NoSuchElementException("could not acquire resource")),
                value -> use.andThen(OptionTOf::<F, B>toOptionT).apply(value).value()),
            option -> {
              Kind<Kind<OptionT<?, ?>, F>, Unit> fold = option.fold(() -> pure(Unit.unit()), release);
              Kind<F, Option<Unit>> value = fold.fix(OptionTOf::<F, Unit>toOptionT).value();
              return monadF().map(value, x -> x.fold(Unit::unit, identity()));
            });
    return OptionT.of(monadF(), bracket);
  }
}

interface OptionTMonadDefer<F>
    extends OptionTMonadThrow<F>,
            OptionTDefer<F>,
            OptionTBracket<F>,
            MonadDefer<Kind<OptionT<?, ?>, F>> {

  static <F> OptionTMonadDefer<F> instance(MonadDefer<F> monadDeferF) {
    return () -> monadDeferF;
  }

  @Override
  default OptionT<F, Unit> sleep(Duration duration) {
    return OptionT.of(monadF(), monadF().map(monadF().sleep(duration), Option::some));
  }
}
