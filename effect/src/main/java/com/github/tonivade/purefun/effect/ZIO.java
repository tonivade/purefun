/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Producer.cons;
import static java.util.Objects.requireNonNull;

import java.util.Stack;
import java.util.concurrent.Executor;

import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Sealed;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@Sealed
@HigherKind
public interface ZIO<R, E, A> {

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

  <F extends Kind> Higher1<F, Either<E, A>> foldMap(R env, MonadDefer<F> monad);

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

  default <B, F> ZIO<R, F, B> foldM(Function1<E, ZIO<R, F, B>> mapError, Function1<A, ZIO<R, F, B>> map) {
    return new FoldM<>(this, mapError, map);
  }

  default <B> ZIO<R, Nothing, B> fold(Function1<E, B> mapError, Function1<A, B> map) {
    return foldM(mapError.andThen(ZIO::pure), map.andThen(ZIO::pure));
  }

  default ZIO<R, Nothing, A> recover(Function1<E, A> mapError) {
    return fold(mapError, identity());
  }

  default ZIO<R, E, A> orElse(Producer<ZIO<R, E, A>> other) {
    return foldM(other.asFunction(), Function1.cons(this));
  }

  ZIOModule getModule();

  static <R, E, A> ZIO<R, E, A> accessM(Function1<R, ZIO<R, E, A>> map) {
    return new AccessM<>(map);
  }

  static <R, A> ZIO<R, Nothing, A> access(Function1<R, A> map) {
    return accessM(map.andThen(ZIO::pure));
  }

  static <R> ZIO<R, Nothing, R> env() {
    return access(identity());
  }

  static <R, E, A, B, C> ZIO<R, E, C> map2(ZIO<R, E, A> za, ZIO<R, E, B> zb, Function2<A, B, C> mapper) {
    return za.flatMap(a -> zb.map(b -> mapper.curried().apply(a).apply(b)));
  }

  static <R, E, A> ZIO<R, E, A> absorb(ZIO<R, E, Either<E, A>> value) {
    return value.flatMap(either -> either.fold(ZIO::raiseError, ZIO::pure));
  }

  static <R, A, B> Function1<A, ZIO<R, Throwable, B>> lift(Function1<A, B> function) {
    return value -> from(() -> function.apply(value));
  }

  static <R, E, A> ZIO<R, E, A> fromEither(Producer<Either<E, A>> task) {
    return new Task<>(task);
  }

  static <R, A> ZIO<R, Throwable, A> from(Producer<A> task) {
    return new Attempt<>(task);
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

  static <R, E, A> ZIO<R, E, A> task(Producer<A> task) {
    return new Task<>(task.map(Either::right));
  }

  static <R, E, A> ZIO<R, E, A> raiseError(E error) {
    return new Failure<>(error);
  }

  static <R, A> ZIO<R, Throwable, A> redeem(ZIO<R, Nothing, A> value) {
    return new Redeem<>(value);
  }

  static <R, A extends AutoCloseable, B> ZIO<R, Throwable, B> bracket(ZIO<R, Throwable, A> acquire,
                                                                      Function1<A, ZIO<R, Throwable, B>> use) {
    return new Bracket<>(acquire, use, AutoCloseable::close);
  }

  static <R, A, B> ZIO<R, Throwable, B> bracket(ZIO<R, Throwable, A> acquire,
                                                Function1<A, ZIO<R, Throwable, B>> use,
                                                Consumer1<A> release) {
    return new Bracket<>(acquire, use, release);
  }

  @SuppressWarnings("unchecked")
  static <R, E> ZIO<R, E, Unit> unit() {
    return (ZIO<R, E, Unit>) ZIOModule.UNIT;
  }

  final class Pure<R, E, A> implements ZIO<R, E, A> {

    private final A value;

    protected Pure(A value) {
      this.value = requireNonNull(value);
    }

    @Override
    public Either<E, A> provide(R env) {
      return Either.right(value);
    }

    @Override
    public <F extends Kind> Higher1<F, Either<E, A>> foldMap(R env, MonadDefer<F> monad) {
      return monad.pure(Either.right(value));
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Pure(" + value + ")";
    }
  }

  final class Failure<R, E, A> implements ZIO<R, E, A> {

    private E error;

    protected Failure(E error) {
      this.error = requireNonNull(error);
    }

    @Override
    public Either<E, A> provide(R env) {
      return Either.left(error);
    }

    @Override
    public <F extends Kind> Higher1<F, Either<E, A>> foldMap(R env, MonadDefer<F> monad) {
      return monad.pure(Either.left(error));
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Failure(" + error + ")";
    }
  }

  final class FlatMapped<R, E, A, F, B> implements ZIO<R, F, B> {

    private final Producer<ZIO<R, E, A>> current;
    private final Function1<E, ZIO<R, F, B>> nextError;
    private final Function1<A, ZIO<R, F, B>> next;

    protected FlatMapped(Producer<ZIO<R, E, A>> current,
                         Function1<E, ZIO<R, F, B>> nextError,
                         Function1<A, ZIO<R, F, B>> next) {
      this.current = requireNonNull(current);
      this.nextError = requireNonNull(nextError);
      this.next = requireNonNull(next);
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
              () -> run((Either<E, A>) Either.left(f)),
              left::apply,
              right::apply)
          ,
          b -> new FlatMapped<>(
              () -> run((Either<E, A>) Either.right(b)),
              left::apply,
              right::apply)
      );
    }

    @Override
    public <X extends Kind> Higher1<X, Either<F, B>> foldMap(R env, MonadDefer<X> monad) {
      Higher1<X, Either<E, A>> foldMap = current.get().foldMap(env, monad);
      Higher1<X, ZIO<R, F, B>> map =
          monad.map(foldMap, either -> either.bimap(nextError, next).fold(identity(), identity()));
      return monad.flatMap(map, zio -> zio.foldMap(env, monad));
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
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

  final class Task<R, E, A> implements ZIO<R, E, A> {

    private final Producer<Either<E, A>> task;

    protected Task(Producer<Either<E, A>> task) {
      this.task = requireNonNull(task);
    }

    @Override
    public Either<E, A> provide(R env) {
      return task.get();
    }

    @Override
    public <F extends Kind> Higher1<F, Either<E, A>> foldMap(R env, MonadDefer<F> monad) {
      return monad.later(task::get);
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Task(?)";
    }
  }

  final class Suspend<R, E, A> implements ZIO<R, E, A> {

    private final Producer<ZIO<R, E, A>> lazy;

    protected Suspend(Producer<ZIO<R, E, A>> lazy) {
      this.lazy = requireNonNull(lazy);
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
    public <F extends Kind> Higher1<F, Either<E, A>> foldMap(R env, MonadDefer<F> monad) {
      return monad.defer(() -> lazy.get().foldMap(env, monad));
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    protected ZIO<R, E, A> next() {
      return lazy.get();
    }

    @Override
    public String toString() {
      return "Suspend(?)";
    }
  }

  final class Swap<R, E, A> implements ZIO<R, A, E> {

    private final ZIO<R, E, A> current;

    protected Swap(ZIO<R, E, A> current) {
      this.current = requireNonNull(current);
    }

    @Override
    public Either<A, E> provide(R env) {
      return current.provide(env).swap();
    }

    @Override
    public <F extends Kind> Higher1<F, Either<A, E>> foldMap(R env, MonadDefer<F> monad) {
      return monad.map(current.foldMap(env, monad), Either::swap);
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Swap(" + current + ")";
    }
  }

  final class Attempt<R, A> implements ZIO<R, Throwable, A> {

    private final Producer<A> current;

    protected Attempt(Producer<A> current) {
      this.current = requireNonNull(current);
    }

    @Override
    public Either<Throwable, A> provide(R env) {
      return Try.of(current).toEither();
    }

    @Override
    public <F extends Kind> Higher1<F, Either<Throwable, A>> foldMap(R env, MonadDefer<F> monad) {
      return monad.later(() -> Try.of(current).toEither());
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Attempt(" + current + ")";
    }
  }

  final class Redeem<R, A> implements ZIO<R, Throwable, A> {

    private final ZIO<R, Nothing, A> current;

    protected Redeem(ZIO<R, Nothing, A> current) {
      this.current = requireNonNull(current);
    }

    @Override
    public Either<Throwable, A> provide(R env) {
      return Try.of(() -> current.provide(env).get()).toEither();
    }

    @Override
    public <F extends Kind> Higher1<F, Either<Throwable, A>> foldMap(R env, MonadDefer<F> monad) {
      return monad.later(() -> provide(env));
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Redeem(" + current + ")";
    }
  }

  final class AccessM<R, E, A> implements ZIO<R, E, A> {

    private final Function1<R, ZIO<R, E, A>> function;

    protected AccessM(Function1<R, ZIO<R, E, A>> function) {
      this.function = requireNonNull(function);
    }

    @Override
    public Either<E, A> provide(R env) {
      return function.apply(env).provide(env);
    }

    @Override
    public <F extends Kind> Higher1<F, Either<E, A>> foldMap(R env, MonadDefer<F> monad) {
      Higher1<F, ZIO<R, E, A>> later = monad.later(() -> function.apply(env));
      return monad.flatMap(later, zio -> zio.foldMap(env, monad));
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "AccessM(?)";
    }
  }

  final class FoldM<R, E, A, F, B> implements ZIO<R, F, B> {

    private final ZIO<R, E, A> current;
    private final Function1<E, ZIO<R, F, B>> nextError;
    private final Function1<A, ZIO<R, F, B>> next;

    protected FoldM(ZIO<R, E, A> current, Function1<E, ZIO<R, F, B>> nextError, Function1<A, ZIO<R, F, B>> next) {
      this.current = requireNonNull(current);
      this.nextError = requireNonNull(nextError);
      this.next = requireNonNull(next);
    }

    @Override
    public Either<F, B> provide(R env) {
      return current.provide(env).fold(nextError, next).provide(env);
    }

    @Override
    public <X extends Kind> Higher1<X, Either<F, B>> foldMap(R env, MonadDefer<X> monad) {
      Higher1<X, Either<E, A>> foldMap = current.foldMap(env, monad);
      Higher1<X, ZIO<R, F, B>> map = monad.map(foldMap, either -> either.fold(nextError, next));
      return monad.flatMap(map, zio -> zio.foldMap(env, monad));
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "FoldM(" + current + ", ?, ?)";
    }
  }

  final class Bracket<R, A, B> implements ZIO<R, Throwable, B> {

    private final ZIO<R, Throwable, A> acquire;
    private final Function1<A, ZIO<R, Throwable, B>> use;
    private final Consumer1<A> release;

    protected Bracket(ZIO<R, Throwable, A> acquire,
                    Function1<A, ZIO<R, Throwable, B>> use,
                    Consumer1<A> release) {
      this.acquire = requireNonNull(acquire);
      this.use = requireNonNull(use);
      this.release = requireNonNull(release);
    }

    @Override
    public Either<Throwable, B> provide(R env) {
      try (ZIOResource<A> resource = new ZIOResource<>(acquire.provide(env), release)) {
        return resource.apply(use).provide(env);
      }
    }

    @Override
    public <F extends Kind> Higher1<F, Either<Throwable, B>> foldMap(R env, MonadDefer<F> monad) {
      return monad.bracket(monad.flatMap(acquire.foldMap(env, monad), monad::<A>fromEither),
                           use.andThen(zio -> zio.foldMap(env, monad)),
                           release);
    }

    @Override
    public ZIOModule getModule() {
      throw new UnsupportedOperationException();
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
  static <R, E, A, F, B> ZIO<R, E, A> collapse(ZIO<R, E, A> eval) {
    ZIO<R, E, A> current = eval;
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
      } else break;
    }
    return current;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  static <R, E, A> Either<E, A> evaluate(R env, ZIO<R, E, A> eval) {
    Stack<Function1<Either, ZIO>> stack = new Stack<>();
    ZIO<R, E, A> current = eval;
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
          current = (ZIO<R, E, A>) currentFlatMapped.run(next.provide(env));
        }
      } else if (!stack.isEmpty()) {
        current = (ZIO<R, E, A>) stack.pop().apply(current.provide(env));
      } else break;
    }
    return current.provide(env);
  }
}

final class ZIOResource<A> implements AutoCloseable {

  private final Either<Throwable, A> resource;
  private final Consumer1<A> release;

  ZIOResource(Either<Throwable, A> resource, Consumer1<A> release) {
    this.resource = requireNonNull(resource);
    this.release = requireNonNull(release);
  }

  public <R, B> ZIO<R, Throwable, B> apply(Function1<A, ZIO<R, Throwable, B>> use) {
    return resource.map(use).fold(ZIO::raiseError, identity());
  }

  @Override
  public void close() {
    resource.toOption().ifPresent(release);
  }
}