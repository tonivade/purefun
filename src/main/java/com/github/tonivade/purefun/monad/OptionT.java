/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.Producer.unit;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Filterable;
import com.github.tonivade.purefun.FlatMap2;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Eq;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.Transformer;

public final class OptionT<F extends Kind, T> implements FlatMap2<OptionT.µ, F, T>, Filterable<T> {

  public static final class µ implements Kind {}

  private final Monad<F> monad;
  private final Higher1<F, Option<T>> value;

  private OptionT(Monad<F> monad, Higher1<F, Option<T>> value) {
    this.monad = requireNonNull(monad);
    this.value = requireNonNull(value);
  }

  @Override
  public <R> OptionT<F, R> map(Function1<T, R> map) {
    return OptionT.of(monad, monad.map(value, v -> v.map(map)));
  }

  @Override
  public <R> OptionT<F, R> flatMap(Function1<T, ? extends Higher2<OptionT.µ, F, R>> map) {
    return OptionT.of(monad, flatMapF(v -> OptionT.narrowK(map.apply(v)).value));
  }

  public <R> Higher1<F, R> fold(Producer<R> orElse, Function1<T, R> map) {
    return monad.map(value, v -> v.fold(orElse, map));
  }

  public <G extends Kind> OptionT<G, T> mapK(Monad<G> other, Transformer<F, G> transformer) {
    return OptionT.of(other, transformer.apply(value));
  }

  public Higher1<F, T> get() {
    return monad.map(value, Option::get);
  }

  public Higher1<F, Boolean> isEmpty() {
    return monad.map(value, Option::isEmpty);
  }

  public Higher1<F, T> getOrElse(T orElse) {
    return getOrElse(unit(orElse));
  }

  public Higher1<F, T> getOrElse(Producer<T> orElse) {
    return fold(orElse, identity());
  }

  @Override
  public OptionT<F, T> filter(Matcher1<T> filter) {
    return new OptionT<>(monad, monad.map(value, v -> v.filter(filter)));
  }

  Higher1<F, Option<T>> value() {
    return value;
  }

  public static <F extends Kind, T> OptionT<F, T> lift(Monad<F> monad, Option<T> value) {
    return of(monad, monad.pure(value));
  }

  public static <F extends Kind, T> OptionT<F, T> of(Monad<F> monad, Higher1<F, Option<T>> value) {
    return new OptionT<>(monad, value);
  }

  public static <F extends Kind, T> OptionT<F, T> some(Monad<F> monad, T value) {
    return lift(monad, Option.some(value));
  }

  public static <F extends Kind, T> OptionT<F, T> none(Monad<F> monad) {
    return lift(monad, Option.none());
  }

  public static <F extends Kind, T> Eq<Higher2<OptionT.µ, F, T>> eq(Eq<Higher1<F, Option<T>>> eq) {
    return (a, b) -> eq.eqv(narrowK(a).value, narrowK(b).value);
  }

  public static <F extends Kind> Monad<Higher1<OptionT.µ, F>> monad(Monad<F> monadF) {
    return new OptionTMonad<F>() {

      @Override
      public Monad<F> monadF() {
        return monadF;
      }
    };
  }

  public static <F extends Kind> MonadError<Higher1<OptionT.µ, F>, Nothing> monadError(Monad<F> monadF) {
    return new OptionTMonadErrorFromMonad<F>() {

      @Override
      public Monad<F> monadF() {
        return monadF;
      }
    };
  }

  public static <F extends Kind, E> MonadError<Higher1<OptionT.µ, F>, E> monadError(MonadError<F, E> monadErrorF) {
    return new OptionTMonadErrorFromMonadError<F, E>() {

      @Override
      public MonadError<F, E> monadErrorF() {
        return monadErrorF;
      }
    };
  }

  public static <F extends Kind, T> OptionT<F, T> narrowK(Higher2<OptionT.µ, F, T> hkt) {
    return (OptionT<F, T>) hkt;
  }

  public static <F extends Kind, T> OptionT<F, T> narrowK(Higher1<Higher1<OptionT.µ, F>, T> hkt) {
    return (OptionT<F, T>) hkt;
  }

  private <R> Higher1<F, Option<R>> flatMapF(Function1<T, Higher1<F, Option<R>>> map) {
   return monad.flatMap(value, v -> v.fold(unit(monad.pure(Option.none())), map));
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

interface OptionTMonadErrorFromMonad<F extends Kind> extends MonadError<Higher1<OptionT.µ, F>, Nothing>, OptionTMonad<F> {

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

interface OptionTMonadErrorFromMonadError<F extends Kind, E> extends MonadError<Higher1<OptionT.µ, F>, E>, OptionTMonad<F> {

  MonadError<F, E> monadErrorF();

  @Override
  default Monad<F> monadF() {
    return monadErrorF();
  }

  @Override
  default <A> OptionT<F, A> raiseError(E error) {
    return OptionT.of(monadF(), monadErrorF().raiseError(error));
  }

  @Override
  default <A> OptionT<F, A> handleErrorWith(Higher1<Higher1<OptionT.µ, F>, A> value,
      Function1<E, ? extends Higher1<Higher1<OptionT.µ, F>, A>> handler) {
    return OptionT.of(monadF(), monadErrorF().handleErrorWith(OptionT.narrowK(value).value(),
        error -> handler.andThen(OptionT::narrowK).apply(error).value()));
  }
}