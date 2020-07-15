/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.time.Duration;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.type.Either;

public abstract class Schedule<R, S, A, B> {
  
  private Schedule() { }
  
  public abstract URIO<R, S> initial();

  public abstract B extract(A last, S state);
  
  public abstract ZIO<R, Unit, S> update(A last, S state);
  
  public <C> Schedule<R, S, A, C> map(Function1<B, C> mapper) {
    return Schedule.of(
      initial(), 
      this::update, 
      (a, s) -> mapper.apply(extract(a, s)));
  }
  
  public <C> Schedule<R, S, C, B> contramap(Function1<C, A> comap) {
    return Schedule.of(
      initial(), 
      (c, s) -> update(comap.apply(c), s), 
      (c, s) -> extract(comap.apply(c), s));
  }
  
  public <C, D> Schedule<R, S, C, D> dimap(Function1<C, A> comap, Function1<B, D> map) {
    return contramap(comap).map(map);
  }
  
  public <C> Schedule<R, S, A, C> as(C value) {
    return map(ignore -> value);
  }
  
  public Schedule<R, S, A, Unit> unit() {
    return as(Unit.unit());
  }

  public <T> Schedule<R, Either<S, T>, A, B> andThen(Schedule<R, T, A, B> next) {
    return andThenEither(next).map(Either::merge);
  }

  public <T, C> Schedule<R, Either<S, T>, A, Either<B, C>> andThenEither(Schedule<R, T, A, C> next) {
    return Schedule.of(
        initial().map(Either::<S, T>left), 
        (a, st) -> st.fold(
            s -> {
              ZIO<R, Unit, Either<S, T>> orElse = 
                  next.initial().<Unit>toZIO().flatMap(c -> next.update(a, c).map(Either::<S, T>right));
              return this.update(a, s).map(Either::<S, T>left).orElse(orElse);
            }, 
            t -> next.update(a, t).map(Either::<S, T>right)),
        (a, st) -> st.fold(
            s -> Either.left(this.extract(a, s)), 
            t -> Either.right(next.extract(a, t))));
  }
  
  public <T, C> Schedule<R, Tuple2<S, T>, A, Tuple2<B, C>> zip(Schedule<R, T, A, C> other) {
    return Schedule.<R, Tuple2<S, T>, A, Tuple2<B, C>>of(
      this.initial().<Unit>toZIO().zip(other.initial().<Unit>toZIO()).orDie(), 
      (a, st) -> {
        ZIO<R, Unit, S> self = this.update(a, st.get1());
        ZIO<R, Unit, T> next = other.update(a, st.get2());
        return self.zip(next);
      }, 
      (a, st) -> Tuple.of(
        this.extract(a, st.get1()), 
        other.extract(a, st.get2())));
  }

  public <T, C> Schedule<R, Tuple2<S, T>, A, B> zipLeft(Schedule<R, T, A, C> other) {
    return zip(other).map(Tuple2::get1);
  }

  public <T, C> Schedule<R, Tuple2<S, T>, A, C> zipRight(Schedule<R, T, A, C> other) {
    return zip(other).map(Tuple2::get2);
  }
  
  public <T, C> Schedule<R, Tuple2<S, T>, A, C> compose(Schedule<R, T, B, C> other) {
    return Schedule.<R, Tuple2<S, T>, A, C>of(
      this.initial().<Unit>toZIO().zip(other.initial().<Unit>toZIO()).orDie(), 
      (a, st) -> {
        ZIO<R,Unit,S> self = this.update(a, st.get1());
        ZIO<R,Unit,T> next = other.update(this.extract(a, st.get1()), st.get2());
        return self.zip(next);
      }, 
      (a, st) -> other.extract(this.extract(a, st.get1()), st.get2()));
  }
  
  public Schedule<R, Tuple2<S, ImmutableList<B>>, A, ImmutableList<B>> collect() {
    return fold(ImmutableList.<B>empty(), (list, b) -> list.append(b));
  }

  public <Z> Schedule<R, Tuple2<S, Z>, A, Z> fold(Z zero, Function2<Z, B, Z> next) {
    return foldM(zero, (z, b) -> ZIO.pure(next.apply(z, b)));
  }
  
  public <Z> Schedule<R, Tuple2<S, Z>, A, Z> foldM(Z zero, Function2<Z, B, ZIO<R, Unit, Z>> next) {
    return Schedule.of(
      initial().map(s -> Tuple.of(s, zero)), 
      (a, sz) -> {
        ZIO<R, Unit, S> update = update(a, sz.get1());
        ZIO<R, Unit, Z> other = next.apply(sz.get2(), extract(a, sz.get1()));
        return update.zip(other);
      }, 
      (a, sz) -> sz.get2());
  }
  
  public Schedule<R, S, A, B> addDelay(Function1<B, Duration> map) {
    return addDelayM(map.andThen(URIO::pure));
  }
  
  public Schedule<R, S, A, B> addDelayM(Function1<B, URIO<R, Duration>> map) {
    return updated(update -> (a, s) -> {
      ZIO<R, Unit, Tuple2<Duration, S>> map2 = 
        ZIO.map2(
          map.apply(extract(a, s)).toZIO(), 
          update.update(a, s), 
          Tuple::of);
      
      return map2.flatMap(ds -> {
        ZIO<R, Unit, Unit> sleep = URIO.<R>sleep(ds.get1()).toZIO();
        return sleep.map(ignore -> ds.get2());
      });
    });
  }
  
  public Schedule<R, S, A, B> whileInput(Matcher1<A> condition) {
    return whileInputM(condition.asFunction().andThen(UIO::pure));
  }
  
  public Schedule<R, S, A, B> whileInputM(Function1<A, UIO<Boolean>> condition) {
    return check((a, b) -> condition.apply(a));
  }
  
  public Schedule<R, S, A, B> whileOutput(Matcher1<B> condition) {
    return whileOutputM(condition.asFunction().andThen(UIO::pure));
  }
  
  public Schedule<R, S, A, B> whileOutputM(Function1<B, UIO<Boolean>> condition) {
    return check((a, b) -> condition.apply(b));
  }
  
  public Schedule<R, S, A, B> untilInput(Matcher1<A> condition) {
    return untilInputM(condition.asFunction().andThen(UIO::pure));
  }
  
  public Schedule<R, S, A, B> untilInputM(Function1<A, UIO<Boolean>> condition) {
    return updated(update -> (a, s) -> {
      UIO<Boolean> apply = condition.apply(a);
      return apply.<R, Unit>toZIO()
        .flatMap(test -> test ? ZIO.raiseError(Unit.unit()) : update(a, s));
    });
  }
  
  public Schedule<R, S, A, B> untilOutput(Matcher1<B> condition) {
    return untilOutputM(condition.asFunction().andThen(UIO::pure));
  }
  
  public Schedule<R, S, A, B> untilOutputM(Function1<B, UIO<Boolean>> condition) {
    return updated(update -> (a, s) -> {
      UIO<Boolean> apply = condition.apply(extract(a, s));
      return apply.<R, Unit>toZIO()
        .flatMap(test -> test ? ZIO.raiseError(Unit.unit()) : update(a, s));
    });
  }
  
  public Schedule<R, S, A, B> check(Function2<A, B, UIO<Boolean>> condition) {
    return updated(update -> (a, s) -> {
      ZIO<R, Unit, Boolean> apply = condition.apply(a, this.extract(a, s)).toZIO();
      return apply.flatMap(result -> result != null && result ? update.update(a, s) : ZIO.raiseError(Unit.unit()));
    });
  }
  
  private Schedule<R, S, A, B> updated(Function1<Update<R, S, A>, Update<R, S, A>> update) {
    return Schedule.of(initial(), update.apply(this::update), this::extract);
  }
  
  public static <R, S, A, B> Schedule<R, S, A, B> of(
      URIO<R, S> initial, 
      Update<R, S, A> update,
      Extract<A, S, B> extract) {
    checkNonNull(initial);
    checkNonNull(update);
    checkNonNull(extract);
    return new Schedule<R, S, A, B>() {
      
      @Override
      public URIO<R, S> initial() {
        return initial;
      }
      
      @Override
      public ZIO<R, Unit, S> update(A last, S state) {
        return update.update(last, state);
      }
      
      @Override
      public B extract(A last, S state) {
        return extract.extract(last, state);
      }
    };
  }
  
  public static <R, A> Schedule<R, Integer, A, Unit> once() {
    return Schedule.<R, A>recurs(1).unit();
  }
  
  public static <R, A> Schedule<R, Integer, A, Integer> recurs(int times) {
    return Schedule.<R, A>forever().whileOutput(x -> x < times);
  }
  
  public static <R, A> Schedule<R, Integer, A, Integer> spaced(Duration delay) {
    return Schedule.<R, A>forever().addDelay(cons(delay));
  }
  
  public static <R, A> Schedule<R, Integer, A, Duration> linear(Duration delay) {
    return delayed(Schedule.<R, A>forever().map(i -> delay.multipliedBy(i + 1)));
  }
  
  public static <R, A> Schedule<R, Integer, A, Duration> exponential(Duration delay) {
    return exponential(delay, 2.0);
  }
  
  public static <R, A> Schedule<R, Integer, A, Duration> exponential(Duration delay, double factor) {
    return delayed(Schedule.<R, A>forever().map(i -> delay.multipliedBy((long) Math.pow(factor, i.doubleValue()))));
  }
  
  public static <R, A> Schedule<R, Integer, A, Duration> delayed(Schedule<R, Integer, A, Duration> schedule) {
    return schedule.addDelay(x -> x);
  }
  
  public static <R, A> Schedule<R, Tuple2<Integer, Integer>, A, Tuple2<Integer, Integer>> recursSpaced(Duration delay, int times) {
    return Schedule.<R, A>recurs(times).zip(Schedule.<R, A>spaced(delay));
  }
  
  public static <R, A> Schedule<R, Unit, A, Unit> never() {
    return Schedule.of(
      URIO.unit(), 
      (a, s) -> ZIO.<R, Unit, Unit>raiseError(Unit.unit()), 
      (a, s) -> s);
  }
  
  public static <R, A> Schedule<R, Integer, A, Integer> forever() {
    return unfold(0, a -> a + 1);
  }
  
  public static <R, A, B> Schedule<R, Integer, A, B> succeed(B value) {
    return Schedule.<R, A>forever().as(value);
  }
  
  public static <R, A> Schedule<R, Unit, A, A> identity() {
    return Schedule.of(URIO.unit(), 
      (a, s) -> ZIO.unit(), 
      (a, s) -> a);
  }
  
  public static <R, A> Schedule<R, Unit, A, A> doWhile(Matcher1<A> condition) {
    return doWhileM(condition.asFunction().andThen(UIO::pure));
  }
  
  public static <R, A> Schedule<R, Unit, A, A> doWhileM(Function1<A, UIO<Boolean>> condition) {
    return Schedule.<R, A>identity().whileInputM(condition);
  }
  
  public static <R, A> Schedule<R, Unit, A, A> doUntil(Matcher1<A> condition) {
    return doWhileM(condition.asFunction().andThen(UIO::pure));
  }
  
  public static <R, A> Schedule<R, Unit, A, A> doUntilM(Function1<A, UIO<Boolean>> condition) {
    return Schedule.<R, A>identity().untilInputM(condition);
  }
  
  public static <R, A, B> Schedule<R, B, A, B> unfold(B initial, Operator1<B> next) {
    return unfoldM(URIO.pure(initial), next.andThen(ZIO::pure));
  }
  
  public static <R, A, B> Schedule<R, B, A, B> unfoldM(
      URIO<R, B> initial, Function1<B, ZIO<R, Unit, B>> next) {
    return Schedule.<R, B, A, B>of(initial, (a, s) -> next.apply(s), (a, s) -> s);
  }
  
  @FunctionalInterface
  public static interface Update<R, S, A> {

    ZIO<R, Unit, S> update(A last, S state);

  }
  
  @FunctionalInterface
  public static interface Extract<A, S, B> {

    B extract(A last, S state);

  }
}
