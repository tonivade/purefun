/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Consumer1.noop;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Either;

public class ZManaged<R, A> {

  private final ZIO<R, Throwable, Tuple2<A, Consumer1<A>>> resource;

  protected ZManaged(ZIO<R, Throwable, Tuple2<A, Consumer1<A>>> resource) {
    this.resource = checkNonNull(resource);
  }
  
  public <B> ZManaged<R, B> map(Function1<A, B> mapper) {
    return flatMap(a -> pure(ZIO.pure(mapper.apply(a))));
  }
  
  public <B> ZManaged<R, B> flatMap(Function1<A, ZManaged<R, B>> mapper) {
    ZIO<R, Throwable, Tuple2<B, Consumer1<B>>> result = resource.flatMap(t -> {
      ZManaged<R, B> apply = mapper.apply(t.get1());
      return apply.resource.map( 
          r -> Tuple.of(r.get1(), ignore -> releaseAndThen(t, r)));
    });
    return new ZManaged<>(result);
  }
  
  public <B> ZManaged<R, B> andThen(ZManaged<A, B> other) {
    ZIO<R, Throwable, Tuple2<B, Consumer1<B>>> flatMap = resource.flatMap(a -> {
      Either<Throwable, Tuple2<B, Consumer1<B>>> next = other.resource.provide(a.get1());
      return ZIO.fromEither(() -> next.map(t -> t.map2(r -> ingore -> releaseAndThen(a, t))));
    });
    return new ZManaged<>(flatMap);
  }
  
  public <B> ZIO<R, Throwable, B> use(Function1<A, ZIO<R, Throwable, B>> use) {
    return ZIO.bracket(resource, a -> use.apply(a.get1()), ZManaged::release);
  }
  
  public <B> ZManaged<R, Tuple2<A, B>> combine(ZManaged<R, B> other) {
    return new ZManaged<>(ZIO.bracket(resource,
        t -> ZIO.bracket(other.resource,
          r -> ZIO.pure(Tuple.of(Tuple.of(t.get1(), r.get1()), noop())), 
          ZManaged::release), 
        ZManaged::release));
  }
  
  public <B> ZManaged<R, Either<A, B>> either(ZManaged<R, B> other) {
    ZIO<R, Throwable, Either<Tuple2<A, Consumer1<A>>, Tuple2<B, Consumer1<B>>>> foldM = 
        this.resource.foldM(
            error -> other.resource.map(Either::right), 
            success -> ZIO.pure(Either.left(success)));
    
    return new ZManaged<>(foldM.map(
        e -> e.fold(
            a -> Tuple.of(Either.left(a.get1()), ignore -> release(a)), 
            b -> Tuple.of(Either.right(b.get1()), ignore -> release(b)))
        ));
  }
  
  public static <R, A> ZManaged<R, A> pure(A resource) {
    return pure(ZIO.pure(resource));
  }
  
  public static <R, A> ZManaged<R, A> pure(ZIO<R, Throwable, A> resource) {
    return from(resource, noop());
  }
  
  public static <R, A extends AutoCloseable> ZManaged<R, A> from(ZIO<R, Throwable, A> resource) {
    return from(resource, AutoCloseable::close);
  }
  
  public static <R, A> ZManaged<R, A> from(ZIO<R, Throwable, A> resource, Consumer1<A> release) {
    return new ZManaged<>(resource.map(a -> Tuple.of(a, release)));
  }
  
  public static <R, A extends AutoCloseable> ZManaged<R, A> from(Function1<R, A> mapper) {
    return from(mapper, AutoCloseable::close);
  }
  
  public static <R, A> ZManaged<R, A> from(Function1<R, A> mapper, Consumer1<A> release) {
    return new ZManaged<>(RIO.<R, A>access(mapper).map(y -> Tuple.of(y, release)).toZIO());
  }
  
  public static <R, A extends AutoCloseable> ZManaged<R, A> fromM(Function1<R, ZIO<R, Throwable, A>> mapper) {
    return fromM(mapper, AutoCloseable::close);
  }
  
  public static <R, A> ZManaged<R, A> fromM(Function1<R, ZIO<R, Throwable, A>> mapper, Consumer1<A> release) {
    return new ZManaged<>(ZIO.<R, Throwable, A>accessM(mapper).map(y -> Tuple.of(y, release)));
  }

  private static <T, R> void releaseAndThen(
      Tuple2<T, Consumer1<T>> outter, Tuple2<R, Consumer1<R>> inner) {
    try {
      release(inner);
    } finally {
      release(outter);
    }
  }

  private static <T> void release(Tuple2<T, Consumer1<T>> t) {
    t.get2().accept(t.get1());
  }
}
