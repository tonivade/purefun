/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static java.util.Objects.requireNonNull;

import java.util.stream.Stream;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.type.Either;

@HigherKind
public interface Trampoline<T> {

  Trampoline<T> apply();

  boolean complete();

  T get();

  default <R> Trampoline<R> map(Function1<T, R> map) {
    return TrampolineModule.resume(this)
        .fold(next -> more(() -> next.map(map)),
              value -> done(map.apply(value)));
  }

  default <R> Trampoline<R> flatMap(Function1<T, Trampoline<R>> map) {
    return TrampolineModule.resume(this)
        .fold(next -> more(() -> next.flatMap(map)), map::apply);
  }

  default <R> R fold(Function1<Trampoline<T>, R> more, Function1<T, R> done) {
    return complete() ? done.apply(get()) : more.apply(apply());
  }

  default T run() {
    return TrampolineModule.iterate(this).get();
  }

  TrampolineModule module();

  static <T> Trampoline<T> done(T value) {
    return new Done<>(value);
  }

  static <T> Trampoline<T> more(Producer<Trampoline<T>> next) {
    return new More<>(next);
  }

  final class Done<T> implements Trampoline<T> {

    final T value;

    private Done(T value) {
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

    private More(Producer<Trampoline<T>> next) {
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
        .filter(Trampoline::complete).findFirst().orElseThrow(IllegalStateException::new);
  }

  static <T> Either<Trampoline<T>, T> resume(Trampoline<T> trampoline) {
    return trampoline.fold(Either::left, Either::right);
  }
}