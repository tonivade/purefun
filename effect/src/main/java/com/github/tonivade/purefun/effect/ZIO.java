/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
import com.github.tonivade.purefun.Effect;
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
import com.github.tonivade.purefun.concurrent.FutureOf;
import com.github.tonivade.purefun.concurrent.Future_;
import com.github.tonivade.purefun.concurrent.Promise;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.EitherOf;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Async;
import com.github.tonivade.purefun.typeclasses.Instance;

@HigherKind(sealed = true)
public interface ZIO<R, E, A> extends ZIOOf<R, E, A>, Effect<Kind<Kind<ZIO_, R>, E>, A> {

  Either<E, A> provide(R env);

  default Future<A> toFuture(R env) {
    return toFuture(Future.DEFAULT_EXECUTOR, env);
  }

  default Future<A> toFuture(Executor executor, R env) {
    return foldMap(env, Instance.async(Future_.class, executor)).fix(FutureOf.toFuture());
  }

  default void provideAsync(R env, Consumer1<? super Try<? extends A>> callback) {
    provideAsync(Future.DEFAULT_EXECUTOR, env, callback);
  }

  default void provideAsync(Executor executor, R env, Consumer1<? super Try<? extends A>> callback) {
    toFuture(executor, env).onComplete(callback);
  }

  <F extends Witness> Kind<F, A> foldMap(R env, Async<F> monad);

  default ZIO<R, A, E> swap() {
    return new Swap<>(this);
  }

  @Override
  default <B> ZIO<R, E, B> map(Function1<? super A, ? extends B> map) {
    return flatMap(map.andThen(ZIO::pure));
  }

  default <B> ZIO<R, B, A> mapError(Function1<? super E, ? extends B> map) {
    return flatMapError(map.andThen(ZIO::raiseError));
  }

  default <B, F> ZIO<R, F, B> bimap(Function1<? super E, ? extends F> mapError, Function1<? super A, ? extends B> map) {
    return biflatMap(mapError.andThen(ZIO::raiseError), map.andThen(ZIO::pure));
  }

  @Override
  default <B> ZIO<R, E, B> flatMap(Function1<? super A, ? extends Kind<Kind<Kind<ZIO_, R>, E>, ? extends B>> map) {
    return biflatMap(ZIO::<R, E, B>raiseError, map.andThen(ZIOOf::narrowK));
  }

  default <F> ZIO<R, F, A> flatMapError(Function1<? super E, ? extends Kind<Kind<Kind<ZIO_, R>, F>, ? extends A>> map) {
    return biflatMap(map, ZIO::<R, F, A>pure);
  }

  default <F, B> ZIO<R, F, B> biflatMap(
      Function1<? super E, ? extends Kind<Kind<Kind<ZIO_, R>, F>, ? extends B>> left, 
      Function1<? super A, ? extends Kind<Kind<Kind<ZIO_, R>, F>, ? extends B>> right) {
    return new FlatMapped<>(cons(this), left.andThen(ZIOOf::narrowK), right.andThen(ZIOOf::narrowK));
  }

  @Override
  default <B> ZIO<R, E, B> andThen(Kind<Kind<Kind<ZIO_, R>, E>, ? extends B> next) {
    return flatMap(ignore -> next);
  }

  @Override
  default <B> ZIO<R, E, B> ap(Kind<Kind<Kind<ZIO_, R>, E>, Function1<? super A, ? extends B>> apply) {
    return new Apply<>(this, apply.fix(ZIOOf.toZIO()));
  }

  default <F, B> ZIO<R, F, B> foldM(
      Function1<? super E, ? extends Kind<Kind<Kind<ZIO_, R>, F>, ? extends B>> mapError, 
      Function1<? super A, ? extends Kind<Kind<Kind<ZIO_, R>, F>, ? extends B>> map) {
    return new FoldM<>(this, mapError.andThen(ZIOOf::narrowK), map.andThen(ZIOOf::narrowK));
  }

  default <B> ZIO<R, Nothing, B> fold(
      Function1<? super E, ? extends B> mapError, Function1<? super A, ? extends B> map) {
    return foldM(mapError.andThen(ZIO::pure), map.andThen(ZIO::pure));
  }

  default ZIO<R, Nothing, A> recover(Function1<? super E, ? extends A> mapError) {
    return fold(mapError, identity());
  }

  default ZIO<R, E, A> orElse(Kind<Kind<Kind<ZIO_, R>, E>, ? extends A> other) {
    return foldM(Function1.cons(other), Function1.cons(this));
  }
  
  default <B> ZIO<R, E, Tuple2<A, B>> zip(Kind<Kind<Kind<ZIO_, R>, E>, ? extends B> other) {
    return zipWith(other, Tuple::of);
  }
  
  default <B> ZIO<R, E, A> zipLeft(Kind<Kind<Kind<ZIO_, R>, E>, ? extends B> other) {
    return zipWith(other, first());
  }
  
  default <B> ZIO<R, E, B> zipRight(Kind<Kind<Kind<ZIO_, R>, E>, ? extends B> other) {
    return zipWith(other, second());
  }
  
  default <B, C> ZIO<R, E, C> zipWith(Kind<Kind<Kind<ZIO_, R>, E>, ? extends B> other, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return map2(this, other.fix(ZIOOf.toZIO()), mapper);
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
  
  default <B> ZIO<R, E, B> repeat(Schedule<R, A, B> schedule) {
    return repeatOrElse(schedule, (e, b) -> raiseError(e));
  }
  
  default <B> ZIO<R, E, B> repeatOrElse(
      Schedule<R, A, B> schedule,
      Function2<E, Option<B>, ZIO<R, E, B>> orElse) {
    return repeatOrElseEither(schedule, orElse).map(Either::merge);
  }

  default <B, C> ZIO<R, E, Either<C, B>> repeatOrElseEither(
      Schedule<R, A, B> schedule,
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
  
  default <B> ZIO<R, E, A> retry(Schedule<R, E, B> schedule) {
    return retryOrElse(schedule, (e, b) -> raiseError(e));
  }

  default <B> ZIO<R, E, A> retryOrElse(
      Schedule<R, E, B> schedule,
      Function2<E, B, ZIO<R, E, A>> orElse) {
    return retryOrElseEither(schedule, orElse).map(Either::merge);
  }

  default <B, C> ZIO<R, E, Either<B, A>> retryOrElseEither(
      Schedule<R, E, C> schedule,
      Function2<E, C, ZIO<R, E, B>> orElse) {
    return new Retry<>(this, schedule, orElse);
  }

  @Override
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
    return new RIO<>(refineOrDie(Throwable.class));
  }
  
  default Managed<R, E, A> toManaged() {
    return Managed.pure(this);
  }
  
  default Managed<R, E, A> toManaged(Consumer1<? super A> release) {
    return Managed.from(this, release);
  }

  static <R, E, A> ZIO<R, E, A> accessM(Function1<? super R, ? extends ZIO<R, E, ? extends A>> map) {
    return new AccessM<>(map);
  }

  static <R, E, A> ZIO<R, E, A> access(Function1<? super R, ? extends A> map) {
    return accessM(map.andThen(ZIO::pure));
  }

  static <R, E> ZIO<R, E, R> env() {
    return access(identity());
  }

  static <R, E, A, B, C> ZIO<R, E, C> map2(ZIO<R, E, ? extends A> za, ZIO<R, E, ? extends B> zb, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return zb.ap(za.map(mapper.curried()));
  }

  static <R, E, A> ZIO<R, E, A> absorb(ZIO<R, E, Either<E, A>> value) {
    return value.flatMap(either -> either.fold(ZIO::raiseError, ZIO::pure));
  }

  static <R, A, B> Function1<A, ZIO<R, Throwable, B>> lift(Function1<? super A, ? extends B> function) {
    return value -> pure(function.apply(value));
  }

  static <R, A, B> Function1<A, ZIO<R, Throwable, B>> liftOption(Function1<? super A, Option<? extends B>> function) {
    return value -> fromOption(function.apply(value));
  }

  static <R, A, B> Function1<A, ZIO<R, Throwable, B>> liftTry(Function1<? super A, Try<? extends B>> function) {
    return value -> fromTry(function.apply(value));
  }

  static <R, E, A, B> Function1<A, ZIO<R, E, B>> liftEither(Function1<? super A, Either<E, ? extends B>> function) {
    return value -> fromEither(function.apply(value));
  }

  static <R, A> ZIO<R, Throwable, A> fromOption(Option<? extends A> task) {
    return fromOption(cons(task));
  }

  static <R, A> ZIO<R, Throwable, A> fromOption(Producer<Option<? extends A>> task) {
    return fromEither(task.andThen(Option::toEither));
  }

  static <R, A> ZIO<R, Throwable, A> fromTry(Try<? extends A> task) {
    return fromTry(cons(task));
  }

  static <R, A> ZIO<R, Throwable, A> fromTry(Producer<Try<? extends A>> task) {
    return fromEither(task.andThen(Try::toEither));
  }

  static <R, E, A> ZIO<R, E, A> fromEither(Either<E, ? extends A> task) {
    return fromEither(cons(task));
  }

  static <R, E, A> ZIO<R, E, A> fromEither(Producer<Either<E, ? extends A>> task) {
    return new Task<>(task);
  }

  static <R> ZIO<R, Throwable, Unit> exec(CheckedRunnable task) {
    return new Attempt<>(task.asProducer());
  }

  static <R, E, A> ZIO<R, E, A> pure(A value) {
    return new Pure<>(value);
  }

  static <R, E, A> ZIO<R, E, A> defer(Producer<ZIO<R, E, ? extends A>> lazy) {
    return new Suspend<>(lazy);
  }

  static <R, A> ZIO<R, Throwable, A> task(Producer<? extends A> task) {
    return new Attempt<>(task);
  }
  
  static <R, A> ZIO<R, Throwable, A> async(Consumer1<Consumer1<? super Try<? extends A>>> consumer) {
    return asyncF(consumer.asFunction().andThen(UIO::pure));
  }
  
  static <R, A> ZIO<R, Throwable, A> asyncF(Function1<Consumer1<? super Try<? extends A>>, UIO<Unit>> consumer) {
    return new AsyncTask<>(consumer);
  }

  static <R, E, A> ZIO<R, E, A> raiseError(E error) {
    return new Failure<>(error);
  }

  static <R, A> ZIO<R, Throwable, A> redeem(ZIO<R, Nothing, ? extends A> value) {
    return new Redeem<>(value);
  }

  static <R> ZIO<R, Throwable, Unit> sleep(Duration delay) {
    return new Sleep<>(delay);
  }

  static <R, E, A> ZIO<R, E, Sequence<A>> traverse(Sequence<? extends ZIO<R, E, A>> sequence) {
    return sequence.foldLeft(pure(ImmutableList.empty()), 
        (ZIO<R, E, Sequence<A>> xs, ZIO<R, E, A> a) -> map2(xs, a, Sequence::append));
  }

  static <R, E, A extends AutoCloseable, B> ZIO<R, E, B> bracket(ZIO<R, E, ? extends A> acquire,
                                                                 Function1<? super A, ? extends ZIO<R, E, ? extends B>> use) {
    return new Bracket<>(acquire, use, AutoCloseable::close);
  }

  static <R, E, A, B> ZIO<R, E, B> bracket(ZIO<R, E, ? extends A> acquire,
                                           Function1<? super A, ? extends ZIO<R, E, ? extends B>> use,
                                           Consumer1<? super A> release) {
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
    public <F extends Witness> Kind<F, A> foldMap(R env, Async<F> monad) {
      return monad.pure(value);
    }

    @Override
    public String toString() {
      return "Pure(" + value + ")";
    }
  }

  final class Apply<R, E, A, B> implements SealedZIO<R, E, B> {
    
    private final ZIO<R, E, A> value;
    private final ZIO<R, E, Function1<? super A, ? extends B>> apply;

    protected Apply(ZIO<R, E, A> value, ZIO<R, E, Function1<? super A, ? extends B>> apply) {
      this.value = checkNonNull(value);
      this.apply = checkNonNull(apply);
    }

    @Override
    public Either<E, B> provide(R env) {
      return ZIOModule.evaluate(env, value.flatMap(a -> apply.map(map -> map.apply(a))));
    }
    
    @Override
    public <F extends Witness> Kind<F, B> foldMap(R env, Async<F> monad) {
      return monad.ap(value.foldMap(env, monad), apply.foldMap(env, monad));
    }
    
    @Override
    public String toString() {
      return "Apply(" + value + ", ?)";
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
    public <F extends Witness> Kind<F, A> foldMap(R env, Async<F> monad) {
      return monad.raiseError(wrap(error));
    }

    @Override
    public String toString() {
      return "Failure(" + error + ")";
    }
  }

  final class FlatMapped<R, E, A, F, B> implements SealedZIO<R, F, B> {

    private final Producer<ZIO<R, E, A>> current;
    private final Function1<? super E, ? extends ZIO<R, F, ? extends B>> nextError;
    private final Function1<? super A, ? extends ZIO<R, F, ? extends B>> next;

    protected FlatMapped(Producer<ZIO<R, E, A>> current,
                         Function1<? super E, ? extends ZIO<R, F, ? extends B>> nextError,
                         Function1<? super A, ? extends ZIO<R, F, ? extends B>> next) {
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
    public <F1, B1> ZIO<R, F1, B1> biflatMap(
        Function1<? super F, ? extends Kind<Kind<Kind<ZIO_, R>, F1>, ? extends B1>> left, 
        Function1<? super B, ? extends Kind<Kind<Kind<ZIO_, R>, F1>, ? extends B1>> right) {
      return new FlatMapped<>(
          () -> (ZIO<R, F, B>) start(),
          f -> new FlatMapped<>(
              () -> run((Either<E, A>) Either.left(f)), left.andThen(ZIOOf::narrowK)::apply, right.andThen(ZIOOf::narrowK)::apply),
          b -> new FlatMapped<>(
              () -> run((Either<E, A>) Either.right(b)), left.andThen(ZIOOf::narrowK)::apply, right.andThen(ZIOOf::narrowK)::apply)
      );
    }

    @Override
    public <X extends Witness> Kind<X, B> foldMap(R env, Async<X> monad) {
      Kind<X, A> foldMap = current.get().foldMap(env, monad);
      Kind<X, Either<Throwable, A>> attempt = monad.attempt(foldMap);
      Kind<X, ZIO<R, F, B>> map =
          monad.map(attempt, 
              either -> ZIOOf.narrowK(either.bimap(error -> nextError.apply(unwrap(error)), next).fold(identity(), identity())));
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
      return ZIOOf.narrowK(value.bimap(nextError, next).fold(identity(), identity()));
    }
  }

  final class Task<R, E, A> implements SealedZIO<R, E, A> {

    private final Producer<Either<E, ? extends A>> task;

    protected Task(Producer<Either<E, ? extends A>> task) {
      this.task = checkNonNull(task);
    }

    @Override
    public Either<E, A> provide(R env) {
      return EitherOf.narrowK(task.get());
    }

    @Override
    public <F extends Witness> Kind<F, A> foldMap(R env, Async<F> monad) {
      Kind<F, Either<E, ? extends A>> later = monad.later(task::get);
      return monad.flatMap(later, either -> either.fold(error -> monad.raiseError(wrap(error)), monad::<A>pure));
    }

    @Override
    public String toString() {
      return "Task(?)";
    }
  }

  final class Suspend<R, E, A> implements SealedZIO<R, E, A> {

    private final Producer<ZIO<R, E, ? extends A>> lazy;

    protected Suspend(Producer<ZIO<R, E, ? extends A>> lazy) {
      this.lazy = checkNonNull(lazy);
    }

    @Override
    public Either<E, A> provide(R env) {
      return ZIOModule.collapse(this).provide(env);
    }

    @Override
    public <B> ZIO<R, E, B> flatMap(Function1<? super A, ? extends Kind<Kind<Kind<ZIO_, R>, E>, ? extends B>> map) {
      return new FlatMapped<>(lazy::get, ZIO::raiseError, map.andThen(ZIOOf::narrowK)::apply);
    }

    @Override
    public <F extends Witness> Kind<F, A> foldMap(R env, Async<F> monad) {
      return monad.defer(() -> lazy.get().foldMap(env, monad));
    }

    protected ZIO<R, E, A> next() {
      return lazy.andThen(ZIOOf::<R, E, A>narrowK).get();
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
      return ZIOModule.evaluate(env, current).swap();
    }

    @Override
    public <F extends Witness> Kind<F, E> foldMap(R env, Async<F> monad) {
      return monad.flatMap(monad.attempt(current.foldMap(env, monad)), 
          either -> either.fold(
              error -> monad.pure(unwrap(error)), value -> monad.raiseError(wrap(value))));
    }

    @Override
    public String toString() {
      return "Swap(" + current + ")";
    }
  }

  final class AsyncTask<R, A> implements SealedZIO<R, Throwable, A> {

    private final Function1<Consumer1<? super Try<? extends A>>, UIO<Unit>> consumer;

    protected AsyncTask(Function1<Consumer1<? super Try<? extends A>>, UIO<Unit>> consumer) {
      this.consumer = checkNonNull(consumer);
    }

    @Override
    public Either<Throwable, A> provide(R env) {
      Promise<A> promise = Promise.make();
      ZIOModule.evaluate(env, consumer.apply(promise::tryComplete).toZIO());
      return promise.await().toEither();
    }

    @Override
    public <F extends Witness> Kind<F, A> foldMap(R env, Async<F> monad) {
      return monad.async(c -> consumer.apply(c).foldMap(monad));
    }

    @Override
    public String toString() {
      return "Async(?)";
    }
  }

  final class Attempt<R, A> implements SealedZIO<R, Throwable, A> {

    private final Producer<? extends A> current;

    protected Attempt(Producer<? extends A> current) {
      this.current = checkNonNull(current);
    }

    @Override
    public Either<Throwable, A> provide(R env) {
      return Try.<A>of(current).toEither();
    }

    @Override
    public <F extends Witness> Kind<F, A> foldMap(R env, Async<F> monad) {
      return monad.later(current);
    }

    @Override
    public String toString() {
      return "Attempt(" + current + ")";
    }
  }

  final class Redeem<R, A> implements SealedZIO<R, Throwable, A> {

    private final ZIO<R, Nothing, ? extends A> current;

    protected Redeem(ZIO<R, Nothing, ? extends A> current) {
      this.current = checkNonNull(current);
    }

    @Override
    public Either<Throwable, A> provide(R env) {
      return Try.<A>of(() -> ZIOModule.evaluate(env, current).get()).toEither();
    }

    @Override
    public <F extends Witness> Kind<F, A> foldMap(R env, Async<F> monad) {
      return monad.defer(() -> provide(env).fold(monad::<A>raiseError, monad::<A>pure));
    }

    @Override
    public String toString() {
      return "Redeem(" + current + ")";
    }
  }

  final class AccessM<R, E, A> implements SealedZIO<R, E, A> {

    private final Function1<? super R, ? extends ZIO<R, E, ? extends A>> function;

    protected AccessM(Function1<? super R, ? extends ZIO<R, E, ? extends A>> function) {
      this.function = checkNonNull(function);
    }

    @Override
    public Either<E, A> provide(R env) {
      return ZIOModule.evaluate(env, function.apply(env));
    }

    @Override
    public <F extends Witness> Kind<F, A> foldMap(R env, Async<F> monad) {
      Kind<F, ZIO<R, E, ? extends A>> later = monad.later(() -> function.apply(env));
      return monad.flatMap(later, zio -> zio.foldMap(env, monad));
    }

    @Override
    public String toString() {
      return "AccessM(?)";
    }
  }

  final class FoldM<R, E, A, F, B> implements SealedZIO<R, F, B> {

    private final ZIO<R, E, A> current;
    private final Function1<? super E, ? extends ZIO<R, F, ? extends B>> nextError;
    private final Function1<? super A, ? extends ZIO<R, F, ? extends B>> next;

    protected FoldM(ZIO<R, E, A> current, 
        Function1<? super E, ? extends ZIO<R, F, ? extends B>> nextError, 
            Function1<? super A, ? extends ZIO<R, F, ? extends B>> next) {
      this.current = checkNonNull(current);
      this.nextError = checkNonNull(nextError);
      this.next = checkNonNull(next);
    }

    @Override
    public Either<F, B> provide(R env) {
      return ZIOModule.evaluate(env, ZIOModule.evaluate(env, current).fold(nextError, next));
    }

    @Override
    public <X extends Witness> Kind<X, B> foldMap(R env, Async<X> monad) {
      Kind<X, A> foldMap = current.foldMap(env, monad);
      Kind<X, Either<Throwable, A>> attempt = monad.attempt(foldMap);
      Kind<X, ZIO<R, F, B>> map = monad.map(attempt, 
          either -> either.fold(
              error -> nextError.andThen(ZIOOf::<R, F, B>narrowK).apply(unwrap(error)), 
              next.andThen(ZIOOf::<R, F, B>narrowK)));
      return monad.flatMap(map, zio -> zio.foldMap(env, monad));
    }

    @Override
    public String toString() {
      return "FoldM(" + current + ", ?, ?)";
    }
  }
  
  final class Repeat<R, S, E, A, B, C> implements SealedZIO<R, E, Either<C, B>> {
    
    private final ZIO<R, E, A> current;
    private final ScheduleImpl<R, S, A, B> schedule;
    private final Function2<E, Option<B>, ZIO<R, E, C>> orElse;

    @SuppressWarnings("unchecked")
    protected Repeat(ZIO<R, E, A> current, Schedule<R, A, B> schedule, Function2<E, Option<B>, ZIO<R, E, C>> orElse) {
      this.current = checkNonNull(current);
      this.schedule = (ScheduleImpl<R, S, A, B>) checkNonNull(schedule);
      this.orElse = checkNonNull(orElse);
    }

    @Override
    public Either<E, Either<C, B>> provide(R env) {
      return ZIOModule.evaluate(env, current).fold(
          error -> ZIOModule.evaluate(env, orElse.apply(error, Option.<B>none()).map(Either::<C, B>left)),
          a -> ZIOModule.evaluate(env, schedule.initial().<E>toZIO()).flatMap(s -> run(env, a, s)));
    }
    
    @Override
    public <F extends Witness> Kind<F, Either<C, B>> foldMap(R env, Async<F> monad) {
      return current.foldM(
          error -> orElse.apply(error, Option.<B>none()).map(Either::<C, B>left),
          a -> schedule.initial().<E>toZIO().flatMap(s -> loop(a, s)))
        .foldMap(env, monad);
    }

    @Override
    public String toString() {
      return "Repeat(" + current + ", ?, ?)";
    }

    private ZIO<R, E, Either<C, B>> loop(A later, S state) {
      return schedule.update(later, state)
        .foldM(error -> ZIO.pure(Either.right(schedule.extract(later, state))), 
          s -> current.foldM(
            e -> orElse.apply(e, Option.some(schedule.extract(later, state))).map(Either::<C, B>left), 
            a -> loop(a, s)));
    }

    private Either<E, Either<C, B>> run(R env, A last, S state) {
      A a = last;
      S s = state;
      
      while (true) {
        Either<Unit, S> update = ZIOModule.evaluate(env, schedule.update(a, s));
        
        if (update.isLeft()) {
          return Either.right(Either.right(schedule.extract(a, s)));
        }
        
        Either<E, A> provide = ZIOModule.evaluate(env, current);
        
        if (provide.isLeft()) {
          return orElse.apply(provide.getLeft(), Option.some(schedule.extract(a, s))).provide(env).map(Either::<C, B>left);
        }

        a = provide.getRight();
        s = update.getRight();
      }
    }
  }

  final class Retry<R, E, A, B, S> implements SealedZIO<R, E, Either<B, A>> {
    
    private final ZIO<R, E, A> current;
    private final ScheduleImpl<R, S, E, S> schedule;
    private final Function2<E, S, ZIO<R, E, B>> orElse;

    @SuppressWarnings("unchecked")
    protected Retry(ZIO<R, E, A> current, Schedule<R, E, S> schedule, Function2<E, S, ZIO<R, E, B>> orElse) {
      this.current = checkNonNull(current);
      this.schedule = (ScheduleImpl<R, S, E, S>) checkNonNull(schedule);
      this.orElse = checkNonNull(orElse);
    }

    @Override
    public Either<E, Either<B, A>> provide(R env) {
      return ZIOModule.evaluate(env, schedule.initial().<E>toZIO()).flatMap(s -> run(env, s));
    }

    @Override
    public <F extends Witness> Kind<F, Either<B, A>> foldMap(R env, Async<F> monad) {
      ZIO<R, E, S> zio = schedule.initial().<E>toZIO();
      return monad.flatMap(zio.foldMap(env, monad), s -> loop(s).foldMap(env, monad));
    }

    @Override
    public String toString() {
      return "Retry(" + current + ", ?, ?)";
    }

    private ZIO<R, E, Either<B, A>> loop(S state) {
      return current.foldM(error -> {
        ZIO<R, Unit, S> update = schedule.update(error, state);
        return update.foldM(
          e -> orElse.apply(error, state).map(Either::<B, A>left), this::loop);
      }, value -> ZIO.pure(Either.right(value)));
    }
    
    private Either<E, Either<B, A>> run(R env, S state) {
      S s = state;
      
      while(true) {
        Either<E, A> provide = ZIOModule.evaluate(env, current);
        
        if (provide.isRight()) {
          return Either.right(Either.right(provide.getRight()));
        }
        
        Either<Unit, S> update = ZIOModule.evaluate(env, schedule.update(provide.getLeft(), s));
        
        if (update.isLeft()) {
          return ZIOModule.evaluate(env, orElse.apply(provide.getLeft(), s)).map(Either::<B, A>left);
        }
        
        s = update.getRight();
      }
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
    public <F extends Witness> Kind<F, Unit> foldMap(R env, Async<F> monad) {
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
      Either<E, A> result = ZIOModule.evaluate(env, current);
      return result.map(value -> Tuple.of(Duration.ofNanos(System.nanoTime() - start), value));
    }
    
    @Override
    public <F extends Witness> Kind<F, Tuple2<Duration, A>> foldMap(R env, Async<F> monad) {
      return monad.timed(current.foldMap(env, monad));
    }
    
    @Override
    public String toString() {
      return "Timed(" + current + ')';
    }
  }

  final class Bracket<R, E, A, B> implements SealedZIO<R, E, B> {

    private final ZIO<R, E, ? extends A> acquire;
    private final Function1<? super A, ? extends ZIO<R, E, ? extends B>> use;
    private final Consumer1<? super A> release;

    protected Bracket(ZIO<R, E, ? extends A> acquire,
                      Function1<? super A, ? extends ZIO<R, E, ? extends B>> use,
                      Consumer1<? super A> release) {
      this.acquire = checkNonNull(acquire);
      this.use = checkNonNull(use);
      this.release = checkNonNull(release);
    }

    @Override
    public Either<E, B> provide(R env) {
      try (ZIOResource<E, A> resource = new ZIOResource<>(ZIOModule.evaluate(env, acquire), release)) {
        return ZIOModule.evaluate(env, resource.apply(use));
      }
    }

    @Override
    public <F extends Witness> Kind<F, B> foldMap(R env, Async<F> monad) {
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
  static <R, E, A> Either<E, A> evaluate(R env, ZIO<R, E, ? extends A> self) {
    Deque<Function1<Either, ZIO>> stack = new LinkedList<>();
    ZIO<R, E, ? extends A> current = self;
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
    return EitherOf.narrowK(current.provide(env));
  }
}

final class ZIOResource<E, A> implements AutoCloseable {

  private final Either<E, ? extends A> resource;
  private final Consumer1<? super A> release;

  ZIOResource(Either<E, ? extends A> resource, Consumer1<? super A> release) {
    this.resource = checkNonNull(resource);
    this.release = checkNonNull(release);
  }

  public <R, B> ZIO<R, E, B> apply(Function1<? super A, ? extends ZIO<R, E, ? extends B>> use) {
    return resource.map(use.andThen(ZIOOf::<R, E, B>narrowK)).fold(ZIO::raiseError, identity());
  }

  @Override
  public void close() {
    resource.toOption().ifPresent(release);
  }
}