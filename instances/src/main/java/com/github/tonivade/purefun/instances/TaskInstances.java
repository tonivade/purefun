/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import java.time.Duration;
import java.util.concurrent.Executor;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.effect.TaskOf;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Async;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Concurrent;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Fiber;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;
import com.github.tonivade.purefun.typeclasses.Resource;
import com.github.tonivade.purefun.typeclasses.Runtime;

public interface TaskInstances {

  static Functor<Task<?>> functor() {
    return TaskFunctor.INSTANCE;
  }

  static Applicative<Task<?>> applicative() {
    return TaskApplicative.INSTANCE;
  }

  static Monad<Task<?>> monad() {
    return TaskMonad.INSTANCE;
  }

  static MonadError<Task<?>, Throwable> monadError() {
    return TaskMonadError.INSTANCE;
  }

  static MonadThrow<Task<?>> monadThrow() {
    return TaskMonadThrow.INSTANCE;
  }

  static MonadDefer<Task<?>> monadDefer() {
    return TaskMonadDefer.INSTANCE;
  }

  static Async<Task<?>> async() {
    return TaskAsync.INSTANCE;
  }

  static Concurrent<Task<?>> concurrent() {
    return TaskConcurrent.instance(Future.DEFAULT_EXECUTOR);
  }

  static Concurrent<Task<?>> concurrent(Executor executor) {
    return TaskConcurrent.instance(executor);
  }

  static <A> Reference<Task<?>, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }

  static <A extends AutoCloseable> Resource<Task<?>, A> resource(Task<A> acquire) {
    return resource(acquire, AutoCloseable::close);
  }

  static <A> Resource<Task<?>, A> resource(Task<A> acquire, Consumer1<A> release) {
    return Resource.from(monadDefer(), acquire, release);
  }

  static Console<Task<?>> console() {
    return TaskConsole.INSTANCE;
  }

  static Runtime<Task<?>> runtime() {
    return TaskRuntime.INSTANCE;
  }
}

interface TaskFunctor extends Functor<Task<?>> {

  TaskFunctor INSTANCE = new TaskFunctor() {};

  @Override
  default <A, B> Task<B> map(Kind<Task<?>, ? extends A> value, Function1<? super A, ? extends B> map) {
    return TaskOf.toTask(value).map(map);
  }
}

interface TaskPure extends Applicative<Task<?>> {

  @Override
  default <A> Task<A> pure(A value) {
    return Task.pure(value);
  }
}

interface TaskApplicative extends TaskPure {

  TaskApplicative INSTANCE = new TaskApplicative() {};

  @Override
  default <A, B> Task<B>
          ap(Kind<Task<?>, ? extends A> value,
             Kind<Task<?>, ? extends Function1<? super A, ? extends B>> apply) {
    return value.fix(TaskOf::<A>toTask).ap(apply.fix(TaskOf::toTask));
  }
}

interface TaskMonad extends TaskPure, Monad<Task<?>> {

  TaskMonad INSTANCE = new TaskMonad() {};

  @Override
  default <A, B> Task<B>
          flatMap(Kind<Task<?>, ? extends A> value,
                  Function1<? super A, ? extends Kind<Task<?>, ? extends B>> map) {
    return TaskOf.toTask(value).flatMap(map.andThen(TaskOf::toTask));
  }
}

interface TaskMonadError extends TaskMonad, MonadError<Task<?>, Throwable> {

  TaskMonadError INSTANCE = new TaskMonadError() {};

  @Override
  default <A> Task<A> raiseError(Throwable error) {
    return Task.raiseError(error);
  }

  @Override
  default <A> Task<A> handleErrorWith(
      Kind<Task<?>, A> value,
      Function1<? super Throwable, ? extends Kind<Task<?>, ? extends A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<? super Throwable, Task<A>> mapError = handler.andThen(TaskOf::toTask);
    Function1<A, Task<A>> map = Task::pure;
    Task<A> task = TaskOf.toTask(value);
    return task.foldM(mapError, map);
  }
}

interface TaskMonadThrow extends TaskMonadError, MonadThrow<Task<?>> {

  TaskMonadThrow INSTANCE = new TaskMonadThrow() {};
}

interface TaskDefer extends Defer<Task<?>> {

  @Override
  default <A> Task<A>
          defer(Producer<? extends Kind<Task<?>, ? extends A>> defer) {
    return Task.defer(defer);
  }
}

interface TaskBracket extends TaskMonadError, Bracket<Task<?>, Throwable> {

  @Override
  default <A, B> Task<B>
          bracket(Kind<Task<?>, ? extends A> acquire,
                  Function1<? super A, ? extends Kind<Task<?>, ? extends B>> use,
                  Function1<? super A, ? extends Kind<Task<?>, Unit>> release) {
    return Task.bracket(acquire, use, release);
  }
}

interface TaskMonadDefer
    extends MonadDefer<Task<?>>, TaskDefer, TaskBracket {

  TaskMonadDefer INSTANCE = new TaskMonadDefer() {};

  @Override
  default Task<Unit> sleep(Duration duration) {
    return Task.sleep(duration);
  }
}

interface TaskAsync extends Async<Task<?>>, TaskMonadDefer {

  TaskAsync INSTANCE = new TaskAsync() {};

  @Override
  default <A> Task<A> asyncF(Function1<Consumer1<? super Try<? extends A>>, Kind<Task<?>, Unit>> consumer) {
    return Task.asyncF(consumer.andThen(TaskOf::toTask));
  }
}

interface TaskConcurrent extends TaskAsync, Concurrent<Task<?>> {

  static TaskConcurrent instance(Executor executor) {
    return () -> executor;
  }

  Executor executor();

  @Override
  default <A, B> Task<Either<Tuple2<A, Fiber<Task<?>, B>>, Tuple2<Fiber<Task<?>, A>, B>>> racePair(Kind<Task<?>, ? extends A> fa,
    Kind<Task<?>, ? extends B> fb) {
    return Task.racePair(executor(), fa, fb);
  }

  @Override
  default <A> Task<Fiber<Task<?>, A>> fork(Kind<Task<?>, ? extends A> value) {
    Task<A> fix = value.fix(TaskOf::toTask);
    return fix.fork();
  }

}

final class TaskConsole implements Console<Task<?>> {

  public static final TaskConsole INSTANCE = new TaskConsole();

  private final SystemConsole console = new SystemConsole();

  @Override
  public Task<String> readln() {
    return Task.task(console::readln);
  }

  @Override
  public Task<Unit> println(String text) {
    return Task.exec(() -> console.println(text));
  }
}

interface TaskRuntime extends Runtime<Task<?>> {

  TaskRuntime INSTANCE = new TaskRuntime() {};

  @Override
  default <T> T run(Kind<Task<?>, T> value) {
    return value.fix(TaskOf::toTask).safeRunSync().getOrElseThrow();
  }

  @Override
  default <T> Sequence<T> run(Sequence<Kind<Task<?>, T>> values) {
    return run(Task.traverse(values.map(TaskOf::<T>toTask)));
  }

  @Override
  default <T> Future<T> parRun(Kind<Task<?>, T> value, Executor executor) {
    return value.fix(TaskOf::<T>toTask).runAsync();
  }

  @Override
  default <T> Future<Sequence<T>> parRun(Sequence<Kind<Task<?>, T>> values, Executor executor) {
    return parRun(Task.traverse(values.map(TaskOf::<T>toTask)), executor);
  }
}