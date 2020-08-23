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
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;

@HigherKind(sealed = true)
public interface Schedule<R, A, B> extends ScheduleOf<R, A, B> {

  <C> Schedule<R, A, C> map(Function1<B, C> mapper);

  <C> Schedule<R, C, B> contramap(Function1<C, A> comap);

  default <C, D> Schedule<R, C, D> dimap(Function1<C, A> comap, Function1<B, D> map) {
    return contramap(comap).map(map);
  }

  default <C> Schedule<R, A, C> as(C value) {
    return map(ignore -> value);
  }

  default Schedule<R, A, Unit> unit() {
    return as(Unit.unit());
  }

  default Schedule<R, A, B> andThen(Schedule<R, A, B> next) {
    return andThenEither(next).map(Either::merge);
  }

  <C> Schedule<R, A, Either<B, C>> andThenEither(Schedule<R, A, C> next);

  <C> Schedule<R, A, Tuple2<B, C>> zip(Schedule<R, A, C> other);

  default <C> Schedule<R, A, B> zipLeft(Schedule<R, A, C> other) {
    return zip(other).map(Tuple2::get1);
  }

  default <C> Schedule<R, A, C> zipRight(Schedule<R, A, C> other) {
    return zip(other).map(Tuple2::get2);
  }

  <C> Schedule<R, A, C> compose(Schedule<R, B, C> other);

  default Schedule<R, A, Sequence<B>> collectAll() {
    return this.<Sequence<B>>fold(ImmutableList.<B>empty(), Sequence::append);
  }

  default <Z> Schedule<R, A, Z> fold(Z zero, Function2<Z, B, Z> next) {
    return foldM(zero, (z, b) -> ZIO.pure(next.apply(z, b)));
  }

  <Z> Schedule<R, A, Z> foldM(Z zero, Function2<Z, B, ZIO<R, Unit, Z>> next);

  default Schedule<R, A, B> addDelay(Function1<B, Duration> map) {
    return addDelayM(map.andThen(URIO::pure));
  }

  Schedule<R, A, B> addDelayM(Function1<B, URIO<R, Duration>> map);

  default Schedule<R, A, B> whileInput(Matcher1<A> condition) {
    return whileInputM(condition.asFunction().andThen(UIO::pure));
  }

  default Schedule<R, A, B> whileInputM(Function1<A, UIO<Boolean>> condition) {
    return check((a, b) -> condition.apply(a));
  }

  default Schedule<R, A, B> whileOutput(Matcher1<B> condition) {
    return whileOutputM(condition.asFunction().andThen(UIO::pure));
  }

  default Schedule<R, A, B> whileOutputM(Function1<B, UIO<Boolean>> condition) {
    return check((a, b) -> condition.apply(b));
  }

  default Schedule<R, A, B> untilInput(Matcher1<A> condition) {
    return untilInputM(condition.asFunction().andThen(UIO::pure));
  }

  Schedule<R, A, B> untilInputM(Function1<A, UIO<Boolean>> condition);

  default Schedule<R, A, B> untilOutput(Matcher1<B> condition) {
    return untilOutputM(condition.asFunction().andThen(UIO::pure));
  }

  Schedule<R, A, B> untilOutputM(Function1<B, UIO<Boolean>> condition);

  Schedule<R, A, B> check(Function2<A, B, UIO<Boolean>> condition);

  public static <R, A> Schedule<R, A, Unit> once() {
    return Schedule.<R, A>recurs(1).unit();
  }

  static <R, A> Schedule<R, A, Integer> recurs(int times) {
    return Schedule.<R, A>forever().whileOutput(x -> x < times);
  }

  static <R, A> Schedule<R, A, Integer> spaced(Duration delay) {
    return Schedule.<R, A>forever().addDelay(cons(delay));
  }

  static <R, A> Schedule<R, A, Duration> linear(Duration delay) {
    return delayed(Schedule.<R, A>forever().map(i -> delay.multipliedBy(i + 1)));
  }

  static <R, A> Schedule<R, A, Duration> exponential(Duration delay) {
    return exponential(delay, 2.0);
  }

  static <R, A> Schedule<R, A, Duration> exponential(Duration delay, double factor) {
    return delayed(Schedule.<R, A>forever().map(i -> delay.multipliedBy((long) Math.pow(factor, i.doubleValue()))));
  }

  static <R, A> Schedule<R, A, Duration> delayed(Schedule<R, A, Duration> schedule) {
    return schedule.addDelay(x -> x);
  }

  static <R, A> Schedule<R, A, Tuple2<Integer, Integer>> recursSpaced(Duration delay, int times) {
    return Schedule.<R, A>recurs(times).zip(Schedule.<R, A>spaced(delay));
  }

  static <R, A> Schedule<R, A, Unit> never() {
    return ScheduleImpl.of(
            URIO.unit(),
            (a, s) -> ZIO.<R, Unit, Unit>raiseError(Unit.unit()),
            (a, s) -> s);
  }

  static <R, A> Schedule<R, A, Integer> forever() {
    return unfold(0, a -> a + 1);
  }

  static <R, A, B> Schedule<R, A, B> succeed(B value) {
    return Schedule.<R, A>forever().as(value);
  }

  static <R, A> Schedule<R, A, A> identity() {
    return ScheduleImpl.of(URIO.unit(),
            (a, s) -> ZIO.unit(),
            (a, s) -> a);
  }

  static <R, A> Schedule<R, A, A> doWhile(Matcher1<A> condition) {
    return doWhileM(condition.asFunction().andThen(UIO::pure));
  }

  static <R, A> Schedule<R, A, A> doWhileM(Function1<A, UIO<Boolean>> condition) {
    return Schedule.<R, A>identity().whileInputM(condition);
  }

  static <R, A> Schedule<R, A, A> doUntil(Matcher1<A> condition) {
    return doUntilM(condition.asFunction().andThen(UIO::pure));
  }

  static <R, A> Schedule<R, A, A> doUntilM(Function1<A, UIO<Boolean>> condition) {
    return Schedule.<R, A>identity().untilInputM(condition);
  }

  static <R, A, B> Schedule<R, A, B> unfold(B initial, Operator1<B> next) {
    return unfoldM(URIO.pure(initial), next.andThen(ZIO::pure));
  }

  static <R, A, B> Schedule<R, A, B> unfoldM(
          URIO<R, B> initial, Function1<B, ZIO<R, Unit, B>> next) {
    return ScheduleImpl.<R, B, A, B>of(initial, (a, s) -> next.apply(s), (a, s) -> s);
  }

  @FunctionalInterface
  interface Update<R, S, A> {

    ZIO<R, Unit, S> update(A last, S state);

  }

  @FunctionalInterface
  interface Extract<A, S, B> {

    B extract(A last, S state);

  }
}

abstract class ScheduleImpl<R, S, A, B> implements SealedSchedule<R, A, B>, Schedule.Update<R, S, A>, Schedule.Extract<A, S, B> {
  
  private ScheduleImpl() { }
  
  public abstract URIO<R, S> initial();

  @Override
  public <C> Schedule<R, A, C> map(Function1<B, C> mapper) {
    return ScheduleImpl.of(
      initial(), 
      this::update, 
      (a, s) -> mapper.apply(extract(a, s)));
  }

  @Override
  public <C> Schedule<R, C, B> contramap(Function1<C, A> comap) {
    return ScheduleImpl.of(
      initial(), 
      (c, s) -> update(comap.apply(c), s), 
      (c, s) -> extract(comap.apply(c), s));
  }

  public Schedule<R, A, B> andThen(Schedule<R, A, B> next) {
    return andThenEither(next).map(Either::merge);
  }

  @SuppressWarnings("unchecked")
  public <C> Schedule<R, A, Either<B, C>> andThenEither(Schedule<R, A, C> next) {
    return _andThenEither((ScheduleImpl<R, ?, A, C>) next);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <C> Schedule<R, A, Tuple2<B, C>> zip(Schedule<R, A, C> other) {
    return _zip((ScheduleImpl<R, ?, A, C>) other);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <C> Schedule<R, A, C> compose(Schedule<R, B, C> other) {
    return _compose((ScheduleImpl<R, ?, B, C>) other);
  }

  @Override
  public <Z> Schedule<R, A, Z> foldM(Z zero, Function2<Z, B, ZIO<R, Unit, Z>> next) {
    return ScheduleImpl.of(
      initial().map(s -> Tuple.of(s, zero)), 
      (a, sz) -> {
        ZIO<R, Unit, S> update = update(a, sz.get1());
        ZIO<R, Unit, Z> other = next.apply(sz.get2(), extract(a, sz.get1()));
        return update.zip(other);
      }, 
      (a, sz) -> sz.get2());
  }

  @Override
  public Schedule<R, A, B> addDelayM(Function1<B, URIO<R, Duration>> map) {
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

  @Override
  public Schedule<R, A, B> untilInputM(Function1<A, UIO<Boolean>> condition) {
    return updated(update -> (a, s) -> {
      UIO<Boolean> apply = condition.apply(a);
      return apply.<R, Unit>toZIO()
              .flatMap(test -> test ? ZIO.raiseError(Unit.unit()) : update(a, s));
    });
  }

  @Override
  public Schedule<R, A, B> untilOutputM(Function1<B, UIO<Boolean>> condition) {
    return updated(update -> (a, s) -> {
      UIO<Boolean> apply = condition.apply(extract(a, s));
      return apply.<R, Unit>toZIO()
        .flatMap(test -> test ? ZIO.raiseError(Unit.unit()) : update(a, s));
    });
  }

  @Override
  public Schedule<R, A, B> check(Function2<A, B, UIO<Boolean>> condition) {
    return updated(update -> (a, s) -> {
      ZIO<R, Unit, Boolean> apply = condition.apply(a, this.extract(a, s)).toZIO();
      return apply.flatMap(result -> result != null && result ? update.update(a, s) : ZIO.raiseError(Unit.unit()));
    });
  }

  private <T, C> Schedule<R, A, Either<B, C>> _andThenEither(ScheduleImpl<R, T, A, C> next) {
    return ScheduleImpl.of(
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

  private <T, C> Schedule<R, A, Tuple2<B, C>> _zip(ScheduleImpl<R, T, A, C> other) {
    return ScheduleImpl.<R, Tuple2<S, T>, A, Tuple2<B, C>>of(
            this.initial().<Unit>toZIO().zip(other.initial().<Unit>toZIO()).toURIO(),
            (a, st) -> {
              ZIO<R, Unit, S> self = this.update(a, st.get1());
              ZIO<R, Unit, T> next = other.update(a, st.get2());
              return self.zip(next);
            },
            (a, st) -> Tuple.of(
                    this.extract(a, st.get1()),
                    other.extract(a, st.get2())));
  }

  private <T, C> Schedule<R, A, C> _compose(ScheduleImpl<R, T, B, C> other) {
    return ScheduleImpl.<R, Tuple2<S, T>, A, C>of(
            this.initial().<Unit>toZIO().zip(other.initial().<Unit>toZIO()).toURIO(),
            (a, st) -> {
              ZIO<R,Unit,S> self = this.update(a, st.get1());
              ZIO<R,Unit,T> next = other.update(this.extract(a, st.get1()), st.get2());
              return self.zip(next);
            },
            (a, st) -> other.extract(this.extract(a, st.get1()), st.get2()));
  }

  private ScheduleImpl<R, S, A, B> updated(Function1<Update<R, S, A>, Update<R, S, A>> update) {
    return ScheduleImpl.of(initial(), update.apply(this::update), this::extract);
  }
  
  public static <R, S, A, B> ScheduleImpl<R, S, A, B> of(
      URIO<R, S> initial, 
      Update<R, S, A> update,
      Extract<A, S, B> extract) {
    checkNonNull(initial);
    checkNonNull(update);
    checkNonNull(extract);
    return new ScheduleImpl<R, S, A, B>() {
      
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
}
