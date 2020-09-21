/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Unit.unit;
import java.time.Duration;
import java.util.NoSuchElementException;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.transformer.OptionT;
import com.github.tonivade.purefun.transformer.OptionTOf;
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

  static <F extends Witness, T> Eq<Kind<Kind<OptionT_, F>, T>> eq(Eq<Kind<F, Option<T>>> eq) {
    return (a, b) -> eq.eqv(OptionTOf.narrowK(a).value(), OptionTOf.narrowK(b).value());
  }

  static <F extends Witness> Monad<Kind<OptionT_, F>> monad(Monad<F> monadF) {
    return OptionTMonad.instance(checkNonNull(monadF));
  }

  static <F extends Witness> MonadError<Kind<OptionT_, F>, Unit> monadError(Monad<F> monadF) {
    return OptionTMonadErrorFromMonad.instance(checkNonNull(monadF));
  }

  static <F extends Witness, E> MonadError<Kind<OptionT_, F>, E> monadError(MonadError<F, E> monadErrorF) {
    return OptionTMonadErrorFromMonadError.instance(checkNonNull(monadErrorF));
  }

  static <F extends Witness> MonadThrow<Kind<OptionT_, F>> monadThrow(MonadThrow<F> monadThrowF) {
    return OptionTMonadThrow.instance(checkNonNull(checkNonNull(monadThrowF)));
  }

  static <F extends Witness> MonadDefer<Kind<OptionT_, F>> monadDefer(MonadDefer<F> monadDeferF) {
    return OptionTMonadDefer.instance(checkNonNull(monadDeferF));
  }

  static <F extends Witness, A> Reference<Kind<OptionT_, F>, A> ref(MonadDefer<F> monadF, A value) {
    return Reference.of(monadDefer(monadF), value);
  }
}

interface OptionTMonad<F extends Witness> extends Monad<Kind<OptionT_, F>> {

  static <F extends Witness> OptionTMonad<F> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  Monad<F> monadF();

  @Override
  default <T> OptionT<F, T> pure(T value) {
    return OptionT.some(monadF(), value);
  }

  @Override
  default <T, R> OptionT<F, R> flatMap(Kind<Kind<OptionT_, F>, T> value,
      Function1<? super T, ? extends Kind<Kind<OptionT_, F>, ? extends R>> map) {
    return OptionTOf.narrowK(value).flatMap(map.andThen(OptionTOf::narrowK));
  }
}

interface OptionTMonadErrorFromMonad<F extends Witness>
    extends MonadError<Kind<OptionT_, F>, Unit>, OptionTMonad<F> {

  static <F extends Witness> OptionTMonadErrorFromMonad<F> instance(Monad<F> monadF) {
    return () -> monadF;
  }

  @Override
  default <A> OptionT<F, A> raiseError(Unit error) {
    return OptionT.<F, A>none(monadF());
  }

  @Override
  default <A> OptionT<F, A> handleErrorWith(Kind<Kind<OptionT_, F>, A> value,
      Function1<? super Unit, ? extends Kind<Kind<OptionT_, F>, ? extends A>> handler) {
    return OptionT.of(monadF(),
        monadF().flatMap(OptionTOf.<F, A>narrowK(value).value(),
            option -> option.fold(
              () -> handler.andThen(OptionTOf::<F, A>narrowK).apply(unit()).value(),
              a -> monadF().pure(Option.some(a)))));
  }
}

interface OptionTMonadErrorFromMonadError<F extends Witness, E>
    extends MonadError<Kind<OptionT_, F>, E>, OptionTMonad<F> {

  static <F extends Witness, E> OptionTMonadErrorFromMonadError<F, E> instance(MonadError<F, E> monadF) {
    return () -> monadF;
  }

  @Override
  MonadError<F, E> monadF();

  @Override
  default <A> OptionT<F, A> raiseError(E error) {
    return OptionT.<F, A>of(monadF(), monadF().raiseError(error));
  }

  @Override
  default <A> OptionT<F, A> handleErrorWith(Kind<Kind<OptionT_, F>, A> value,
      Function1<? super E, ? extends Kind<Kind<OptionT_, F>, ? extends A>> handler) {
    return OptionT.of(monadF(),
      monadF().handleErrorWith(
        OptionTOf.<F, A>narrowK(value).value(), error -> handler.andThen(OptionTOf::<F, A>narrowK).apply(error).value()));
  }
}

interface OptionTMonadThrow<F extends Witness>
    extends MonadThrow<Kind<OptionT_, F>>,
            OptionTMonadErrorFromMonadError<F, Throwable> {

  static <F extends Witness> OptionTMonadThrow<F> instance(MonadThrow<F> monadThrowF) {
    return () -> monadThrowF;
  }
}

interface OptionTDefer<F extends Witness> extends Defer<Kind<OptionT_, F>> {

  MonadDefer<F> monadF();

  @Override
  default <A> OptionT<F, A> defer(Producer<Kind<Kind<OptionT_, F>, A>> defer) {
    return OptionT.of(monadF(), monadF().defer(() -> defer.map(OptionTOf::narrowK).get().value()));
  }
}

interface OptionTBracket<F extends Witness> extends Bracket<Kind<OptionT_, F>, Throwable> {

  Bracket<F, Throwable> monadF();

  @Override
  default <A, B> OptionT<F, B> bracket(Kind<Kind<OptionT_, F>, A> acquire,
                                       Function1<A, ? extends Kind<Kind<OptionT_, F>, B>> use,
                                       Consumer1<A> release) {
    Kind<F, Option<B>> bracket =
        monadF().bracket(
            acquire.fix(OptionTOf::narrowK).value(),
            option -> option.fold(
                () -> monadF().raiseError(new NoSuchElementException("could not acquire resource")),
                value -> use.andThen(OptionTOf::narrowK).apply(value).value()),
            option -> option.fold(Unit::unit, release.asFunction()));
    return OptionT.of(monadF(), bracket);
  }
}

interface OptionTMonadDefer<F extends Witness>
    extends OptionTMonadThrow<F>,
            OptionTDefer<F>,
            OptionTBracket<F>,
            MonadDefer<Kind<OptionT_, F>> {

  static <F extends Witness> OptionTMonadDefer<F> instance(MonadDefer<F> monadDeferF) {
    return () -> monadDeferF;
  }

  @Override
  default OptionT<F, Unit> sleep(Duration duration) {
    return OptionT.of(monadF(), monadF().map(monadF().sleep(duration), Option::some));
  }
}
