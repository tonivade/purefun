/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Nothing.nothing;

import com.github.tonivade.purefun.CheckedConsumer1;
import com.github.tonivade.purefun.FlatMap1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.algebra.Monad;
import com.github.tonivade.purefun.data.Sequence;

@FunctionalInterface
public interface IO<T> extends FlatMap1<IO.µ, T> {

  final class µ implements Kind {}

  T unsafeRunSync();

  @Override
  default <R> IO<R> map(Function1<T, R> map) {
    return () -> map.apply(unsafeRunSync());
  }

  @Override
  default <R> IO<R> flatMap(Function1<T, ? extends Higher1<IO.µ, R>> map) {
    return () -> map.andThen(IO::narrowK).apply(unsafeRunSync()).unsafeRunSync();
  }

  default <R> IO<R> andThen(IO<R> after) {
    return flatMap(ignore -> after);
  }

  static <T> IO<T> pure(T value) {
    return () -> value;
  }

  static <T, R> Function1<T, IO<R>> lift(Function1<T, R> task) {
    return task.andThen(IO::pure);
  }

  static IO<Nothing> exec(Runnable task) {
    return () -> { task.run(); return nothing(); };
  }

  static <T> IO<T> of(Producer<T> producer) {
    return producer::get;
  }

  static IO<Nothing> noop() {
    return pure(nothing());
  }

  static <T, R> IO<R> bracket(IO<T> acquire,
                              Function1<T, IO<R>> use,
                              CheckedConsumer1<T> release) {
    return () -> {
      try (IOResource<T> resource = new IOResource<>(acquire.unsafeRunSync(), release)) {
        return resource.apply(use).unsafeRunSync();
      }
    };
  }

  static <T extends AutoCloseable, R> IO<R> bracket(IO<T> acquire,
                                                    Function1<T, IO<R>> use) {
    return bracket(acquire, use, AutoCloseable::close);
  }

  static IO<Nothing> sequence(Sequence<IO<?>> sequence) {
    return sequence.fold(noop(), IO::andThen).andThen(noop());
  }

  static <T> IO<T> narrowK(Higher1<IO.µ, T> hkt) {
    return (IO<T>) hkt;
  }

  static Monad<IO.µ> monad() {
    return new Monad<IO.µ>() {

      @Override
      public <T> IO<T> pure(T value) {
        return pure(value);
      }

      @Override
      public <T, R> IO<R> flatMap(Higher1<IO.µ, T> value, Function1<T, ? extends Higher1<IO.µ, R>> map) {
        return narrowK(value).flatMap(map);
      }
    };
  }
}

final class IOResource<T> implements AutoCloseable {
  final T resource;
  final CheckedConsumer1<T> release;

  IOResource(T resource, CheckedConsumer1<T> release) {
    this.resource = resource;
    this.release = release;
  }

  public <R> IO<R> apply(Function1<T, IO<R>> use) {
    return use.apply(resource);
  }

  @Override
  public void close() {
    release.unchecked().accept(resource);
  }
}
