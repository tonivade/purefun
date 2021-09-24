/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Consumer1.noop;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Witness;

@HigherKind
public final class Resource<F extends Witness, T> implements ResourceOf<F, T> {
  
  private final MonadDefer<F> monad;
  private final Kind<F, Tuple2<T, Consumer1<? super T>>> resource;
  
  Resource(MonadDefer<F> monad, Kind<F, Tuple2<T, Consumer1<? super T>>> resource) {
    this.monad = checkNonNull(monad);
    this.resource = checkNonNull(resource);
  }
  
  public <R> Resource<F, R> map(Function1<? super T, ? extends R> mapper) {
    return flatMap(t -> pure(monad, mapper.andThen(monad::<R>pure).apply(t)));
  }
  
  public <R> Resource<F, R> flatMap(Function1<? super T, ? extends Resource<F, ? extends R>> mapper) {
    return new Resource<>(monad, monad.flatMap(resource, 
        t -> monad.map(mapper.andThen(ResourceOf::narrowK).apply(t.get1()).resource, 
            r -> Tuple.of(r.get1(), (Consumer1<R>) ignore -> releaseAndThen(t, r)))));
  }
  
  public <R> Kind<F, R> use(Function1<? super T, ? extends Kind<F, ? extends R>> use) {
    return monad.bracket(resource, t -> use.apply(t.get1()), release());
  }
  
  public <R> Resource<F, Tuple2<T, R>> combine(Resource<F, ? extends R> other) {
    return new Resource<>(monad, monad.bracket(resource, 
        t -> monad.bracket(other.resource, 
          r -> monad.pure(Tuple.of(Tuple.of(t.get1(), r.get1()), noop())), 
          release()), 
        release()));
  }
  
  public static <F extends Witness, T> Resource<F, T> pure(
      MonadDefer<F> monad, Kind<F, ? extends T> acquire) {
    return new Resource<>(monad, monad.map(acquire, t -> Tuple.of(t, noop())));
  }

  public static <F extends Witness, T> Resource<F, T> from(
      MonadDefer<F> monad, Kind<F, ? extends T> acquire, Consumer1<? super T> release) {
    return new Resource<>(monad, monad.map(acquire, t -> Tuple.of(t, release)));
  }

  public static <F extends Witness, T extends AutoCloseable> Resource<F, T> from(
      MonadDefer<F> monad, Kind<F, ? extends T> acquire) {
    return from(monad, acquire, AutoCloseable::close);
  }

  private static <T, R> void releaseAndThen(
      Tuple2<T, Consumer1<? super T>> outter, Tuple2<R, Consumer1<? super R>> inner) {
    try {
      Resource.<R>release().accept(inner);
    } finally {
      Resource.<T>release().accept(outter);
    }
  }

  private static <T> Consumer1<Tuple2<T, Consumer1<? super T>>> release() {
    return t -> t.get2().accept(t.get1());
  }
}
