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
import java.time.Duration;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;
import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Effect;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.Promise;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.EitherOf;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

@HigherKind(sealed = true)
public interface ZIO<R, E, A> extends ZIOOf<R, E, A>, Effect<Kind<Kind<ZIO_, R>, E>, A> {

  default Either<E, A> provide(R env) {
    return runAsync(env).getOrElseThrow();
  }

  default Future<Either<E, A>> runAsync(R env) {
    return Future.from(ZIOModule.runAsync(env, this, IOConnection.UNCANCELLABLE));
  }

  default void provideAsync(R env, Consumer1<? super Try<? extends Either<E, A>>> callback) {
    runAsync(env).onComplete(callback::accept);
  }

  default ZIO<R, A, E> swap() {
    throw new UnsupportedOperationException();
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
    return new FlatMapped<>(this, left.andThen(ZIOOf::narrowK), right.andThen(ZIOOf::narrowK));
  }

  @Override
  default <B> ZIO<R, E, B> andThen(Kind<Kind<Kind<ZIO_, R>, E>, ? extends B> next) {
    return flatMap(ignore -> next);
  }

  @Override
  default <B> ZIO<R, E, B> ap(Kind<Kind<Kind<ZIO_, R>, E>, Function1<? super A, ? extends B>> apply) {
    return map2(this, apply.fix(ZIOOf.toZIO()), (v, a) -> a.apply(v));
  }

  default <B> URIO<R, B> fold(
      Function1<? super E, ? extends B> mapError, Function1<? super A, ? extends B> map) {
    return new URIO<>(biflatMap(mapError.andThen(ZIO::pure), map.andThen(ZIO::pure)));
  }

  default URIO<R, A> recover(Function1<? super E, ? extends A> mapError) {
    return fold(mapError, identity());
  }

  default ZIO<R, E, A> orElse(Kind<Kind<Kind<ZIO_, R>, E>, ? extends A> other) {
    return biflatMap(Function1.cons(other), Function1.cons(this));
  }
  
  @Override
  default <B> ZIO<R, E, Tuple2<A, B>> zip(Kind<Kind<Kind<ZIO_, R>, E>, ? extends B> other) {
    return zipWith(other, Tuple::of);
  }
  
  @Override
  default <B> ZIO<R, E, A> zipLeft(Kind<Kind<Kind<ZIO_, R>, E>, ? extends B> other) {
    return zipWith(other, first());
  }
  
  @Override
  default <B> ZIO<R, E, B> zipRight(Kind<Kind<Kind<ZIO_, R>, E>, ? extends B> other) {
    return zipWith(other, second());
  }
  
  @Override
  default <B, C> ZIO<R, E, C> zipWith(Kind<Kind<Kind<ZIO_, R>, E>, ? extends B> other, 
      Function2<? super A, ? super B, ? extends C> mapper) {
    return map2(this, other.fix(ZIOOf.toZIO()), mapper);
  }

  @Override
  default ZIO<R, E, A> repeat() {
    return repeat(1);
  }

  @Override
  default ZIO<R, E, A> repeat(int times) {
    return repeat(Schedule.<R, A>recurs(times).zipRight(Schedule.identity()));
  }

  @Override
  default ZIO<R, E, A> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  @Override
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

  @Override
  default ZIO<R, E, A> retry() {
    return retry(1);
  }

  @Override
  default ZIO<R, E, A> retry(int maxRetries) {
    return retry(Schedule.recurs(maxRetries));
  }

  @Override
  default ZIO<R, E, A> retry(Duration delay) {
    return retry(delay, 1);
  }

  @Override
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
    return ZIO.<R, E, Long>later(System::nanoTime).flatMap(
      start -> map(result -> Tuple.of(Duration.ofNanos(System.nanoTime() - start), result)));
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
    throw new UnsupportedOperationException("not implemented yet");
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
    return new Delay<>(task);
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

  static <R, E, A> ZIO<R, E, A> later(Producer<? extends A> task) {
    return fromEither(task.andThen(Either::right));
  }
  
  static <R, E, A> ZIO<R, E, A> async(Consumer1<Consumer1<? super Either<E, ? extends A>>> consumer) {
    return cancellable(consumer.asFunction().andThen(ZIO::pure));
  }
  
  static <R, E, A> ZIO<R, E, A> cancellable(Function1<Consumer1<? super Either<E, ? extends A>>, ZIO<R, Throwable, Unit>> consumer) {
    return new Async<>(consumer);
  }

  static <R, E, A> ZIO<R, E, A> raiseError(E error) {
    return new Failure<>(error);
  }

  static <R, A> ZIO<R, Throwable, A> redeem(ZIO<R, Nothing, ? extends A> value) {
    return new Redeem<>(value);
  }

  static <R> ZIO<R, Throwable, Unit> sleep(Duration delay) {
    return cancellable(callback -> {
      Future<Unit> sleep = Future.sleep(delay)
        .onComplete(result -> callback.accept(Either.right(Unit.unit())));
      return ZIO.exec(() -> sleep.cancel(true));
    });  
  }

  static <R, E, A> ZIO<R, E, Sequence<A>> traverse(Sequence<? extends ZIO<R, E, A>> sequence) {
    return sequence.foldLeft(pure(ImmutableList.empty()), 
        (ZIO<R, E, Sequence<A>> xs, ZIO<R, E, A> a) -> map2(xs, a, Sequence::append));
  }

  static <R, E, A extends AutoCloseable, B> ZIO<R, E, B> bracket(ZIO<R, E, ? extends A> acquire,
                                                                 Function1<? super A, ? extends ZIO<R, E, ? extends B>> use) {
    return bracket(acquire, use, AutoCloseable::close);
  }

  static <R, E, A, B> ZIO<R, E, B> bracket(ZIO<R, E, ? extends A> acquire,
                                           Function1<? super A, ? extends ZIO<R, E, ? extends B>> use,
                                           Consumer1<? super A> release) {
    return bracket(acquire, use, release.asFunction().andThen(ZIO::pure));
  }

  static <R, E, A, B> ZIO<R, E, B> bracket(ZIO<R, E, ? extends A> acquire,
                                           Function1<? super A, ? extends ZIO<R, E, ? extends B>> use,
                                           Function1<? super A, ? extends ZIO<R, E, Unit>> release) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @SuppressWarnings("unchecked")
  static <R, E> ZIO<R, E, Unit> unit() {
    return (ZIO<R, E, Unit>) ZIOModule.UNIT;
  }

  final class Pure<R, E, A> implements SealedZIO<R, E, A> {

    final A value;

    protected Pure(A value) {
      this.value = checkNonNull(value);
    }

    @Override
    public String toString() {
      return "Pure(" + value + ")";
    }
  }

  final class Failure<R, E, A> implements SealedZIO<R, E, A> {

    final E error;

    protected Failure(E error) {
      this.error = checkNonNull(error);
    }

    @Override
    public String toString() {
      return "Failure(" + error + ")";
    }
  }

  final class FlatMapped<R, E, A, F, B> implements SealedZIO<R, F, B> {

    final ZIO<R, E, A> current;
    final Function1<? super E, ? extends ZIO<R, F, ? extends B>> nextError;
    final Function1<? super A, ? extends ZIO<R, F, ? extends B>> next;

    protected FlatMapped(ZIO<R, E, A> current,
                         Function1<? super E, ? extends ZIO<R, F, ? extends B>> nextError,
                         Function1<? super A, ? extends ZIO<R, F, ? extends B>> next) {
      this.current = checkNonNull(current);
      this.nextError = checkNonNull(nextError);
      this.next = checkNonNull(next);
    }

    @Override
    public String toString() {
      return "FlatMapped(" + current + ", ?, ?)";
    }
  }

  final class Delay<R, E, A> implements SealedZIO<R, E, A> {

    final Producer<Either<E, ? extends A>> task;

    protected Delay(Producer<Either<E, ? extends A>> task) {
      this.task = checkNonNull(task);
    }

    @Override
    public String toString() {
      return "Delay(?)";
    }
  }

  final class Suspend<R, E, A> implements SealedZIO<R, E, A> {

    final Producer<ZIO<R, E, ? extends A>> lazy;

    protected Suspend(Producer<ZIO<R, E, ? extends A>> lazy) {
      this.lazy = checkNonNull(lazy);
    }

    @Override
    public String toString() {
      return "Suspend(?)";
    }
  }

  final class Async<R, E, A> implements SealedZIO<R, E, A> {

    final Function1<Consumer1<? super Either<E, ? extends A>>, ZIO<R, Throwable, Unit>> callback;

    protected Async(Function1<Consumer1<? super Either<E, ? extends A>>, ZIO<R, Throwable, Unit>> callback) {
      this.callback = checkNonNull(callback);
    }

    @Override
    public String toString() {
      return "Async(?)";
    }
  }

  final class Attempt<R, A> implements SealedZIO<R, Throwable, A> {

    final Producer<? extends A> current;

    protected Attempt(Producer<? extends A> current) {
      this.current = checkNonNull(current);
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
    public String toString() {
      return "Redeem(" + current + ")";
    }
  }

  final class AccessM<R, E, A> implements SealedZIO<R, E, A> {

    final Function1<? super R, ? extends ZIO<R, E, ? extends A>> function;

    protected AccessM(Function1<? super R, ? extends ZIO<R, E, ? extends A>> function) {
      this.function = checkNonNull(function);
    }

    @Override
    public String toString() {
      return "AccessM(?)";
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
    public String toString() {
      return "Repeat(" + current + ", ?, ?)";
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
    public String toString() {
      return "Retry(" + current + ", ?, ?)";
    }
  }
}

interface ZIOModule {

  ZIO<?, ?, Unit> UNIT = ZIO.pure(Unit.unit());

  static <R, E, A> Promise<Either<E, A>> runAsync(R env, ZIO<R, E, A> current, IOConnection connection) {
    return runAsync(env, current, connection, new CallStack<>(), Promise.make());
  }

  @SuppressWarnings("unchecked")
  static <R, E, F, G, A, B, C> Promise<Either<E, A>> runAsync(R env, ZIO<R, E, A> current, IOConnection connection, CallStack<R, E, A> stack, Promise<Either<E, A>> promise) {
    while (true) {
      try {
        current = unwrap(env, current, identity());
        
        if (current instanceof ZIO.Pure) {
          ZIO.Pure<R, E, A> pure = (ZIO.Pure<R, E, A>) current;
          return promise.succeeded(Either.right(pure.value));
        }
        
        if (current instanceof ZIO.Failure) {
          ZIO.Failure<R, E, A> failure = (ZIO.Failure<R, E, A>) current;
          return promise.succeeded(Either.left(failure.error));
        }
        
        if (current instanceof ZIO.Async) {
          return executeAsync((ZIO.Async<R, E, A>) current, connection, promise);
        }
        
        if (current instanceof ZIO.FlatMapped) {
          stack.push();
          
          ZIO.FlatMapped<R, F, B, E, A> flatMapped = (ZIO.FlatMapped<R, F, B, E, A>) current;
          ZIO<R, F, ? extends B> source = unwrap(env, flatMapped.current, identity());
          
          if (source instanceof ZIO.Async) {
            Promise<Either<F, B>> nextPromise = Promise.make();
            
            nextPromise.then(either -> {
              Function1<? super B, ZIO<R, E, A>> andThen = flatMapped.next.andThen(ZIOOf::narrowK);
              Function1<? super F, ZIO<R, E, A>> andThenError = flatMapped.nextError.andThen(ZIOOf::narrowK);
              ZIO<R, E, A> fold = either.fold(andThenError, andThen);
              runAsync(env, fold, connection, stack, promise);
            });
            
            executeAsync((ZIO.Async<R, F, B>) source, connection, nextPromise);
            
            return promise;
          }

          if (source instanceof ZIO.Pure) {
            ZIO.Pure<R, F, B> pure = (ZIO.Pure<R, F, B>) source;
            Function1<? super B, ZIO<R, E, A>> andThen = flatMapped.next.andThen(ZIOOf::narrowK);
            current = andThen.apply(pure.value);
          } else if (source instanceof ZIO.Failure) {
            ZIO.Failure<R, F, B> failure = (ZIO.Failure<R, F, B>) source;
            Function1<? super F, ZIO<R, E, A>> andThen = flatMapped.nextError.andThen(ZIOOf::narrowK);
            current = andThen.apply(failure.error);
          } else if (source instanceof ZIO.FlatMapped) {
            ZIO.FlatMapped<R, G, C, F, B> flatMapped2 = (ZIO.FlatMapped<R, G, C, F, B>) source;
            
            current = flatMapped2.current.biflatMap(
              e -> flatMapped2.nextError.apply(e).biflatMap(flatMapped.nextError, flatMapped.next), 
              a -> flatMapped2.next.apply(a).biflatMap(flatMapped.nextError, flatMapped.next));
          }
        } else {
          stack.pop();
        }
      } catch (Throwable error) {
        Option<ZIO<R, E, A>> result = stack.tryHandle(error);
        
        if (result.isPresent()) {
          current = result.get();
        } else {
          return promise.failed(error);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  static <R, E, F, A, U> ZIO<R, E, A> unwrap(R env, ZIO<R, E, A> current, Function1<ZIO<R, E, ? extends A>, ZIO<R, F, ? extends U>> next) {
    while (true) {
      if (current instanceof ZIO.Failure) {
        return current;
      } else if (current instanceof ZIO.Pure) {
        return current;
      } else if (current instanceof ZIO.FlatMapped) {
        return current;
      } else if (current instanceof ZIO.Async) {
        return current;
      } else if (current instanceof ZIO.AccessM) {
        ZIO.AccessM<R, E, A> accessM = (ZIO.AccessM<R, E, A>) current;
        current = accessM.function.andThen(ZIOOf::narrowK).apply(env);
      } else if (current instanceof ZIO.Suspend) {
        ZIO.Suspend<R, E, A> suspend = (ZIO.Suspend<R, E, A>) current;
        Producer<ZIO<R, E, A>> andThen = suspend.lazy.andThen(ZIOOf::narrowK);
        current = andThen.get();
      } else if (current instanceof ZIO.Delay) {
        ZIO.Delay<R, E, A> delay = (ZIO.Delay<R, E, A>) current;
        Either<E, ? extends A> value = delay.task.get();
        return value.fold(ZIO::raiseError, ZIO::pure);
      } else if (current instanceof ZIO.Attempt) {
        ZIO.Attempt<R, A> attempt = (ZIO.Attempt<R, A>) current;
        Either<E, ? extends A> either = (Either<E, ? extends A>) attempt.current.liftEither().get();
        return either.fold(ZIO::raiseError, ZIO::pure);
      } else {
        throw new IllegalStateException("not supported: " + current);
      }
    }
  }

  static <R, E, A> Promise<Either<E, A>> executeAsync(ZIO.Async<R, E, A> current, IOConnection connection, Promise<Either<E, A>> promise) {
    if (connection.isCancellable() && !connection.updateState(StateIO::startingNow).isRunnable()) {
      return promise.cancel();
    }
    
    Function1<Consumer1<? super Either<E, ? extends A>>, ZIO<R, Throwable, Unit>> callback = current.callback;
    connection.setCancelToken(callback.apply(either -> promise.succeeded(either.fix(EitherOf::narrowK))));
    
    promise.thenRun(() -> connection.setCancelToken(UNIT));
    
    if (connection.isCancellable() && connection.updateState(StateIO::notStartingNow).isCancelligNow()) {
      connection.cancelNow();
    }

    return promise;
  }
}

interface IOConnection {
  
  IOConnection UNCANCELLABLE = new IOConnection() {};
  
  default boolean isCancellable() { return false; }
  
  default void setCancelToken(ZIO<?, ?, Unit> cancel) { }
  
  default void cancelNow() { }
  
  default void cancel() { }
  
  default StateIO updateState(Operator1<StateIO> update) { return StateIO.INITIAL; }
  
  static IOConnection cancellable() {
    return new IOConnection() {
      
      private ZIO<?, ?, Unit> cancelToken;
      private final AtomicReference<StateIO> state = new AtomicReference<>(StateIO.INITIAL);
      
      @Override
      public boolean isCancellable() { return true; }
      
      @Override
      public void setCancelToken(ZIO<?, ?, Unit> cancel) { this.cancelToken = checkNonNull(cancel); }
      
      @Override
      public void cancelNow() { cancelToken.runAsync(null); }
      
      @Override
      public void cancel() {
        if (state.getAndUpdate(StateIO::cancellingNow).isCancelable()) {
          cancelNow();
        
          state.set(StateIO.CANCELLED);
        }
      }
      
      @Override
      public StateIO updateState(Operator1<StateIO> update) {
        return state.updateAndGet(update::apply);
      }
    };
  }
}

final class StateIO {
  
  public static final StateIO INITIAL = new StateIO(false, false, false);
  public static final StateIO CANCELLED = new StateIO(true, false, false);
  
  private final boolean isCancelled;
  private final boolean cancelligNow;
  private final boolean startingNow;
  
  public StateIO(boolean isCancelled, boolean cancelligNow, boolean startingNow) {
    this.isCancelled = isCancelled;
    this.cancelligNow = cancelligNow;
    this.startingNow = startingNow;
  }
  
  public boolean isCancelled() {
    return isCancelled;
  }
  
  public boolean isCancelligNow() {
    return cancelligNow;
  }
  
  public boolean isStartingNow() {
    return startingNow;
  }
  
  public StateIO cancellingNow() {
    return new StateIO(isCancelled, true, startingNow);
  }
  
  public StateIO startingNow() {
    return new StateIO(isCancelled, cancelligNow, true);
  }
  
  public StateIO notStartingNow() {
    return new StateIO(isCancelled, cancelligNow, false);
  }
  
  public boolean isCancelable() {
    return !isCancelled && !cancelligNow && !startingNow;
  }
  
  public boolean isRunnable() {
    return !isCancelled && !cancelligNow;
  }
}

final class CallStack<R, E, A> implements Recoverable {
  
  private StackItem<R, E, A> top = new StackItem<>();
  
  public void push() {
    top.push();
  }

  public void pop() {
    if (top.count() > 0) {
      top.pop();
    } else {
      top = top.prev();
    }
  }
  
  public void add(PartialFunction1<? super Throwable, ? extends ZIO<R, E, ? extends A>> mapError) {
    if (top.count() > 0) {
      top.pop();
      top = new StackItem<>(top);
    }
    top.add(mapError);
  }
  
  public Option<ZIO<R, E, A>> tryHandle(Throwable error) {
    while (top != null) {
      top.reset();
      Option<ZIO<R, E, A>> result = top.tryHandle(error);
      
      if (result.isPresent()) {
        return result;
      } else {
        top = top.prev();
      }
    }
    return Option.none();
  }
}

final class StackItem<R, E, A> {
  
  private int count = 0;
  private Deque<PartialFunction1<? super Throwable, ? extends ZIO<R, E, ? extends A>>> recover = new LinkedList<>();

  private final StackItem<R, E, A> prev;

  public StackItem() {
    this(null);
  }

  public StackItem(StackItem<R, E, A> prev) {
    this.prev = prev;
  }
  
  public StackItem<R, E, A> prev() {
    return prev;
  }
  
  public int count() {
    return count;
  }
  
  public void push() {
    count++;
  }
  
  public void pop() {
    count--;
  }
  
  public void reset() {
    count = 0;
  }
  
  public void add(PartialFunction1<? super Throwable, ? extends ZIO<R, E, ? extends A>> mapError) {
    recover.addFirst(mapError);
  }

  public Option<ZIO<R, E, A>> tryHandle(Throwable error) {
    while (!recover.isEmpty()) {
      PartialFunction1<? super Throwable, ? extends ZIO<R, E, ? extends A>> mapError = recover.removeFirst();
      if (mapError.isDefinedAt(error)) {
        PartialFunction1<? super Throwable, ZIO<R, E, A>> andThen = mapError.andThen(ZIOOf::narrowK);
        return Option.some(andThen.apply(error));
      }
    }
    return Option.none();
  }
}