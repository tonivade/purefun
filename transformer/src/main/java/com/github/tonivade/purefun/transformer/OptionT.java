/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Producer.cons;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Filterable;
import com.github.tonivade.purefun.FlatMap2;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Transformer;

@HigherKind
public interface OptionT<F extends Kind, T> extends FlatMap2<OptionT.µ, F, T>, Filterable<T> {

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
  
  @Override
  default OptionT<F, T> filterNot(Matcher1<T> matcher) {
    return filter(matcher.negate());
  }

  static <F extends Kind, T> OptionT<F, T> lift(Monad<F> monad, Option<T> value) {
    return OptionT.of(monad, monad.pure(value));
  }

  static <F extends Kind, T> OptionT<F, T> of(Monad<F> monad, Higher1<F, Option<T>> value) {
    requireNonNull(monad);
    requireNonNull(value);
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

  default <R> Higher1<F, Option<R>> flatMapF(Function1<T, Higher1<F, Option<R>>> map) {
   return monad().flatMap(value(), v -> v.fold(cons(monad().pure(Option.none())), map));
  }
}
