/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.Producer.cons;

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
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Eq;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.Transformer;

public interface OptionT<F extends Kind, T> extends FlatMap2<OptionT.µ, F, T>, Filterable<T> {

  final class µ implements Kind {}

  Monad<F> monad();
  Higher1<F, Option<T>> value();

  @Override
  default <R> OptionT<F, R> map(Function1<T, R> map) {
    return OptionT.of(monad(), monad().map(value(), v -> v.map(map)));
  }

  @Override
  default <R> OptionT<F, R> flatMap(Function1<T, ? extends Higher2<OptionT.µ, F, R>> map) {
    return OptionT.of(monad(), flatMapF(v -> map.andThen(OptionT::narrowK).apply(v).value()));
  }

  default <R> Higher1<F, R> fold(Producer<R> orElse, Function1<T, R> map) {
    return monad().map(value(), v -> v.fold(orElse, map));
  }

  default <G extends Kind> OptionT<G, T> mapK(Monad<G> other, Transformer<F, G> transformer) {
    return OptionT.of(other, transformer.apply(value()));
  }

  default Higher1<F, T> get() {
    return monad().map(value(), Option::get);
  }

  default Higher1<F, Boolean> isEmpty() {
    return monad().map(value(), Option::isEmpty);
  }

  default Higher1<F, T> getOrElse(T orElse) {
    return getOrElse(cons(orElse));
  }

  default Higher1<F, T> getOrElse(Producer<T> orElse) {
    return fold(orElse, identity());
  }

  @Override
  default OptionT<F, T> filter(Matcher1<T> filter) {
    return OptionT.of(monad(), monad().map(value(), v -> v.filter(filter)));
  }

  static <F extends Kind, T> OptionT<F, T> lift(Monad<F> monad, Option<T> value) {
    return OptionT.of(monad, monad.pure(value));
  }

  static <F extends Kind, T> OptionT<F, T> of(Monad<F> monad, Higher1<F, Option<T>> value) {
    return new OptionT<F, T>() {

      @Override
      public Monad<F> monad() { return monad; }

      @Override
      public Higher1<F, Option<T>> value() { return value; }
    };
  }

  static <F extends Kind, T> OptionT<F, T> some(Monad<F> monad, T value) {
    return lift(monad, Option.some(value));
  }

  static <F extends Kind, T> OptionT<F, T> none(Monad<F> monad) {
    return lift(monad, Option.none());
  }

  static <F extends Kind, T> Eq<Higher2<OptionT.µ, F, T>> eq(Eq<Higher1<F, Option<T>>> eq) {
    return (a, b) -> eq.eqv(narrowK(a).value(), narrowK(b).value());
  }

  static <F extends Kind> Monad<Higher1<OptionT.µ, F>> monad(Monad<F> monadF) {
    return new OptionTMonad<F>() {

      @Override
      public Monad<F> monadF() { return monadF; }
    };
  }

  static <F extends Kind> MonadError<Higher1<OptionT.µ, F>, Nothing> monadError(Monad<F> monadF) {
    return new OptionTMonadErrorFromMonad<F>() {

      @Override
      public Monad<F> monadF() { return monadF; }
    };
  }

  static <F extends Kind, E> MonadError<Higher1<OptionT.µ, F>, E> monadError(MonadError<F, E> monadErrorF) {
    return new OptionTMonadErrorFromMonadError<F, E>() {

      @Override
      public MonadError<F, E> monadF() { return monadErrorF; }
    };
  }

  static <F extends Kind> Defer<Higher1<OptionT.µ, F>> defer(Monad<F> monad, Defer<F> defer) {
    return new OptionTDefer<F>() {

      @Override
      public Monad<F> monadF() { return monad; }

      @Override
      public Defer<F> deferF() { return defer; }
    };
  }

  static <F extends Kind, T> OptionT<F, T> narrowK(Higher2<OptionT.µ, F, T> hkt) {
    return (OptionT<F, T>) hkt;
  }

  static <F extends Kind, T> OptionT<F, T> narrowK(Higher1<Higher1<OptionT.µ, F>, T> hkt) {
    return (OptionT<F, T>) hkt;
  }

  default <R> Higher1<F, Option<R>> flatMapF(Function1<T, Higher1<F, Option<R>>> map) {
   return monad().flatMap(value(), v -> v.fold(cons(monad().pure(Option.none())), map));
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