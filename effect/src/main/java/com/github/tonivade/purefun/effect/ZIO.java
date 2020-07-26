/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Function2.first;
import static com.github.tonivade.purefun.Function2.second;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.effect.WrappedException.unwrap;
import static com.github.tonivade.purefun.effect.WrappedException.wrap;

import java.time.Duration;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@HigherKind(sealed = true)
public interface ZIO<R, E, A> extends ZIOOf<R, E, A> {

  Either<E, A> provide(R env);

  default Future<Either<E, A>> toFuture(R env) {
    return toFuture(Future.DEFAULT_EXECUTOR, env);
  }

  default Future<Either<E, A>> toFuture(Executor executor, R env) {
    return Future.async(executor, () -> provide(env));
  }

  default void provideAsync(R env, Consumer1<Try<Either<E, A>>> callback) {
    provideAsync(Future.DEFAULT_EXECUTOR, env, callback);
  }

  default void provideAsync(Executor executor, R env, Consumer1<Try<Either<E, A>>> callback) {
    toFuture(executor, env).onComplete(callback);
  }

  <F extends Witness> Kind<F, A> foldMap(R env, MonadDefer<F> monad);

  default ZIO<R, A, E> swap() {
    return new Swap<>(this);
  }

  default <B> ZIO<R, E, B> map(Function1<A, B> map) {
    return flatMap(map.andThen(ZIO::pure));
  }

  default <B> ZIO<R, B, A> mapError(Function1<E, B> map) {
    return flatMapError(map.andThen(ZIO::raiseError));
  }

  default <B, F> ZIO<R, F, B> bimap(Function1<E, F> mapError, Function1<A, B> map) {
    return biflatMap(mapError.andThen(ZIO::raiseError), map.andThen(ZIO::pure));
  }

  default <B> ZIO<R, E, B> flatMap(Function1<A, ZIO<R, E, B>> map) {
    return biflatMap(ZIO::<R, E, B>raiseError, map);
  }

  default <F> ZIO<R, F, A> flatMapError(Function1<E, ZIO<R, F, A>> map) {
    return biflatMap(map, ZIO::<R, F, A>pure);
  }

  default <F, B> ZIO<R, F, B> biflatMap(Function1<E, ZIO<R, F, B>> left, Function1<A, ZIO<R, F, B>> right) {
    return new FlatMapped<>(cons(this), left, right);
  }

  default <B> ZIO<R, E, B> andThen(ZIO<R, E, B> next) {
    return flatMap(ignore -> next);
  }

  default <F, B> ZIO<R, F, B> foldM(Function1<E, ZIO<R, F, B>> mapError, Function1<A, ZIO<R, F, B>> map) {
    return new FoldM<>(this, mapError, map);
  }

  default <B> ZIO<R, Nothing, B> fold(Function1<E, B> mapError, Function1<A, B> map) {
    return foldM(mapError.andThen(ZIO::pure), map.andThen(ZIO::pure));
  }

  default ZIO<R, Nothing, A> recover(Function1<E, A> mapError) {
    return fold(mapError, identity());
  }

  default ZIO<R, E, A> orElse(ZIO<R, E, A> other) {
    return foldM(Function1.cons(other), Function1.cons(this));
  }
  
  default <B> ZIO<R, E, Tuple2<A, B>> zip(ZIO<R, E, B> other) {
    return zipWith(other, Tuple::of);
  }
  
  default <B> ZIO<R, E, A> zipLeft(ZIO<R, E, B> other) {
    return zipWith(other, first());
  }
  
  default <B> ZIO<R, E, B> zipRight(ZIO<R, E, B> other) {
    return zipWith(other, second());
  }
  
  default <B, C> ZIO<R, E, C> zipWith(ZIO<R, E, B> other, Function2<A, B, C> mapper) {
    return map2(this, other, mapper);
  }

  default ZIO<R, E, A> repeat() {
    return repeat(1);
  }

  default ZIO<R, E, A> repeat(int times) {
    return repeat(Schedule.<R, A>recurs(times).zipRight(Schedule.identity()));
  }

  default ZIO<R, E, A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  default ZIO<R, E, A> repeat(Duration delay, int times) {
    return repeat(Schedule.<R, A>recursSpaced(delay, times).zipRight(Schedule.identity()));
  }
  
  default <S, B> ZIO<R, E, B> repeat(Schedule<R, S, A, B> schedule) {
    return repeatOrElse(schedule, (e, b) -> raiseError(e));
  }
  
  default <S, B> ZIO<R, E, B> repeatOrElse(
      Schedule<R, S, A, B> schedule, 
      Function2<E, Option<B>, ZIO<R, E, B>> orElse) {
    return repeatOrElseEither(schedule, orElse).map(Either::merge);
  }

  default <S, B, C> ZIO<R, E, Either<C, B>> repeatOrElseEither(
      Schedule<R, S, A, B> schedule, 
      Function2<E, Option<B>, ZIO<R, E, C>> orElse) {
    return new Repeat<>(this, schedule, orElse);
  }

  default ZIO<R, E, A> retry() {
    return retry(1);
  }

  default ZIO<R, E, A> retry(int maxRetries) {
    return retry(Schedule.recurs(maxRetries));
  }

  default ZIO<R, E, A> retry(Duration delay) {
    return retry(delay, 1);
  }

  default ZIO<R, E, A> retry(Duration delay, int maxRetries) {
    return retry(Schedule.<R, E>recursSpaced(delay, maxRetries));
  }
  
  default <S> ZIO<R, E, A> retry(Schedule<R, S, E, S> schedule) {
    return retryOrElse(schedule, (e, s) -> raiseError(e));
  }

  default <S> ZIO<R, E, A> retryOrElse(
      Schedule<R, S, E, S> schedule,
      Function2<E, S, ZIO<R, E, A>> orElse) {
    return retryOrElseEither(schedule, orElse).map(Either::merge);
  }

  default <S, B> ZIO<R, E, Either<B, A>> retryOrElseEither(
      Schedule<R, S, E, S> schedule,
      Function2<E, S, ZIO<R, E, B>> orElse) {
    return new Retry<>(this, schedule, orElse);
  }

  default ZIO<R, E, Tuple2<Duration, A>> timed() {
    return new Timed<>(this);
  }
  
  @SuppressWarnings("unchecked")
  default <X extends Throwable> ZIO<R, X, A> refineOrDie(Class<X> type) {
    return mapError(error -> {
      if (type.isAssignableFrom(error.getClass())) {
        return (X) error;
      }
      throw new ClassCastException(error.getClass() + " not asignable to " + type);
    });
  }
  
  default URIO<R, A> toURIO() {
    return new URIO<>(mapError(error -> {
      if (error instanceof Throwable) {
        throw (Throwable) error;
      }
      throw new ClassCastException(error.getClass() + " is not throwable");
    }));
  }
  
  default RIO<R, A> toRIO() {
    return new RIO<>(mapError(error -> {
      if (error instanceof Throwable) {
        return (Throwable) error;
      }
      throw new ClassCastException(error.getClass() + " is not throwable");
    }));
  }
  
  default ZManaged<R, E, A> toManaged() {
    return ZManaged.pure(this);
  }
  
  default ZManaged<R, E, A> toManaged(Consumer1<A> release) {
    return ZManaged.from(this, release);
  }

  static <R, E, A> ZIO<R, E, A> accessM(Function1<R, ZIO<R, E, A>> map) {
    return new AccessM<>(map);
  }

  static <R, E, A> ZIO<R, E, A> access(Function1<R, A> map) {
    return accessM(map.andThen(ZIO::pure));
  }

  static <R, E> ZIO<R, E, R> env() {
    return access(identity());
  }

  static <R, E, A, B, C> ZIO<R, E, C> map2(ZIO<R, E, A> za, ZIO<R, E, B> zb, Function2<A, B, C> mapper) {
    return za.flatMap(a -> zb.map(b -> mapper.curried().apply(a).apply(b)));
  }

  static <R, E, A> ZIO<R, E, A> absorb(ZIO<R, E, Either<E, A>> value) {
    return value.flatMap(either -> either.fold(ZIO::raiseError, ZIO::pure));
  }

  static <R, A, B> Function1<A, ZIO<R, Throwable, B>> lift(Function1<A, B> function) {
    return value -> task(() -> function.apply(value));
  }

  static <R, E, A> ZIO<R, E, A> fromEither(Producer<Either<E, A>> task) {
    return new Task<>(task);
  }

  static <R> ZIO<R, Throwable, Unit> exec(CheckedRunnable task) {
    return new Attempt<>(task.asProducer());
  }

  static <R, E, A> ZIO<R, E, A> pure(A value) {
    return new Pure<>(value);
  }

  static <R, E, A> ZIO<R, E, A> defer(Producer<ZIO<R, E, A>> lazy) {
    return new Suspend<>(lazy);
  }

  static <R, A> ZIO<R, Throwable, A> task(Producer<A> task) {
    return new Attempt<>(task);
  }

  static <R, E, A> ZIO<R, E, A> raiseError(E error) {
    return new Failure<>(error);
  }

  static <R, A> ZIO<R, Throwable, A> redeem(ZIO<R, Nothing, A> value) {
    return new Redeem<>(value);
  }

  static <R> ZIO<R, Throwable, Unit> sleep(Duration delay) {
    return new Sleep<>(delay);
  }

  static <R, E, A extends AutoCloseable, B> ZIO<R, E, B> bracket(ZIO<R, E, A> acquire,
                                                                 Function1<A, ZIO<R, E, B>> use) {
    return new Bracket<>(acquire, use, AutoCloseable::close);
  }

  static <R, E, A, B> ZIO<R, E, B> bracket(ZIO<R, E, A> acquire,
                                           Function1<A, ZIO<R, E, B>> use,
                                           Consumer1<A> release) {
    return new Bracket<>(acquire, use, release);
  }

  @SuppressWarnings("unchecked")
  static <R, E> ZIO<R, E, Unit> unit() {
    return (ZIO<R, E, Unit>) ZIOModule.UNIT;
  }

  final class Pure<R, E, A> implements SealedZIO<R, E, A> {

    private final A value;

    protected Pure(A value) {
      this.value = checkNonNull(value);
    }

    @Override
    public Either<E, A> provide(R env) {
      return Either.right(value);
    }

    @Override
    public <F extends Witness> Kind<F, A> foldMap(R env, MonadDefer<F> monad) {
      return monad.pure(value);
    }

    @Override
    public String toString() {
      return "Pure(" + value + ")";
    }
  }

  final class Failure<R, E, A> implements SealedZIO<R, E, A> {

    private final E error;

    protected Failure(E error) {
      this.error = checkNonNull(error);
    }

    @Override
    public Either<E, A> provide(R env) {
      return Either.left(error);
    }

    @Override
    public <F extends Witness> Kind<F, A> foldMap(R env, MonadDefer<F> monad) {
      return monad.raiseError(wrap(error));
    }

    @Override
    public String toString() {
      return "Failure(" + error + ")";
    }
  }

  final class FlatMapped<R, E, A, F, B> implements SealedZIO<R, F, B> {

    private final Producer<ZIO<R, E, A>> current;
    private final Function1<E, ZIO<R, F, B>> nextError;
    private final Function1<A, ZIO<R, F, B>> next;

    protected FlatMapped(Producer<ZIO<R, E, A>> current,
                         Function1<E, ZIO<R, F, B>> nextError,
                         Function1<A, ZIO<R, F, B>> next) {
      this.current = checkNonNull(current);
      this.nextError = checkNonNull(nextError);
      this.next = checkNonNull(next);
    }

    @Override
    public Either<F, B> provide(R env) {
      return ZIOModule.evaluate(env, this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <F1, B1> ZIO<R, F1, B1> biflatMap(Function1<F, ZIO<R, F1, B1>> left, Function1<B, ZIO<R, F1, B1>> right) {
      return new FlatMapped<>(
          () -> (ZIO<R, F, B>) start(),
          f -> new FlatMapped<>(
              () -> run((Either<E, A>) Either.left(f)), left::apply, right::apply),
          b -> new FlatMapped<>(
              () -> run((Either<E, A>) Either.right(b)), left::apply, right::apply)
      );
    }

    @Override
    public <X extends Witness> Kind<X, B> foldMap(R env, MonadDefer<X> monad) {
      Kind<X, A> foldMap = current.get().foldMap(env, monad);
      Kind<X, Either<Throwable, A>> attempt = monad.attempt(foldMap);
      Kind<X, ZIO<R, F, B>> map =
          monad.map(attempt, 
              either -> either.bimap(error -> nextError.apply(unwrap(error)), next).fold(identity(), identity()));
      return monad.flatMap(map, zio -> zio.foldMap(env, monad));
    }

    @Override
    public String toString() {
      return "FlatMapped(" + current + ", ?, ?)";
    }

    protected ZIO<R, E, A> start() {
      return current.get();
    }

    protected ZIO<R, F, B> run(Either<E, A> value) {
      return value.bimap(nextError, next).fold(identity(), identity());
    }
  }

  final class Task<R, E, A> implements SealedZIO<R, E, A> {

    private final Producer<Either<E, A>> task;

    protected Task(Producer<Either<E, A>> task) {
      this.task = checkNonNull(task);
    }

    @Override
    public Either<E, A> provide(R env) {
      return task.get();
    }

    @Override
    public <F extends Witness> Kind<F, A> foldMap(R env, MonadDefer<F> monad) {
      Kind<F, Either<E, A>> later = monad.later(task::get);
      return monad.flatMap(later, either -> either.fold(error -> monad.raiseError(wrap(error)), monad::<A>pure));
    }

    @Override
    public String toString() {
      return "Task(?)";
    }
  }

  final class Suspend<R, E, A> implements SealedZIO<R, E, A> {

    private final Producer<ZIO<R, E, A>> lazy;

    protected Suspend(Producer<ZIO<R, E, A>> lazy) {
      this.lazy = checkNonNull(lazy);
    }

    @Override
    public Either<E, A> provide(R env) {
      return ZIOModule.collapse(this).provide(env);
    }

    @Override
    public <B> ZIO<R, E, B> flatMap(Function1<A, ZIO<R, E, B>> map) {
      return new FlatMapped<>(lazy::get, ZIO::raiseError, map::apply);
    }

    @Override
    public <F extends Witness> Kind<F, A> foldMap(R env, MonadDefer<F> monad) {
      return monad.defer(() -> lazy.get().foldMap(env, monad));
    }

    protected ZIO<R, E, A> next() {
      return lazy.get();
    }

    @Override
    public String toString() {
      return "Suspend(?)";
    }
  }

  final class Swap<R, E, A> implements SealedZIO<R, A, E> {

    private final ZIO<R, E, A> current;

    protected Swap(ZIO<R, E, A> current) {
      this.current = checkNonNull(current);
    }

    @Override
    public Either<A, E> provide(R env) {
      return current.provide(env).swap();
    }

    @Override
    public <F extends Witness> Kind<F, E> foldMap(R env, MonadDefer<F> monad) {
      return monad.flatMap(monad.attempt(current.foldMap(env, monad)), 
          either -> either.fold(
              error -> monad.pure(unwrap(error)), value -> monad.raiseError(wrap(value))));
    }

    @Override
    public String toString() {
      return "Swap(" + current + ")";
    }
  }

  final class Attempt<R, A> implements SealedZIO<R, Throwable, A> {

    private final Producer<A> current;

    protected Attempt(Producer<A> current) {
      this.current = checkNonNull(current);
    }

    @Override
    public Either<Throwable, A> provide(R env) {
      return Try.of(current).toEither();
    }

    @Override
    public <F extends Witness> Kind<F, A> foldMap(R env, MonadDefer<F> monad) {
      return monad.later(current);
    }

    @Override
    public String toString() {
      return "Attempt(" + current + ")";
    }
  }

  final class Redeem<R, A> implements SealedZIO<R, Throwable, A> {

    private final ZIO<R, Nothing, A> current;

    protected Redeem(ZIO<R, Nothing, A> current) {
      this.current = checkNonNull(current);
    }

    @Override
    public Either<Throwable, A> provide(R env) {
      return Try.of(() -> current.provide(env).get()).toEither();
    }

    @Override
    public <F extends Witness> Kind<F, A> foldMap(R env, MonadDefer<F> monad) {
      return ZIO.redeem(current).foldMap(env, monad);
    }

    @Override
    public String toString() {
      return "Redeem(" + current + ")";
    }
  }

  final class AccessM<R, E, A> implements SealedZIO<R, E, A> {

    private final Function1<R, ZIO<R, E, A>> function;

    protected AccessM(Function1<R, ZIO<R, E, A>> function) {
      this.function = checkNonNull(function);
    }

    @Override
    public Either<E, A> provide(R env) {
      return function.apply(env).provide(env);
    }

    @Override
    public <F extends Witness> Kind<F, A> foldMap(R env, MonadDefer<F> monad) {
      Kind<F, ZIO<R, E, A>> later = monad.later(() -> function.apply(env));
      return monad.flatMap(later, zio -> zio.foldMap(env, monad));
    }

    @Override
    public String toString() {
      return "AccessM(?)";
    }
  }

  final class FoldM<R, E, A, F, B> implements SealedZIO<R, F, B> {

    private final ZIO<R, E, A> current;
    private final Function1<E, ZIO<R, F, B>> nextError;
    private final Function1<A, ZIO<R, F, B>> next;

    protected FoldM(ZIO<R, E, A> current, Function1<E, ZIO<R, F, B>> nextError, Function1<A, ZIO<R, F, B>> next) {
      this.current = checkNonNull(current);
      this.nextError = checkNonNull(nextError);
      this.next = checkNonNull(next);
    }

    @Override
    public Either<F, B> provide(R env) {
      return current.provide(env).fold(nextError, next).provide(env);
    }

    @Override
    public <X extends Witness> Kind<X, B> foldMap(R env, MonadDefer<X> monad) {
      Kind<X, A> foldMap = current.foldMap(env, monad);
      Kind<X, Either<Throwable, A>> attempt = monad.attempt(foldMap);
      Kind<X, ZIO<R, F, B>> map = monad.map(attempt, 
          either -> either.fold(error -> nextError.apply(unwrap(error)), next::apply));
      return monad.flatMap(map, zio -> zio.foldMap(env, monad));
    }

    @Override
    public String toString() {
      return "FoldM(" + current + ", ?, ?)";
    }
  }
  
  final class Repeat<R, S, E, A, B, C> implements SealedZIO<R, E, Either<C, B>> {
    
    private final ZIO<R, E, A> current;
    private final Schedule<R, S, A, B> schedule;
    private final Function2<E, Option<B>, ZIO<R, E, C>> orElse;

    public Repeat(ZIO<R, E, A> current, Schedule<R, S, A, B> schedule, Function2<E, Option<B>, ZIO<R, E, C>> orElse) {
      this.current = checkNonNull(current);
      this.schedule = checkNonNull(schedule);
      this.orElse = checkNonNull(orElse);
    }
    
    @Override
    public Either<E, Either<C, B>> provide(R env) {
    return run().provide(env);
    }
    
    @Override
    public <F extends Witness> Kind<F, Either<C, B>> foldMap(R env, MonadDefer<F> monad) {
      return run().foldMap(env, monad);
    }

    private ZIO<R, E, Either<C, B>> run() {
      return current.foldM(
          error -> orElse.apply(error, Option.<B>none()).map(Either::<C, B>left),
          a -> schedule.initial().<E>toZIO().flatMap(s -> loop(a, s)));
    }

    private ZIO<R, E, Either<C, B>> loop(A later, S state) {
      return schedule.update(later, state)
        .foldM(error -> ZIO.pure(Either.right(schedule.extract(later, state))), 
          s -> current.foldM(
            e -> orElse.apply(e, Option.some(schedule.extract(later, state))).map(Either::<C, B>left), 
            a -> loop(a, s)));
    }
  }

  final class Retry<R, S, E, A, B> implements SealedZIO<R, E, Either<B, A>> {
    
    private final ZIO<R, E, A> current;
    private final Schedule<R, S, E, S> schedule;
    private final Function2<E, S, ZIO<R, E, B>> orElse;
    
    public Retry(ZIO<R, E, A> current, Schedule<R, S, E, S> schedule, Function2<E, S, ZIO<R, E, B>> orElse) {
      this.current = checkNonNull(current);
      this.schedule = checkNonNull(schedule);
      this.orElse = checkNonNull(orElse);
    }

    @Override
    public Either<E, Either<B, A>> provide(R env) {
      return run().provide(env);
    }

    @Override
    public <F extends Witness> Kind<F, Either<B, A>> foldMap(R env, MonadDefer<F> monad) {
      return run().foldMap(env, monad);
    }

    private ZIO<R, E, Either<B, A>> run() {
      return schedule.initial().<E>toZIO().flatMap(this::loop);
    }

    private ZIO<R, E, Either<B, A>> loop(S state) {
      return current.foldM(error -> {
        ZIO<R, Unit, S> update = schedule.update(error, state);
        return update.foldM(
          e -> orElse.apply(error, state).map(Either::<B, A>left), this::loop);
      }, value -> ZIO.pure(Either.right(value)));
    }
  }

  final class Sleep<R> implements SealedZIO<R, Throwable, Unit> {

    private final Duration duration;

    protected Sleep(Duration duration) {
      this.duration = checkNonNull(duration);
    }

    @Override
    public Either<Throwable, Unit> provide(R env) {
      try {
        Thread.sleep(duration.toMillis());
        return Either.right(Unit.unit());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return Either.left(e);
      }
    }

    @Override
    public <F extends Witness> Kind<F, Unit> foldMap(R env, MonadDefer<F> monad) {
      return monad.sleep(duration);
    }

    @Override
    public String toString() {
      return "Sleep(" + duration + ')';
    }
  }
  
  final class Timed<R, E, A> implements SealedZIO<R, E, Tuple2<Duration, A>> {
    
    private final ZIO<R, E, A> current;

    protected Timed(ZIO<R, E, A> current) {
      this.current = checkNonNull(current);
    }
    
    @Override
    public Either<E, Tuple2<Duration, A>> provide(R env) {
      long start = System.nanoTime();
      Either<E, A> result = current.provide(env);
      return result.map(value -> Tuple.of(Duration.ofNanos(System.nanoTime() - start), value));
    }
    
    @Override
    public <F extends Witness> Kind<F, Tuple2<Duration, A>> foldMap(R env, MonadDefer<F> monad) {
      return monad.defer(() -> provide(env).fold(e -> monad.raiseError(wrap(e)), monad::<Tuple2<Duration, A>>pure));
    }
    
    @Override
    public String toString() {
      return "Timed(" + current + ')';
    }
  }

  final class Bracket<R, E, A, B> implements SealedZIO<R, E, B> {

    private final ZIO<R, E, A> acquire;
    private final Function1<A, ZIO<R, E, B>> use;
    private final Consumer1<A> release;

    protected Bracket(ZIO<R, E, A> acquire,
                    Function1<A, ZIO<R, E, B>> use,
                    Consumer1<A> release) {
      this.acquire = checkNonNull(acquire);
      this.use = checkNonNull(use);
      this.release = checkNonNull(release);
    }

    @Override
    public Either<E, B> provide(R env) {
      try (ZIOResource<E, A> resource = new ZIOResource<>(acquire.provide(env), release)) {
        return resource.apply(use).provide(env);
      }
    }

    @Override
    public <F extends Witness> Kind<F, B> foldMap(R env, MonadDefer<F> monad) {
      return monad.bracket(acquire.foldMap(env, monad),
                           use.andThen(zio -> zio.foldMap(env, monad)),
                           release);
    }

    @Override
    public String toString() {
      return "Bracket(" + acquire + ", ?, ?)";
    }
  }
}

interface ZIOModule {
  ZIO<?, ?, Unit> UNIT = ZIO.pure(Unit.unit());

  @SuppressWarnings("unchecked")
  static <R, E, A, F, B> ZIO<R, E, A> collapse(ZIO<R, E, A> self) {
    ZIO<R, E, A> current = self;
    while (true) {
      if (current instanceof ZIO.Suspend) {
        ZIO.Suspend<R, E, A> suspend = (ZIO.Suspend<R, E, A>) current;
        current = suspend.next();
      } else if (current instanceof ZIO.FlatMapped) {
        ZIO.FlatMapped<R, F, B, E, A> flatMapped = (ZIO.FlatMapped<R, F, B, E, A>) current;
        return new ZIO.FlatMapped<>(
            flatMapped::start,
            e -> collapse(flatMapped.run(Either.left(e))),
            a -> collapse(flatMapped.run(Either.right(a))));
      } else {
        break;
      }
    }
    return current;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  static <R, E, A> Either<E, A> evaluate(R env, ZIO<R, E, A> self) {
    Deque<Function1<Either, ZIO>> stack = new LinkedList<>();
    ZIO<R, E, A> current = self;
    while (true) {
      if (current instanceof ZIO.FlatMapped) {
        ZIO.FlatMapped currentFlatMapped = (ZIO.FlatMapped) current;
        ZIO<R, E, A> next = currentFlatMapped.start();
        if (next instanceof ZIO.FlatMapped) {
          ZIO.FlatMapped nextFlatMapped = (ZIO.FlatMapped) next;
          current = nextFlatMapped.start();
          stack.push(currentFlatMapped::run);
          stack.push(nextFlatMapped::run);
        } else {
          current = currentFlatMapped.run(next.provide(env));
        }
      } else if (!stack.isEmpty()) {
        current = stack.pop().apply(current.provide(env));
      } else {
        break;
      }
    }
    return current.provide(env);
  }
}

final class ZIOResource<E, A> implements AutoCloseable {

  private final Either<E, A> resource;
  private final Consumer1<A> release;

  ZIOResource(Either<E, A> resource, Consumer1<A> release) {
    this.resource = checkNonNull(resource);
    this.release = checkNonNull(release);
  }

  public <R, B> ZIO<R, E, B> apply(Function1<A, ZIO<R, E, B>> use) {
    return resource.map(use).fold(ZIO::raiseError, identity());
  }

  @Override
  public void close() {
    resource.toOption().ifPresent(release);
  }
}