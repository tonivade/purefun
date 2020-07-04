/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Consumer1.noop;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@HigherKind
public final class Resource<F extends Witness, T> implements ResourceOf<F, T> {
  
  private final MonadDefer<F> monad;
  private final Kind<F, Tuple2<T, Consumer1<T>>> resource;
  
  protected Resource(MonadDefer<F> monad, Kind<F, Tuple2<T, Consumer1<T>>> resource) {
    this.monad = requireNonNull(monad);
    this.resource = requireNonNull(resource);
  }
  
  public <R> Resource<F, R> map(Function1<T, R> mapper) {
    return flatMap(t -> pure(monad, mapper.andThen(monad::pure).apply(t)));
  }
  
  public <R> Resource<F, R> flatMap(Function1<T, Resource<F, R>> mapper) {
    return new Resource<>(monad, use(t -> mapper.apply(t).resource));
  }
  
  public <R> Kind<F, R> use(Function1<T, Kind<F, R>> use) {
    return monad.bracket(resource, t -> use.apply(t.get1()), this::release);
  }
  
  public static <F extends Witness, T> Resource<F, T> pure(
      MonadDefer<F> monad, Kind<F, T> acquire) {
    return new Resource<>(monad, monad.map(acquire, t -> Tuple.of(t, noop())));
  }

  public static <F extends Witness, T> Resource<F, T> from(
      MonadDefer<F> monad, Kind<F, T> acquire, Consumer1<T> release) {
    return new Resource<>(monad, monad.map(acquire, t -> Tuple.of(t, release)));
  }

  public static <F extends Witness, T extends AutoCloseable> Resource<F, T> from(
      MonadDefer<F> monad, Kind<F, T> acquire) {
    return from(monad, acquire, AutoCloseable::close);
  }

  private void release(Tuple2<T, Consumer1<T>> t) {
    t.get2().accept(t.get1());
  }
}
