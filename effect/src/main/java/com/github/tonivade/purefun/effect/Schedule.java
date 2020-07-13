/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Unit.unit;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Either;

public abstract class Schedule<R, S, A, B> {

  private final URIO<R, S> initial;
  
  protected Schedule(URIO<R, S> initial) {
    this.initial = checkNonNull(initial);
  }
  
  public URIO<R, S> initial() {
    return initial;
  }

  public abstract B extract(A last, S state);
  
  public abstract ZIO<R, Unit, S> update(A last, S state);
  
  public <C> Schedule<R, S, A, C> map(Function1<B, C> mapper) {
    return Schedule.of(initial, this::update, (a, s) -> mapper.apply(extract(a, s)));
  }

  public <T> Schedule<R, Either<S, T>, A, B> andThen(Schedule<R, T, A, B> next) {
    return andThenEither(next).map(Either::merge);
  }

  public <T, C> Schedule<R, Either<S, T>, A, Either<B, C>> andThenEither(Schedule<R, T, A, C> next) {
    return Schedule.of(
        initial.map(s -> Either.left(s)), 
        (a, st) -> st.fold(
            s -> {
              ZIO<R, Unit, Either<S, T>> orElse = 
                  next.initial().<Unit>toZIO().flatMap(c -> next.update(a, c).map(Either::<S, T>right));
              return this.update(a, s).map(Either::<S, T>left).orElse(orElse);
            }, 
            t -> next.update(a, t).map(Either::<S, T>right)),
        (A a, Either<S, T> st) -> st.fold(
            s -> Either.left(this.extract(a, s)), 
            t -> Either.right(next.extract(a, t)))
        );
  }

  public <Z> Schedule<R, Tuple2<S, Z>, A, Z> fold(Z zero, Function2<Z, B, Z> next) {
    return foldM(zero, (z, b) -> ZIO.pure(next.apply(z, b)));
  }
  
  public <Z> Schedule<R, Tuple2<S, Z>, A, Z> foldM(Z zero, Function2<Z, B, ZIO<R, Unit, Z>> next) {
    return Schedule.of(initial.map(s -> Tuple.of(s, zero)), (a, sz) -> {
      ZIO<R, Unit, S> update = update(a, sz.get1());
      ZIO<R, Unit, Z> apply = next.apply(sz.get2(), extract(a, sz.get1()));
      return ZIO.map2(update, apply, Tuple::of);
    }, (a, sz) -> sz.get2());
  }
  
  public Schedule<R, S, A, B> whileOutput(Matcher1<B> matcher) {
    return whileOutputM(matcher.asFunction().andThen(UIO::pure));
  }
  
  public Schedule<R, S, A, B> whileOutputM(Function1<B, UIO<Boolean>> matcher) {
    return check((ignore, b) -> matcher.apply(b));
  }
  
  public Schedule<R, S, A, B> check(Function2<A, B, UIO<Boolean>> test) {
    return updated(update -> (a, s) -> {
      ZIO<R, Unit, Boolean> apply = test.apply(a, this.extract(a, s)).toZIO();
      return apply.flatMap(result -> result != null && result ? update.update(a, s) : ZIO.raiseError(unit()));
    });
  }
  
  public Schedule<R, S, A, B> updated(Function1<Update<R, S, A>, Update<R, S, A>> update) {
    return Schedule.of(initial, update.apply(this::update)::update, this::extract);
  }
  
  public static <R, S, A, B> Schedule<R, S, A, B> of(
      URIO<R, S> initial, 
      Function2<A, S, ZIO<R, Unit, S>> update,
      Function2<A, S, B> extract) {
    return new Schedule<R, S, A, B>(initial) {
      
      @Override
      public ZIO<R, Unit, S> update(A last, S state) {
        return update.apply(last, state);
      }
      
      @Override
      public B extract(A last, S state) {
        return extract.apply(last, state);
      }
    };
  }
  
  public static <R, A> Schedule<R, Integer, A, Integer> recurs(int times) {
    return Schedule.<R, A>forever().whileOutput(x -> x < times);
  }
  
  public static <R, A> Schedule<R, Unit, A, Unit> never() {
    return Schedule.of(URIO.unit(), (a, s) -> ZIO.<R, Unit, Unit>raiseError(unit()), (a, never) -> never);
  }
  
  public static <R, A> Schedule<R, Integer, A, Integer> forever() {
    return unfold(0, a -> a + 1);
  }
  
  public static <R, A, B> Schedule<R, B, A, B> unfold(B initial, Operator1<B> next) {
    return unfoldM(URIO.pure(initial), next.andThen(ZIO::pure));
  }
  
  public static <R, A, B> Schedule<R, B, A, B> unfoldM(
      URIO<R, B> initial, Function1<B, ZIO<R, Unit, B>> next) {
    return Schedule.<R, B, A, B>of(initial, (a, s) -> next.apply(s), (a, s) -> s);
  }
  
  @FunctionalInterface
  interface Update<R, S, A> {

    ZIO<R, Unit, S> update(A las, S state);

  }
}
