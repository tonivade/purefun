/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import java.util.ArrayDeque;
import java.util.Deque;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;

@HigherKind
public sealed interface Trampoline<T> extends TrampolineOf<T>, Bindable<Trampoline<?>, T> {

  @Override
  default <R> Trampoline<R> map(Function1<? super T, ? extends R> map) {
    return flatMap(map.andThen(Trampoline::done));
  }

  @Override
  default <R> Trampoline<R> flatMap(Function1<? super T, ? extends Kind<Trampoline<?>, ? extends R>> map) {
    return new FlatMap<>(this, map);
  }

  @SuppressWarnings("unchecked")
  default T run() {
    Trampoline<?> current = this;
    Deque<Function1<Object, Trampoline<?>>> stack = new ArrayDeque<>();

    while (true) {
      if (current instanceof Done<?> done) {
        var value = done.value();

        if (stack.isEmpty()) {
          return (T) value; // end of program
        }

        Function1<Object, Trampoline<?>> k = stack.pop();
        current = k.apply(value);
      } else if (current instanceof More<?> more) {
        current = more.next().get();
      } else if (current instanceof FlatMap<?, ?> flatMap) {
        Trampoline<Object> source = (Trampoline<Object>) flatMap.current();
        Function1<Object, Trampoline<?>> nextFn = (Function1<Object, Trampoline<?>>) flatMap.mapper();

        stack.push(nextFn);
        current = source;
      }
    }
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
  }

  record More<T>(Producer<Trampoline<T>> next) implements Trampoline<T> {

    public More {
      checkNonNull(next);
    }
  }

  record FlatMap<T, R>(Trampoline<T> current, Function1<? super T, ? extends Kind<Trampoline<?>, ? extends R>> mapper) implements Trampoline<R> {

    public FlatMap {
      checkNonNull(current);
      checkNonNull(mapper);
    }
  }

}