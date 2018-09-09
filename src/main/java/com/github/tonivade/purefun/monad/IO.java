/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Nothing.nothing;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Monad;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.data.Sequence;

@FunctionalInterface
public interface IO<T> extends Monad<IOKind.µ, T> {

  T unsafeRunSync();

  @Override
  default <R> IO<R> map(Function1<T, R> map) {
    return () -> map.apply(unsafeRunSync());
  }

  @Override
  default <R> IO<R> flatMap(Function1<T, ? extends Higher<IOKind.µ, R>> map) {
    return () -> map.andThen(IOKind::narrowK).apply(unsafeRunSync()).unsafeRunSync();
  }

  default <R> IO<R> andThen(IO<R> after) {
    return flatMap(ignore -> after);
  }

  static <T> IO<T> unit(T value) {
    return () -> value;
  }

  static IO<Nothing> exec(Runnable task) {
    return () -> { task.run(); return nothing(); };
  }

  static <T> IO<T> of(Producer<T> producer) {
    return producer::get;
  }

  static IO<Nothing> noop() {
    return unit(nothing());
  }

  static IO<Nothing> sequence(Sequence<IO<?>> sequence) {
    return sequence.fold(noop(), IO::andThen).andThen(noop());
  }
}
