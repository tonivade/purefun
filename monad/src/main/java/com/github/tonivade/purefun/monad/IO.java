/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

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
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Recoverable;
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
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Async;
import com.github.tonivade.purefun.typeclasses.Instance;

@HigherKind(sealed = true)
public interface IO<T> extends IOOf<T>, Effect<IO_, T>, Recoverable {

  T unsafeRunSync();

  default Try<T> safeRunSync() {
    return Try.of(this::unsafeRunSync);
  }

  default Future<T> toFuture() {
    return toFuture(Future.DEFAULT_EXECUTOR);
  }

  default Future<T> toFuture(Executor executor) {
    return foldMap(Instance.async(Future_.class, executor)).fix(FutureOf.toFuture());
  }

  default void safeRunAsync(Consumer1<? super Try<? extends T>> callback) {
    safeRunAsync(Future.DEFAULT_EXECUTOR, callback);
  }

  default void safeRunAsync(Executor executor, Consumer1<? super Try<? extends T>> callback) {
    toFuture(executor).onComplete(callback);
  }

  <F extends Witness> Kind<F, T> foldMap(Async<F> monad);

  @Override
  default <R> IO<R> map(Function1<? super T, ? extends R> map) {
    return flatMap(map.andThen(IO::pure));
  }

  @Override
  default <R> IO<R> flatMap(Function1<? super T, ? extends Kind<IO_, ? extends R>> map) {
    return new FlatMapped<>(Producer.cons(this), map.andThen(IOOf::narrowK));
  }

  @Override
  default <R> IO<R> andThen(Kind<IO_, ? extends R> after) {
    return flatMap(ignore -> after);
  }

  @Override
  default <R> IO<R> ap(Kind<IO_, Function1<? super T, ? extends R>> apply) {
    return new Apply<>(this, apply.fix(IOOf.toIO()));
  }

  default IO<Try<T>> attempt() {
    return new Attempt<>(this);
  }

  default IO<Either<Throwable, T>> either() {
    return attempt().map(Try::toEither);
  }

  default <L, R> IO<Either<L, R>> either(Function1<? super Throwable, ? extends L> mapError,
                                         Function1<? super T, ? extends R> mapper) {
    return either().map(either -> either.bimap(mapError, mapper));
  }

  default <R> IO<R> redeem(Function1<? super Throwable, ? extends R> mapError,
                           Function1<? super T, ? extends R> mapper) {
    return attempt().map(try_ -> try_.fold(mapError, mapper));
  }

  default <R> IO<R> redeemWith(Function1<? super Throwable, ? extends Kind<IO_, ? extends R>> mapError,
                               Function1<? super T, ? extends Kind<IO_, ? extends R>> mapper) {
    return attempt().flatMap(try_ -> try_.fold(mapError, mapper));
  }

  default IO<T> recover(Function1<? super Throwable, ? extends T> mapError) {
    return redeem(mapError, identity());
  }

  @SuppressWarnings("unchecked")
  default <X extends Throwable> IO<T> recoverWith(Class<X> type, Function1<? super X, ? extends T> function) {
    return recover(cause -> {
      if (type.isAssignableFrom(cause.getClass())) {
        return function.apply((X) cause);
      }
      return sneakyThrow(cause);
    });
  }

  @Override
  default IO<Tuple2<Duration, T>> timed() {
    return new Timed<>(this);
  }

  @Override
  default IO<T> repeat() {
    return repeat(1);
  }

  @Override
  default IO<T> repeat(int times) {
    return IOModule.repeat(this, unit(), times);
  }

  @Override
  default IO<T> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  @Override
  default IO<T> repeat(Duration delay, int times) {
    return IOModule.repeat(this, sleep(delay), times);
  }

  @Override
  default IO<T> retry() {
    return retry(1);
  }

  @Override
  default IO<T> retry(int maxRetries) {
    return IOModule.retry(this, unit(), maxRetries);
  }

  @Override
  default IO<T> retry(Duration delay) {
    return retry(delay, 1);
  }

  @Override
  default IO<T> retry(Duration delay, int maxRetries) {
    return IOModule.retry(this, sleep(delay), maxRetries);
  }

  static <T> IO<T> pure(T value) {
    return new Pure<>(value);
  }

  static <T> IO<T> raiseError(Throwable error) {
    return new Failure<>(error);
  }

  static <T> IO<T> delay(Producer<? extends T> lazy) {
    return suspend(lazy.map(IO::pure));
  }

  static <T> IO<T> suspend(Producer<? extends IO<? extends T>> lazy) {
    return new Suspend<>(lazy);
  }

  static <T, R> Function1<T, IO<R>> lift(Function1<T, R> task) {
    return task.andThen(IO::pure);
  }

  public static <A, B> Function1<A, IO<B>> liftOption(Function1<? super A, Option<? extends B>> function) {
    return value -> fromOption(function.apply(value));
  }

  public static <A, B> Function1<A, IO<B>> liftTry(Function1<? super A, Try<? extends B>> function) {
    return value -> fromTry(function.apply(value));
  }

  public static <A, B> Function1<A, IO<B>> liftEither(Function1<? super A, Either<Throwable, ? extends B>> function) {
    return value -> fromEither(function.apply(value));
  }

  static <T> IO<T> fromOption(Option<? extends T> task) {
    return fromEither(task.toEither());
  }

  static <T> IO<T> fromTry(Try<? extends T> task) {
    return fromEither(task.toEither());
  }

  static <T> IO<T> fromEither(Either<Throwable, ? extends T> task) {
    return task.fold(IO::raiseError, IO::pure);
  }

  static IO<Unit> sleep(Duration duration) {
    return new Sleep(duration);
  }

  static IO<Unit> exec(CheckedRunnable task) {
    return task(task.asProducer());
  }

  static <T> IO<T> task(Producer<? extends T> producer) {
    return new SyncTask<>(producer);
  }

  static <T> IO<T> async(Consumer1<Consumer1<? super Try<? extends T>>> callback) {
    return asyncF(callback.asFunction().andThen(IO::pure));
  }

  static <T> IO<T> asyncF(Function1<Consumer1<? super Try<? extends T>>, IO<Unit>> callback) {
    return new AsyncTask<>(callback);
  }

  static IO<Unit> unit() {
    return IOModule.UNIT;
  }

  static <T, R> IO<R> bracket(IO<? extends T> acquire, 
      Function1<? super T, ? extends IO<? extends R>> use, Consumer1<? super T> release) {
    return new Bracket<>(acquire, use, release);
  }

  static <T extends AutoCloseable, R> IO<R> bracket(IO<? extends T> acquire, 
      Function1<? super T, ? extends IO<? extends R>> use) {
    return bracket(acquire, use, AutoCloseable::close);
  }

  static IO<Unit> sequence(Sequence<IO<?>> sequence) {
    return sequence.fold(unit(), IO::andThen).andThen(unit());
  }

  static <A> IO<Sequence<A>> traverse(Sequence<? extends IO<A>> sequence) {
    return sequence.foldLeft(pure(ImmutableList.empty()), 
        (IO<Sequence<A>> xs, IO<A> a) -> map2(xs, a, Sequence::append));
  }

  static <A, B, C> IO<C> map2(IO<? extends A> fa, IO<? extends B> fb,
                              Function2<? super A, ? super B, ? extends C> mapper) {
    return fb.ap(fa.map(mapper.curried()));
  }

  static <A, B> IO<Tuple2<A, B>> tuple(IO<? extends A> fa, IO<? extends B> fb) {
    return map2(fa, fb, Tuple::of);
  }

  final class Pure<T> implements SealedIO<T> {

    private final T value;

    protected Pure(T value) {
      this.value = checkNonNull(value);
    }

    @Override
    public T unsafeRunSync() {
      return value;
    }

    @Override
    public <F extends Witness> Kind<F, T> foldMap(Async<F> monad) {
      return monad.pure(value);
    }

    @Override
    public String toString() {
      return "Pure(" + value + ")";
    }
  }

  final class Apply<A, B> implements SealedIO<B> {
    
    private final IO<? extends A> value;
    private final IO<Function1<? super A, ? extends B>> apply;

    protected Apply(IO<? extends A> value, IO<Function1<? super A, ? extends B>> apply) {
      this.value = checkNonNull(value);
      this.apply = checkNonNull(apply);
    }

    @Override
    public B unsafeRunSync() {
      return IOModule.evaluate(value.flatMap(a -> apply.map(map -> map.apply(a))));
    }
    
    @Override
    public <F extends Witness> Kind<F, B> foldMap(Async<F> monad) {
      return monad.ap(value.foldMap(monad), apply.foldMap(monad));
    }
    
    @Override
    public String toString() {
      return "Apply(" + value + ", ?)";
    }
  }

  final class FlatMapped<T, R> implements SealedIO<R> {

    private final Producer<? extends IO<? extends T>> current;
    private final Function1<? super T, ? extends IO<? extends R>> next;

    protected FlatMapped(Producer<? extends IO<? extends T>> current,
                         Function1<? super T, ? extends IO<? extends R>> next) {
      this.current = checkNonNull(current);
      this.next = checkNonNull(next);
    }

    @Override
    public R unsafeRunSync() {
      return IOModule.evaluate(this);
    }

    @Override
    public <F extends Witness> Kind<F, R> foldMap(Async<F> monad) {
      return monad.flatMap(
        current.andThen(IOOf::<T>narrowK).get().foldMap(monad), 
        next.andThen(IOOf::<R>narrowK).andThen(io -> io.foldMap(monad)));
    }

    @Override
    public <R1> IO<R1> flatMap(Function1<? super R, ? extends Kind<IO_, ? extends R1>> map) {
      return new FlatMapped<>(this::start, r -> new FlatMapped<>(() -> run(r), map.andThen(IOOf::narrowK)::apply));
    }

    @Override
    public String toString() {
      return "FlatMapped(" + current + ", ?)";
    }

    protected IO<T> start() {
      return current.andThen(IOOf::<T>narrowK).get();
    }

    protected IO<R> run(T value) {
      Function1<? super T, IO<R>> andThen = next.andThen(IOOf::narrowK);
      return andThen.apply(value);
    }
  }

  final class Failure<T> implements SealedIO<T>, Recoverable {

    private final Throwable error;

    protected Failure(Throwable error) {
      this.error = checkNonNull(error);
    }

    @Override
    public T unsafeRunSync() {
      return sneakyThrow(error);
    }

    @Override
    public <F extends Witness> Kind<F, T> foldMap(Async<F> monad) {
      return monad.raiseError(error);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> IO<R> map(Function1<? super T, ? extends R> map) {
      return (IO<R>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> IO<R> flatMap(Function1<? super T, ? extends Kind<IO_, ? extends R>> map) {
      return (IO<R>) this;
    }

    @Override
    public String toString() {
      return "Failure(" + error + ")";
    }
  }

  final class SyncTask<T> implements SealedIO<T> {

    private final Producer<? extends T> task;

    protected SyncTask(Producer<? extends T> task) {
      this.task = checkNonNull(task);
    }

    @Override
    public T unsafeRunSync() {
      return task.get();
    }

    @Override
    public <F extends Witness> Kind<F, T> foldMap(Async<F> monad) {
      return monad.later(task);
    }

    @Override
    public String toString() {
      return "Task(?)";
    }
  }

  final class AsyncTask<T> implements SealedIO<T> {

    private final Function1<Consumer1<? super Try<? extends T>>, IO<Unit>> callback;

    protected AsyncTask(Function1<Consumer1<? super Try<? extends T>>, IO<Unit>> callback) {
      this.callback = checkNonNull(callback);
    }

    @Override
    public T unsafeRunSync() {
      Promise<T> promise = Promise.make();
      IOModule.evaluate(callback.apply(promise::tryComplete));
      return promise.await().getOrElseThrow();
    }

    @Override
    public <F extends Witness> Kind<F, T> foldMap(Async<F> monad) {
      return monad.asyncF(c -> callback.apply(c).foldMap(monad));
    }

    @Override
    public String toString() {
      return "Async(?)";
    }
  }

  final class Suspend<T> implements SealedIO<T> {

    private final Producer<? extends IO<? extends T>> lazy;

    protected Suspend(Producer<? extends IO<? extends T>> lazy) {
      this.lazy = checkNonNull(lazy);
    }

    @Override
    public T unsafeRunSync() {
      return IOModule.collapse(this).unsafeRunSync();
    }

    @Override
    public <R> IO<R> flatMap(Function1<? super T, ? extends Kind<IO_, ? extends R>> map) {
      return new FlatMapped<>(lazy::get, map.andThen(IOOf::narrowK)::apply);
    }

    @Override
    public <F extends Witness> Kind<F, T> foldMap(Async<F> monad) {
      return monad.defer(() -> lazy.get().foldMap(monad));
    }

    @Override
    public String toString() {
      return "Suspend(?)";
    }

    protected IO<T> next() {
      return lazy.andThen(IOOf::<T>narrowK).get();
    }
  }

  final class Sleep implements SealedIO<Unit>, Recoverable {

    private final Duration duration;

    public Sleep(Duration duration) {
      this.duration = checkNonNull(duration);
    }

    @Override
    public Unit unsafeRunSync() {
      try {
        Thread.sleep(duration.toMillis());
        return Unit.unit();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return sneakyThrow(e);
      }
    }

    @Override
    public <F extends Witness> Kind<F, Unit> foldMap(Async<F> monad) {
      return monad.sleep(duration);
    }

    @Override
    public String toString() {
      return "Sleep(" + duration + ')';
    }
  }

  final class Bracket<T, R> implements SealedIO<R> {

    private final IO<? extends T> acquire;
    private final Function1<? super T, ? extends IO<? extends R>> use;
    private final Consumer1<? super T> release;

    protected Bracket(IO<? extends T> acquire, Function1<? super T, ? extends IO<? extends R>> use, Consumer1<? super T> release) {
      this.acquire = checkNonNull(acquire);
      this.use = checkNonNull(use);
      this.release = checkNonNull(release);
    }

    @Override
    public R unsafeRunSync() {
      try (IOResource<T> resource = new IOResource<>(IOModule.evaluate(acquire), release)) {
        return IOModule.evaluate(resource.apply(use));
      }
    }

    @Override
    public <F extends Witness> Kind<F, R> foldMap(Async<F> monad) {
      return monad.bracket(acquire.foldMap(monad), use.andThen(io -> io.foldMap(monad)), release);
    }

    @Override
    public String toString() {
      return "Bracket(" + acquire + ", ?, ?)";
    }
  }
  
  final class Timed<A> implements SealedIO<Tuple2<Duration, A>> {
    
    private final IO<A> current;

    protected Timed(IO<A> current) {
      this.current = checkNonNull(current);
    }
    
    @Override
    public Tuple2<Duration, A> unsafeRunSync() {
      long start = System.nanoTime();
      A result = IOModule.evaluate(current);
      return Tuple.of(Duration.ofNanos(System.nanoTime() - start), result);
    }
    
    @Override
    public <F extends Witness> Kind<F, Tuple2<Duration, A>> foldMap(Async<F> monad) {
      return monad.timed(current.foldMap(monad));
    }
    
    @Override
    public String toString() {
      return "Timed(" + current + ')';
    }
  }

  final class Attempt<T> implements SealedIO<Try<T>> {

    private final IO<T> current;

    protected Attempt(IO<T> current) {
      this.current = checkNonNull(current);
    }

    @Override
    public Try<T> unsafeRunSync() {
      return Try.of(() -> IOModule.evaluate(current));
    }

    @Override
    public <F extends Witness> Kind<F, Try<T>> foldMap(Async<F> monad) {
      return monad.map(monad.attempt(current.foldMap(monad)), Try::fromEither);
    }

    @Override
    public String toString() {
      return "Attempt(" + current + ")";
    }
  }
}

interface IOModule {

  IO<Unit> UNIT = IO.pure(Unit.unit());

  @SuppressWarnings("unchecked")
  static <A, X> IO<A> collapse(IO<A> self) {
    IO<A> current = self;
    while (true) {
      if (current instanceof IO.Suspend) {
        IO.Suspend<A> suspend = (IO.Suspend<A>) current;
        current = suspend.next();
      } else if (current instanceof IO.FlatMapped) {
        IO.FlatMapped<X, A> flatMapped = (IO.FlatMapped<X, A>) current;
        return new IO.FlatMapped<>(flatMapped::start, a -> collapse(flatMapped.run(a)));
      } else break;
    }
    return current;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  static <A> A evaluate(IO<A> self) {
    Deque<Function1<Object, IO>> stack = new LinkedList<>();
    IO<A> current = self;
    while (true) {
      if (current instanceof IO.FlatMapped) {
        IO.FlatMapped currentFlatMapped = (IO.FlatMapped) current;
        IO<A> next = currentFlatMapped.start();
        if (next instanceof IO.FlatMapped) {
          IO.FlatMapped nextFlatMapped = (IO.FlatMapped) next;
          current = nextFlatMapped.start();
          stack.push(currentFlatMapped::run);
          stack.push(nextFlatMapped::run);
        } else {
          current = currentFlatMapped.run(next.unsafeRunSync());
        }
      } else if (!stack.isEmpty()) {
        current = stack.pop().apply(current.unsafeRunSync());
      } else break;
    }
    return current.unsafeRunSync();
  }

  static <T> IO<T> repeat(IO<T> self, IO<Unit> pause, int times) {
    return self.redeemWith(IO::raiseError, value -> {
      if (times > 0) {
        return pause.andThen(repeat(self, pause, times - 1));
      } else return IO.pure(value);
    });
  }

  static <T> IO<T> retry(IO<T> self, IO<Unit> pause, int maxRetries) {
    return self.redeemWith(error -> {
      if (maxRetries > 0) {
        return pause.andThen(retry(self, pause.repeat(), maxRetries - 1));
      } else return IO.raiseError(error);
    }, IO::pure);
  }
}

final class IOResource<T> implements AutoCloseable {

  private final T resource;
  private final Consumer1<? super T> release;

  IOResource(T resource, Consumer1<? super T> release) {
    this.resource = checkNonNull(resource);
    this.release = checkNonNull(release);
  }

  public <R> IO<R> apply(Function1<? super T, ? extends IO<? extends R>> use) {
    return use.andThen(IOOf::<R>narrowK).apply(resource);
  }

  @Override
  public void close() {
    release.accept(resource);
  }
}