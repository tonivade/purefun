/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nullable;

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

  @SuppressWarnings({ "rawtypes", "unchecked" })
  default T run() {
    Trampoline<?> current = this;
    Function1<Object, Trampoline<?>> continuation = null;

    while (true) {
      if (current instanceof Done<?> done) {
        Object value = done.value();

        if (continuation == null) {
          return (T) value;  // end of program
        }

        Function1<Object, Trampoline<?>> k = continuation;
        continuation = null; // clear before applying
        current = k.apply(value);
      } else if (current instanceof More<?> more) {
        current = more.next().get();
      } else if (current instanceof FlatMap<?, ?> flatMap) {
        Trampoline<Object> source = (Trampoline<Object>) flatMap.current();
        Function1<Object, Trampoline<?>> nextFn = (Function1) flatMap.mapper();

        if (source instanceof FlatMap<?, ?> sourceFlatMap) {
          // Reassociate:
          // FlatMap(FlatMap(x, f), g)
          // becomes
          // FlatMap(x, v -> FlatMap(f(v), g))
          Trampoline<Object> inner = (Trampoline<Object>) sourceFlatMap.current();
          Function1<Object, Trampoline<?>> innerFn = (Function1) sourceFlatMap.mapper();

          Function1<Object, Trampoline<?>> merged = v -> {
            Trampoline t = innerFn.apply(v);
            return t.flatMap(nextFn);
          };

          current = inner;
          continuation = chain(merged, continuation);
        } else {
          current = source;
          continuation = chain(nextFn, continuation);
        }
      }
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static Function1<Object, Trampoline<?>> chain(
      Function1<Object, Trampoline<?>> newK, @Nullable Function1<Object, Trampoline<?>> existing) {
    if (existing == null) {
      return newK;
    }

    return value -> {
      Trampoline<?> next = newK.apply(value);
      return new FlatMap(next, existing);
    };
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