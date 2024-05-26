/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Function1.cons;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import java.time.Duration;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Kind2;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;

@HigherKind
public sealed interface Schedule<F, A, B> extends ScheduleOf<F, A, B>, Kind2<Schedule<F, ?, ?>, A, B> {

  static <F> Schedule.Of<F> of(MonadDefer<F> monad) {
    return () -> monad;
  }

  static <F> Schedule.Of<F> of(Class<F> type) {
    return of(Instances.monadDefer(type));
  }

  @SafeVarargs
  static <F> Schedule.Of<F> of(F...reified) {
    return of(Instances.monadDefer(reified));
  }

  MonadDefer<F> monadDefer();

  <C> Schedule<F, A, C> map(Function1<? super B, ? extends C> mapper);

  <C> Schedule<F, C, B> contramap(Function1<? super C, ? extends A> comap);

  default <C, D> Schedule<F, C, D> dimap(Function1<? super C, ? extends A> comap, Function1<? super B, ? extends D> map) {
    Schedule<F, C, B> contramap = contramap(comap);
    return contramap.map(map);
  }

  default <C> Schedule<F, A, C> as(C value) {
    return map(ignore -> value);
  }

  default Schedule<F, A, Unit> unit() {
    return as(Unit.unit());
  }

  default Schedule<F, A, B> andThen(Schedule<F, A, B> next) {
    return andThenEither(next).map(Either::merge);
  }

  <C> Schedule<F, A, Either<B, C>> andThenEither(Schedule<F, A, C> next);

  <C> Schedule<F, A, Tuple2<B, C>> zip(Schedule<F, A, C> other);

  default <C> Schedule<F, A, B> zipLeft(Schedule<F, A, C> other) {
    return zip(other).map(Tuple2::get1);
  }

  default <C> Schedule<F, A, C> zipRight(Schedule<F, A, C> other) {
    return zip(other).map(Tuple2::get2);
  }

  <C> Schedule<F, A, C> compose(Schedule<F, B, C> other);

  default Schedule<F, A, Sequence<B>> collectAll() {
    return this.fold(ImmutableList.empty(), Sequence::append);
  }

  default <Z> Schedule<F, A, Z> fold(Z zero, Function2<? super Z, ? super B, ? extends Z> next) {
    return foldM(zero, (z, b) -> monadDefer().pure(next.andThen(Either::<Unit, Z>right).apply(z, b)));
  }

  <Z> Schedule<F, A, Z> foldM(Z zero, Function2<? super Z, ? super B, ? extends Kind<F, Either<Unit, ? extends Z>>> next);

  default Schedule<F, A, B> addDelay(Function1<B, Duration> map) {
    return addDelayM(map.andThen(monadDefer()::pure));
  }

  Schedule<F, A, B> addDelayM(Function1<B, Kind<F, Duration>> map);

  default Schedule<F, A, B> whileInput(Matcher1<A> condition) {
    return whileInputM(condition.asFunction().andThen(monadDefer()::pure));
  }

  default Schedule<F, A, B> whileInputM(Function1<A, Kind<F, Boolean>> condition) {
    return check((a, b) -> condition.apply(a));
  }

  default Schedule<F, A, B> whileOutput(Matcher1<B> condition) {
    return whileOutputM(condition.asFunction().andThen(monadDefer()::pure));
  }

  default Schedule<F, A, B> whileOutputM(Function1<B, Kind<F, Boolean>> condition) {
    return check((a, b) -> condition.apply(b));
  }

  default Schedule<F, A, B> untilInput(Matcher1<A> condition) {
    return untilInputM(condition.asFunction().andThen(monadDefer()::pure));
  }

  Schedule<F, A, B> untilInputM(Function1<A, Kind<F, Boolean>> condition);

  default Schedule<F, A, B> untilOutput(Matcher1<B> condition) {
    return untilOutputM(condition.asFunction().andThen(monadDefer()::pure));
  }

  Schedule<F, A, B> untilOutputM(Function1<B, Kind<F, Boolean>> condition);

  Schedule<F, A, B> check(Function2<A, B, Kind<F, Boolean>> condition);

  @SafeVarargs
  static <F, A> Schedule<F, A, Unit> once(F...reified) {
    return of(reified).once();
  }

  @SafeVarargs
  static <F, A> Schedule<F, A, Integer> recurs(int times, F...reified) {
    return of(reified).recurs(times);
  }

  @SafeVarargs
  static <F, A> Schedule<F, A, Integer> spaced(Duration delay, F...reified) {
    return of(reified).spaced(delay);
  }

  @SafeVarargs
  static <F, A> Schedule<F, A, Duration> linear(Duration delay, F...reified) {
    return of(reified).linear(delay);
  }

  @SafeVarargs
  static <F, A> Schedule<F, A, Duration> exponential(Duration delay, F...reified) {
    return of(reified).exponential(delay);
  }

  @SafeVarargs
  static <F, A> Schedule<F, A, Duration> exponential(Duration delay, double factor, F...reified) {
    return of(reified).exponential(delay, factor);
  }

  @SafeVarargs
  static <F, A> Schedule<F, A, Duration> delayed(Schedule<F, A, Duration> schedule, F...reified) {
    return of(reified).delayed(schedule);
  }

  @SafeVarargs
  static <F, A> Schedule<F, A, Tuple2<Integer, Integer>> recursSpaced(Duration delay, int times, F...reified) {
    return of(reified).recursSpaced(delay, times);
  }

  @SafeVarargs
  static <F, A> Schedule<F, A, Unit> never(F...reified) {
    return of(reified).never();
  }

  @SafeVarargs
  static <F, A> Schedule<F, A, Integer> forever(F...reified) {
    return of(reified).forever();
  }

  @SafeVarargs
  static <F, A, B> Schedule<F, A, B> succeed(B value, F...reified) {
    return of(reified).succeed(value);
  }

  @SafeVarargs
  static <F, A> Schedule<F, A, A> identity(F...reified) {
    return of(reified).identity();
  }

  @SafeVarargs
  static <F, A> Schedule<F, A, A> doWhile(Matcher1<A> condition, F...reified) {
    return of(reified).doWhile(condition);
  }

  @SafeVarargs
  static <F, A> Schedule<F, A, A> doWhileM(Function1<A, Kind<F, Boolean>> condition, F...reified) {
    return of(reified).doWhileM(condition);
  }

  @SafeVarargs
  static <F, A> Schedule<F, A, A> doUntil(Matcher1<A> condition, F...reified) {
    return of(reified).doUntil(condition);
  }

  @SafeVarargs
  static <F, A> Schedule<F, A, A> doUntilM(Function1<A, Kind<F, Boolean>> condition, F...reified) {
    return of(reified).doUntilM(condition);
  }

  @SafeVarargs
  static <F, A, B> Schedule<F, A, B> unfold(B initial, Operator1<B> next, F...reified) {
    return of(reified).unfold(initial, next);
  }

  @SafeVarargs
  static <F, A, B> Schedule<F, A, B> unfoldM(
      Kind<F, B> initial, Function1<B, Kind<F, Either<Unit, B>>> next, F...reified) {
    return of(reified).unfoldM(initial, next);
  }

  interface Of<F> {

    MonadDefer<F> monad();

    default <A> Schedule<F, A, Unit> once() {
      return this.<A>recurs(1).unit();
    }

    default <A> Schedule<F, A, Integer> recurs(int times) {
      return this.<A>forever().whileOutput(x -> x < times);
    }

    default <A> Schedule<F, A, Integer> spaced(Duration delay) {
      return this.<A>forever().addDelay(cons(delay));
    }

    default <A> Schedule<F, A, Duration> linear(Duration delay) {
      return delayed(this.<A>forever().map(i -> delay.multipliedBy(i + 1L)));
    }

    default <A> Schedule<F, A, Duration> exponential(Duration delay) {
      return exponential(delay, 2.0);
    }

    default <A> Schedule<F, A, Duration> exponential(Duration delay, double factor) {
      return delayed(this.<A>forever().map(i -> delay.multipliedBy((long) Math.pow(factor, i.doubleValue()))));
    }

    default <A> Schedule<F, A, Duration> delayed(Schedule<F, A, Duration> schedule) {
      return schedule.addDelay(x -> x);
    }

    default <A> Schedule<F, A, Tuple2<Integer, Integer>> recursSpaced(Duration delay, int times) {
      return this.<A>recurs(times).zip(this.spaced(delay));
    }

    default <A> Schedule<F, A, Unit> never() {
      return ScheduleImpl.of(
          monad(),
          monad().pure(Unit.unit()),
          (a, s) -> monad().pure(Either.left(Unit.unit())),
          (a, s) -> s);
    }

    default <A> Schedule<F, A, Integer> forever() {
      return unfold(0, a -> a + 1);
    }

    default <A, B> Schedule<F, A, B> succeed(B value) {
      return this.<A>forever().as(value);
    }

    default <A> Schedule<F, A, A> identity() {
      return ScheduleImpl.of(
          monad(),
          monad().pure(Unit.unit()),
          (a, s) -> monad().pure(Either.right(Unit.unit())),
          (a, s) -> a);
    }

    default <A> Schedule<F, A, A> doWhile(Matcher1<A> condition) {
      return this.doWhileM(condition.asFunction().andThen(monad()::pure));
    }

    default <A> Schedule<F, A, A> doWhileM(Function1<A, Kind<F, Boolean>> condition) {
      return this.<A>identity().whileInputM(condition);
    }

    default <A> Schedule<F, A, A> doUntil(Matcher1<A> condition) {
      return doUntilM(condition.asFunction().andThen(monad()::pure));
    }

    default <A> Schedule<F, A, A> doUntilM(Function1<A, Kind<F, Boolean>> condition) {
      return this.<A>identity().untilInputM(condition);
    }

    default <A, B> Schedule<F, A, B> unfold(B initial, Operator1<B> next) {
      return unfoldM(monad().pure(initial), next.andThen(Either::<Unit, B>right).andThen(monad()::pure));
    }

    default <A, B> Schedule<F, A, B> unfoldM(
        Kind<F, B> initial, Function1<B, Kind<F, Either<Unit, B>>> next) {
      return ScheduleImpl.of(monad(), initial, (a, s) -> next.apply(s), (a, s) -> s);
    }
  }

  @FunctionalInterface
  interface Update<F, S, A> {

    Kind<F, Either<Unit, S>> update(A last, S state);

  }

  @FunctionalInterface
  interface Extract<A, S, B> {

    B extract(A last, S state);

  }
}

final class ScheduleImpl<F, S, A, B> implements Schedule<F, A, B>, Schedule.Update<F, S, A>, Schedule.Extract<A, S, B> {

  private final MonadDefer<F> monad;
  private final Kind<F, S> initial;
  private final Update<F, S, A> update;
  private final Extract<A, S, B> extract;

  private ScheduleImpl(MonadDefer<F> monad, Kind<F, S> initial, Update<F, S, A> update, Extract<A, S, B> extract) {
    this.monad = checkNonNull(monad);
    this.initial = checkNonNull(initial);
    this.update = checkNonNull(update);
    this.extract = checkNonNull(extract);
  }

  public Kind<F, S> initial() {
    return initial;
  }

  @Override
  public Kind<F, Either<Unit, S>> update(A last, S state) {
    return update.update(last, state);
  }

  @Override
  public B extract(A last, S state) {
    return extract.extract(last, state);
  }

  @Override
  public MonadDefer<F> monadDefer() {
    return monad;
  }

  @Override
  public <C> Schedule<F, A, C> map(Function1<? super B, ? extends C> mapper) {
    return ScheduleImpl.of(
      monad,
      initial,
      update,
      (a, s) -> mapper.apply(extract(a, s)));
  }

  @Override
  public <C> Schedule<F, C, B> contramap(Function1<? super C, ? extends A> comap) {
    return ScheduleImpl.of(
      monad,
      initial,
      (c, s) -> update(comap.apply(c), s),
      (c, s) -> extract(comap.apply(c), s));
  }

  @Override
  public Schedule<F, A, B> andThen(Schedule<F, A, B> next) {
    return andThenEither(next).map(Either::merge);
  }

  @Override
  public <C> Schedule<F, A, Either<B, C>> andThenEither(Schedule<F, A, C> next) {
    return doAndThenEither((ScheduleImpl<F, ?, A, C>) next);
  }

  @Override
  public <C> Schedule<F, A, Tuple2<B, C>> zip(Schedule<F, A, C> other) {
    return doZip((ScheduleImpl<F, ?, A, C>) other);
  }

  @Override
  public <C> Schedule<F, A, C> compose(Schedule<F, B, C> other) {
    return doCompose((ScheduleImpl<F, ?, B, C>) other);
  }

  @Override
  public <Z> Schedule<F, A, Z> foldM(Z zero, Function2<? super Z, ? super B, ? extends Kind<F, Either<Unit, ? extends Z>>> next) {
    return ScheduleImpl.of(
      monad,
      monad.map(initial, s -> Tuple.of(s, zero)),
      (a, sz) -> {
        Kind<F, Either<Unit, S>> update = update(a, sz.get1());
        Kind<F, Either<Unit, ? extends Z>> other = next.apply(sz.get2(), extract(a, sz.get1()));
        return monad.mapN(update, other).apply((x, y) -> Either.<Unit, S, Z, Tuple2<S, Z>>map2(x, y, Tuple::of));
      },
      (a, sz) -> sz.get2());
  }

  @Override
  public Schedule<F, A, B> addDelayM(Function1<B, Kind<F, Duration>> map) {
    return updated(u -> (a, s) -> {
      Kind<F, Either<Unit, Tuple2<Duration, S>>> map2 =
        monad.mapN(map.apply(extract(a, s)), u.update(a, s)).apply(
              (duration, either) -> either.map(x -> Tuple.of(duration, x)));

      return monad.flatMap(map2, either -> {
        Kind<F, Unit> fold = either.fold(monad::pure, tuple -> monadDefer().sleep(tuple.get1()));
        return monad.map(fold, ignore -> either.map(Tuple2<Duration, S>::get2));
      });
    });
  }

  @Override
  public Schedule<F, A, B> untilInputM(Function1<A, Kind<F, Boolean>> condition) {
    return updated(u -> (a, s) -> {
      Kind<F, Boolean> apply = condition.apply(a);
      return monad.flatMap(apply, test -> test ? monad.pure(Either.left(Unit.unit())) : update(a, s));
    });
  }

  @Override
  public Schedule<F, A, B> untilOutputM(Function1<B, Kind<F, Boolean>> condition) {
    return updated(u -> (a, s) -> {
      Kind<F, Boolean> apply = condition.apply(extract(a, s));
      return monad.flatMap(apply, test -> test ? monad.pure(Either.<Unit, S>left(Unit.unit())) : update(a, s));
    });
  }

  @Override
  public Schedule<F, A, B> check(Function2<A, B, Kind<F, Boolean>> condition) {
    return updated(u -> (a, s) -> {
      Kind<F, Boolean> apply = condition.apply(a, extract(a, s));
      return monad.flatMap(apply, result -> result ? u.update(a, s) : monad.pure(Either.left(Unit.unit())));
    });
  }

  private <T, C> ScheduleImpl<F, Either<S, T>, A, Either<B, C>> doAndThenEither(ScheduleImpl<F, T, A, C> other) {
    return ScheduleImpl.<F, Either<S, T>, A, Either<B, C>>of(
        monad,
        monad.map(initial, Either::<S, T>left),
        (a, st) -> st.fold(
            s -> {
              Kind<F, Either<Unit, Either<S, T>>> orElse =
                  monad.flatMap(other.initial, t -> {
                    Kind<F, Either<Unit, T>> u = other.update(a, t);
                    return monad.map(u, e -> e.map(Either::<S, T>right));
                  });
              Kind<F, Either<Unit, Either<S, T>>> map =
                  monad.map(this.update(a, s), e -> e.map(Either::<S, T>left));
              return monad.mapN(map, orElse).apply(Either<Unit, Either<S, T>>::orElse);
            },
            t -> monad.map(other.update(a, t), e -> e.map(Either::<S, T>right))),
        (a, st) -> st.fold(
            s -> Either.left(this.extract(a, s)),
            t -> Either.right(other.extract(a, t))));
  }

  private <T, C> ScheduleImpl<F, Tuple2<S, T>, A, Tuple2<B, C>> doZip(ScheduleImpl<F, T, A, C> other) {
    return ScheduleImpl.<F, Tuple2<S, T>, A, Tuple2<B, C>>of(
        monad,
        monad.tuple(this.initial, other.initial),
        (a, st) -> {
          Kind<F, Either<Unit, S>> self = this.update(a, st.get1());
          Kind<F, Either<Unit, T>> next = other.update(a, st.get2());
          return monad.mapN(self, next).apply((x, y) -> Either.map2(x, y, Tuple::of));
        },
        (a, st) -> Tuple.of(
            this.extract(a, st.get1()),
            other.extract(a, st.get2())));
  }

  private <T, C> ScheduleImpl<F, Tuple2<S, T>, A, C> doCompose(ScheduleImpl<F, T, B, C> other) {
    return ScheduleImpl.<F, Tuple2<S, T>, A, C>of(
        monad,
        monad.tuple(this.initial, other.initial),
        (a, st) -> {
          Kind<F, Either<Unit, S>> self = this.update(a, st.get1());
          Kind<F, Either<Unit, T>> next = other.update(this.extract(a, st.get1()), st.get2());
          return monad.mapN(self, next).apply((x, y) -> Either.map2(x, y, Tuple::of));
        },
        (a, st) -> other.extract(this.extract(a, st.get1()), st.get2()));
  }

  private ScheduleImpl<F, S, A, B> updated(Function1<Update<F, S, A>, Update<F, S, A>> update) {
    return ScheduleImpl.of(monad, initial, update.apply(this.update), this.extract);
  }

  public static <F, S, A, B> ScheduleImpl<F, S, A, B> of(
      MonadDefer<F> monad,
      Kind<F, S> initial,
      Update<F, S, A> update,
      Extract<A, S, B> extract) {
    return new ScheduleImpl<>(monad, initial, update, extract);
  }
}
