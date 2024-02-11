/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import java.util.stream.Stream;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.type.Either;

@HigherKind
public sealed interface Trampoline<T> extends TrampolineOf<T>, Bindable<Trampoline_, T> {

  Trampoline<T> apply();

  boolean complete();

  T get();

  default <R> Trampoline<R> map(Function1<? super T, ? extends R> map) {
    return resume()
        .fold(next -> more(() -> next.map(map)),
              value -> done(map.apply(value)));
  }

  default <R> Trampoline<R> flatMap(Function1<? super T, ? extends Kind<Trampoline_, ? extends R>> map) {
    return resume()
        .fold(next -> more(() -> next.flatMap(map)), map.andThen(TrampolineOf::narrowK));
  }

  default <R> R fold(Function1<Trampoline<T>, R> more, Function1<T, R> done) {
    return complete() ? done.apply(get()) : more.apply(apply());
  }

  default T run() {
    return iterate().get();
  }

  static <T> Trampoline<T> done(T value) {
    return new Done<>(value);
  }

  static <T> Trampoline<T> more(Producer<Trampoline<T>> next) {
    return new More<>(next);
  }

  record Done<T>(T value) implements Trampoline<T> {

      public Done {
        checkNonNull(value);
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
  }

  record More<T>(Producer<Trampoline<T>> next) implements Trampoline<T> {

      public More {
        checkNonNull(next);
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
  }

  private Trampoline<T> iterate() {
    return Stream.iterate(this, Trampoline::apply)
            .filter(Trampoline::complete).findFirst().orElseThrow(IllegalStateException::new);
  }

  private Either<Trampoline<T>, T> resume() {
    return fold(Either::left, Either::right);
  }
}