/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Consumer1.noop;
import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.time.Duration;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Either;

@HigherKind
public final class Managed<R, E, A> implements ManagedOf<R, E, A> {

  private final ZIO<R, E, Tuple2<A, Consumer1<? super A>>> resource;

  protected Managed(ZIO<R, E, Tuple2<A, Consumer1<? super A>>> resource) {
    this.resource = checkNonNull(resource);
  }
  
  public <B> Managed<R, E, B> map(Function1<? super A, ? extends B> mapper) {
    return flatMap(a -> pure(ZIO.pure(mapper.apply(a))));
  }
  
  public <F> Managed<R, F, A> mapError(Function1<? super E, ? extends F> mapper) {
    return new Managed<>(resource.mapError(mapper));
  }
  
  public <B> Managed<R, E, B> flatMap(Function1<? super A, ? extends Managed<R, E, ? extends B>> mapper) {
    ZIO<R, E, Tuple2<B, Consumer1<? super B>>> result = resource.flatMap(t -> {
      Managed<R, E, B> apply = ManagedOf.narrowK(mapper.apply(t.get1()));
      return apply.resource.map(r -> r.map2(ignore -> releaseAndThen(t, r)));
    });
    return new Managed<>(result);
  }

  public <F> Managed<R, F, A> flatMapError(Function1<? super E, ? extends Managed<R, F, ? extends A>> mapper) {
    return new Managed<>(resource.flatMapError(e -> ManagedOf.<R, F, A>narrowK(mapper.apply(e)).resource));
  }
  
  public <B> Managed<R, E, B> andThen(Managed<A, E, B> other) {
    ZIO<R, E, Tuple2<B, Consumer1<? super B>>> flatMap = resource.flatMap(a -> {
      Either<E, Tuple2<B, Consumer1<? super B>>> next = other.resource.provide(a.get1());
      return ZIO.fromEither(() -> next.map(t -> t.map2(ignore -> releaseAndThen(a, t))));
    });
    return new Managed<>(flatMap);
  }
  
  public <B> ZIO<R, E, B> use(Function1<? super A, ? extends ZIO<R, E, ? extends B>> use) {
    return ZIO.bracket(resource, a -> use.apply(a.get1()), Managed::release);
  }
  
  public <B> Managed<R, Nothing, B> fold(
      Function1<? super E, ? extends B> mapError, Function1<? super A, ? extends B> mapper) {
    return foldM(
        mapError.andThen(Managed::<R, Nothing, B>pure),
        mapper.andThen(Managed::<R, Nothing, B>pure));
  }

  public Managed<R, Nothing, A> recover(Function1<? super E, ? extends A> mapError) {
    return fold(mapError, identity());
  }

  public Managed<R, E, A> orElse(Managed<R, E, ? extends A> other) {
    return foldM(Function1.cons(other), Function1.cons(this));
  }
  
  public <F, B> Managed<R, F, B> foldM(
      Function1<? super E, ? extends Managed<R, F, ? extends B>> mapError, 
      Function1<? super A, ? extends Managed<R, F, ? extends B>> mapper) {
    ZIO<R, F, Tuple2<B, Consumer1<? super B>>> foldM = 
        resource.foldM(
            error -> ManagedOf.<R, F, B>narrowK(mapError.apply(error)).resource,
            a -> ManagedOf.<R, F, B>narrowK(mapper.apply(a.get1())).resource.map(b -> b.map2(ignore -> releaseAndThen(a, b))));
    return new Managed<>(foldM);
  }
  
  public <B> Managed<R, E, Tuple2<A, B>> combine(Managed<R, E, B> other) {
    return new Managed<>(ZIO.bracket(resource,
        t -> ZIO.bracket(other.resource,
          r -> ZIO.pure(Tuple.of(Tuple.of(t.get1(), r.get1()), noop())), 
          Managed::release), 
        Managed::release));
  }
  
  public <B> Managed<R, E, Either<A, B>> either(Managed<R, E, B> other) {
    ZIO<R, E, Either<Tuple2<A, Consumer1<? super A>>, Tuple2<B, Consumer1<? super B>>>> foldM = 
        this.resource.foldM(
            error -> other.resource.map(Either::right), 
            success -> ZIO.pure(Either.left(success)));
    
    return new Managed<>(foldM.map(
        e -> e.fold(
            a -> a.map(Either::left, x -> either -> release(a)), 
            b -> b.map(Either::right, y -> either -> release(b)))
        ));
  }
  
  public Managed<R, E, A> retry() {
    return retry(1);
  }
  
  public Managed<R, E, A> retry(int maxRetries) {
    return retry(Schedule.recurs(maxRetries));
  }
  
  public Managed<R, E, A> retry(Duration delay) {
    return retry(delay, 1);
  }
  
  public Managed<R, E, A> retry(Duration delay, int maxRetries) {
    return retry(Schedule.recursSpaced(delay, maxRetries));
  }
  
  public <B> Managed<R, E, A> retry(Schedule<R, E, B> schedule) {
    return new Managed<>(resource.retry(schedule));
  }
  
  public Managed<R, E, Tuple2<Duration, A>> timed() {
    return new Managed<>(resource.timed().map(
        tt -> Tuple.of(Tuple.of(tt.get1(), tt.get2().get1()), t -> tt.get2().get2().accept(t.get2()))));
  }
  
  public static <R, E, A> Managed<R, E, A> pure(A resource) {
    return pure(ZIO.pure(resource));
  }
  
  public static <R, E, A> Managed<R, E, A> pure(ZIO<R, E, ? extends A> resource) {
    return from(resource, noop());
  }
  
  public static <R, E, A extends AutoCloseable> Managed<R, E, A> from(ZIO<R, E, ? extends A> resource) {
    return from(resource, AutoCloseable::close);
  }
  
  public static <R, E, A> Managed<R, E, A> from(ZIO<R, E, ? extends A> resource, Consumer1<? super A> release) {
    return new Managed<>(resource.map(a -> Tuple.of(a, release)));
  }
  
  public static <R, E, A extends AutoCloseable> Managed<R, E, A> from(Function1<? super R, ? extends A> mapper) {
    return from(mapper, AutoCloseable::close);
  }
  
  public static <R, E, A> Managed<R, E, A> from(Function1<? super R, ? extends A> mapper, Consumer1<? super A> release) {
    return new Managed<>(ZIO.<R, E, A>access(mapper).map(y -> Tuple.of(y, release)));
  }
  
  public static <R, E, A extends AutoCloseable> Managed<R, E, A> fromM(Function1<? super R, ? extends ZIO<R, E, ? extends A>> mapper) {
    return fromM(mapper, AutoCloseable::close);
  }
  
  public static <R, E, A> Managed<R, E, A> fromM(
      Function1<? super R, ? extends ZIO<R, E, ? extends A>> mapper, Consumer1<? super A> release) {
    return new Managed<>(ZIO.<R, E, A>accessM(mapper).map(y -> Tuple.of(y, release)));
  }

  private static <X, T, R> Consumer1<X> releaseAndThen(
      Tuple2<T, Consumer1<? super T>> outer, Tuple2<R, Consumer1<? super R>> inner) {
    return ignore -> {
      try {
        release(inner);
      } finally {
        release(outer);
      }
    };
  }

  private static <T> void release(Tuple2<T, Consumer1<? super T>> t) {
    t.get2().accept(t.get1());
  }
}
