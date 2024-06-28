/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Function1.cons;
import static com.github.tonivade.purefun.core.Function1.fail;
import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import java.util.stream.Stream;

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
    return fold(
        next -> more(() -> next.flatMap(map)),
        value -> {
          Kind<Trampoline<?>, R> apply = Kind.narrowK(map.apply(value));
          return apply.fix(TrampolineOf::toTrampoline);
        });
  }

  default <R> R fold(Function1<Trampoline<T>, R> more, Function1<T, R> done) {
    return switch (this) {
      case Done(var value) -> done.apply(value);
      case More(var next) -> more.apply(next.get());
    };
  }

  default T run() {
    return iterate().fold(fail(IllegalStateException::new), identity());
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

  private Trampoline<T> iterate() {
    return Stream.iterate(this, t -> t.fold(identity(), cons(t)))
        .dropWhile(t -> t instanceof More).findFirst().orElseThrow();
  }
}