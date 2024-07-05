/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Producer.cons;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface OptionT<F extends Kind<F, ?>, T> extends Kind<OptionT<F, ?>, T>, Bindable<OptionT<F, ?>, T> {

  Monad<F> monad();
  Kind<F, Option<T>> value();

  @Override
  default <R> OptionT<F, R> map(Function1<? super T, ? extends R> map) {
    return OptionT.of(monad(), monad().map(value(), v -> v.map(map)));
  }

  @Override
  default <R> OptionT<F, R> flatMap(Function1<? super T, ? extends Kind<OptionT<F, ?>, ? extends R>> map) {
    return OptionT.of(monad(), flatMapF(v -> map.apply(v).<OptionT<F, R>>fix().value()));
  }

  default <R> Kind<F, R> fold(Producer<? extends R> orElse, Function1<? super T, ? extends R> map) {
    return monad().map(value(), v -> v.fold(orElse, map));
  }

  default <G extends Kind<G, ?>> OptionT<G, T> mapK(Monad<G> other, FunctionK<F, G> functionK) {
    return OptionT.of(other, functionK.apply(value()));
  }

  default Kind<F, T> getOrElseThrow() {
    return monad().map(value(), Option::getOrElseThrow);
  }

  default Kind<F, Boolean> isEmpty() {
    return monad().map(value(), Option::isEmpty);
  }

  default Kind<F, T> getOrElse(T orElse) {
    return getOrElse(cons(orElse));
  }

  default Kind<F, T> getOrElse(Producer<? extends T> orElse) {
    return fold(orElse, identity());
  }

  default OptionT<F, T> filter(Matcher1<? super T> filter) {
    return OptionT.of(monad(), monad().map(value(), v -> v.filter(filter)));
  }

  default OptionT<F, T> filterNot(Matcher1<? super T> matcher) {
    return filter(matcher.negate());
  }

  static <F extends Kind<F, ?>, T> OptionT<F, T> lift(Monad<F> monad, Option<T> value) {
    return OptionT.of(monad, monad.pure(value));
  }

  static <F extends Kind<F, ?>, T> OptionT<F, T> of(Monad<F> monad, Kind<F, Option<T>> value) {
    checkNonNull(monad);
    checkNonNull(value);
    return new OptionT<>() {

      @Override
      public Monad<F> monad() { return monad; }

      @Override
      public Kind<F, Option<T>> value() { return value; }
    };
  }

  static <F extends Kind<F, ?>, T> OptionT<F, T> some(Monad<F> monad, T value) {
    return lift(monad, Option.some(value));
  }

  static <F extends Kind<F, ?>, T> OptionT<F, T> none(Monad<F> monad) {
    return lift(monad, Option.none());
  }

  default <R> Kind<F, Option<R>> flatMapF(Function1<? super T, ? extends Kind<F, ? extends Option<R>>> map) {
   return monad().flatMap(value(), v -> v.fold(cons(monad().pure(Option.none())), map));
  }
}
