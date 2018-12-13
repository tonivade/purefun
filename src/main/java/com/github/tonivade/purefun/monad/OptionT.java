/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Producer.unit;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Filterable;
import com.github.tonivade.purefun.FlatMap2;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Transformer;

public final class OptionT<W extends Kind, T> implements FlatMap2<OptionT.µ, W, T>, Filterable<T> {

  public static final class µ implements Kind {}

  private final Monad<W> monad;
  private final Higher1<W, Option<T>> value;

  private OptionT(Monad<W> monad, Higher1<W, Option<T>> value) {
    this.monad = requireNonNull(monad);
    this.value = requireNonNull(value);
  }

  @Override
  public <R> OptionT<W, R> map(Function1<T, R> map) {
    return OptionT.of(monad, monad.map(value, v -> v.map(map)));
  }

  @Override
  public <R> OptionT<W, R> flatMap(Function1<T, ? extends Higher2<OptionT.µ, W, R>> map) {
    return OptionT.of(monad, flatMapF(v -> OptionT.narrowK(map.apply(v)).value));
  }

  public <R> Higher1<W, R> fold(Producer<R> orElse, Function1<T, R> map) {
    return monad.map(value, v -> v.fold(orElse, map));
  }

  public <F extends Kind> OptionT<F, T> mapK(Monad<F> other, Transformer<W, F> transformer) {
    return OptionT.of(other, transformer.apply(value));
  }

  public Higher1<W, T> get() {
    return monad.map(value, Option::get);
  }

  public Higher1<W, Boolean> isEmpty() {
    return monad.map(value, Option::isEmpty);
  }

  public Higher1<W, T> orElse(T orElse) {
    return orElse(unit(orElse));
  }

  public Higher1<W, T> orElse(Producer<T> orElse) {
    return fold(orElse, identity());
  }

  @Override
  public OptionT<W, T> filter(Matcher1<T> filter) {
    return new OptionT<>(monad, monad.map(value, v -> v.filter(filter)));
  }

  public static <W extends Kind, T> OptionT<W, T> lift(Monad<W> monad, Option<T> value) {
    return of(monad, monad.pure(value));
  }

  public static <W extends Kind, T> OptionT<W, T> of(Monad<W> monad, Higher1<W, Option<T>> value) {
    return new OptionT<>(monad, value);
  }

  public static <W extends Kind, T> OptionT<W, T> some(Monad<W> monad, T value) {
    return lift(monad, Option.some(value));
  }

  public static <W extends Kind, T> OptionT<W, T> none(Monad<W> monad) {
    return lift(monad, Option.none());
  }

  public static <W extends Kind> Monad<Higher1<OptionT.µ, W>> monad(Monad<W> monad) {
    return new Monad<Higher1<OptionT.µ, W>>() {

      @Override
      public <T> OptionT<W, T> pure(T value) {
        return OptionT.some(monad, value);
      }

      @Override
      public <T, R> OptionT<W, R> flatMap(Higher1<Higher1<OptionT.µ, W>, T> value,
          Function1<T, ? extends Higher1<Higher1<OptionT.µ, W>, R>> map) {
        return OptionT.narrowK(value).flatMap(map.andThen(OptionT::narrowK));
      }
    };
  }

  public static <W extends Kind, T> OptionT<W, T> narrowK(Higher2<OptionT.µ, W, T> hkt) {
    return (OptionT<W, T>) hkt;
  }

  public static <W extends Kind, T> OptionT<W, T> narrowK(Higher1<Higher1<OptionT.µ, W>, T> hkt) {
    return (OptionT<W, T>) hkt;
  }

  private <R> Higher1<W, Option<R>> flatMapF(Function1<T, Higher1<W, Option<R>>> map) {
   return monad.flatMap(value, v -> v.fold(unit(monad.pure(Option.none())), map));
  }
}
