/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Function1.identity;
import static java.util.Objects.requireNonNull;

import java.util.stream.Stream;

import com.github.tonivade.purefun.type.Either;

public interface Trampoline<T> extends FlatMap1<Trampoline.µ, T>, Holder<T> {

  static final class µ implements Kind {}

  Trampoline<T> apply();

  boolean complete();

  @Override
  default <R> Trampoline<R> map(Function1<T, R> map) {
    return TrampolineModule.resume(this)
        .fold(left -> more(() -> left.map(map)),
              right -> done(map.apply(right)));
  }

  @Override
  default <R> Trampoline<R> flatMap(Function1<T, ? extends Higher1<Trampoline.µ, R>> map) {
    return TrampolineModule.resume(this)
        .fold(left -> more(() -> left.flatMap(map)),
              right -> map.andThen(Trampoline::narrowK).apply(right));
  }

  default <R> R fold(Function1<Trampoline<T>, R> more, Function1<T, R> done) {
    return complete() ? done.apply(get()) : more.apply(apply());
  }

  default T run() {
    return TrampolineModule.iterate(this).get();
  }

  @Override
  @SuppressWarnings("unchecked")
  default <V> Trampoline<V> flatten() {
    try {
      return ((Trampoline<Trampoline<V>>) this).flatMap(identity());
    } catch (ClassCastException e) {
      throw new UnsupportedOperationException("cannot be flattened");
    }
  }

  TrampolineModule module();

  static <T> Trampoline<T> done(T value) {
    return new Done<>(value);
  }

  static <T> Trampoline<T> more(Producer<Trampoline<T>> next) {
    return new More<>(next);
  }

  static <T> Trampoline<T> narrowK(Higher1<Trampoline.µ, T> hkt) {
    return (Trampoline<T>) hkt;
  }

  final class Done<T> implements Trampoline<T> {
    final T value;

    Done(T value) {
      this.value = requireNonNull(value);
    }

    @Override
    public boolean complete() {
      return true;
    }

    @Override
    public T get() {
      return value;
    }

    @Override
    public Trampoline<T> apply() {
      throw new UnsupportedOperationException();
    }

    @Override
    public TrampolineModule module() {
      throw new UnsupportedOperationException();
    }
  }

  final class More<T> implements Trampoline<T> {
    final Producer<Trampoline<T>> next;

    More(Producer<Trampoline<T>> next) {
      this.next = requireNonNull(next);
    }

    @Override
    public boolean complete() {
      return false;
    }

    @Override
    public T get() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Trampoline<T> apply() {
      return next.get();
    }

    @Override
    public TrampolineModule module() {
      throw new UnsupportedOperationException();
    }
  }
}

interface TrampolineModule {

  static <T> Trampoline<T> iterate(Trampoline<T> trampoline) {
    return Stream.iterate(trampoline, Trampoline::apply)
        .filter(Trampoline::complete).findFirst().get();
  }

  static <T> Either<Trampoline<T>, T> resume(Trampoline<T> trampoline) {
    return trampoline.fold(Either::left, Either::right);
  }
}
