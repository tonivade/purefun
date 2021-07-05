/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.effect.TaskOf.toTask;
import java.time.Duration;
import java.util.concurrent.Executor;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.effect.TaskOf;
import com.github.tonivade.purefun.effect.Task_;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Async;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;
import com.github.tonivade.purefun.typeclasses.Resource;
import com.github.tonivade.purefun.typeclasses.Runtime;

public interface TaskInstances {

  static Functor<Task_> functor() {
    return TaskFunctor.INSTANCE;
  }

  static Applicative<Task_> applicative() {
    return TaskApplicative.INSTANCE;
  }

  static Monad<Task_> monad() {
    return TaskMonad.INSTANCE;
  }

  static MonadError<Task_, Throwable> monadError() {
    return TaskMonadError.INSTANCE;
  }

  static MonadThrow<Task_> monadThrow() {
    return TaskMonadThrow.INSTANCE;
  }

  static MonadDefer<Task_> monadDefer() {
    return TaskMonadDefer.INSTANCE;
  }

  static <A> Reference<Task_, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }
  
  static <A extends AutoCloseable> Resource<Task_, A> resource(Task<A> acquire) {
    return resource(acquire, AutoCloseable::close);
  }
  
  static <A> Resource<Task_, A> resource(Task<A> acquire, Consumer1<A> release) {
    return Resource.from(monadDefer(), acquire, release);
  }
  
  static Console<Task_> console() {
    return TaskConsole.INSTANCE;
  }
  
  static Runtime<Task_> runtime() {
    return TaskRuntime.INSTANCE;
  }
}

interface TaskFunctor extends Functor<Task_> {

  TaskFunctor INSTANCE = new TaskFunctor() {};

  @Override
  default <A, B> Task<B> map(Kind<Task_, ? extends A> value, Function1<? super A, ? extends B> map) {
    return TaskOf.narrowK(value).map(map);
  }
}

interface TaskPure extends Applicative<Task_> {

  @Override
  default <A> Task<A> pure(A value) {
    return Task.pure(value);
  }
}

interface TaskApplicative extends TaskPure {

  TaskApplicative INSTANCE = new TaskApplicative() {};

  @Override
  default <A, B> Task<B>
          ap(Kind<Task_, ? extends A> value,
             Kind<Task_, ? extends Function1<? super A, ? extends B>> apply) {
    return value.fix(TaskOf::<A>narrowK).ap(apply.fix(TaskOf::narrowK));
  }
}

interface TaskMonad extends TaskPure, Monad<Task_> {

  TaskMonad INSTANCE = new TaskMonad() {};

  @Override
  default <A, B> Task<B>
          flatMap(Kind<Task_, ? extends A> value,
                  Function1<? super A, ? extends Kind<Task_, ? extends B>> map) {
    return TaskOf.narrowK(value).flatMap(map.andThen(TaskOf::narrowK));
  }
}

interface TaskMonadError extends TaskMonad, MonadError<Task_, Throwable> {

  TaskMonadError INSTANCE = new TaskMonadError() {};

  @Override
  default <A> Task<A> raiseError(Throwable error) {
    return Task.<A>raiseError(error);
  }

  @Override
  default <A> Task<A> handleErrorWith(
      Kind<Task_, A> value,
      Function1<? super Throwable, ? extends Kind<Task_, ? extends A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<? super Throwable, Task<A>> mapError = handler.andThen(TaskOf::narrowK);
    Function1<A, Task<A>> map = Task::pure;
    Task<A> task = TaskOf.narrowK(value);
    return task.foldM(mapError, map);
  }
}

interface TaskMonadThrow extends TaskMonadError, MonadThrow<Task_> {

  TaskMonadThrow INSTANCE = new TaskMonadThrow() {};
}

interface TaskDefer extends Defer<Task_> {

  @Override
  default <A> Task<A>
          defer(Producer<? extends Kind<Task_, ? extends A>> defer) {
    return Task.defer(() -> defer.map(TaskOf::<A>narrowK).get());
  }
}

interface TaskBracket extends TaskMonadError, Bracket<Task_, Throwable> {

  @Override
  default <A, B> Task<B>
          bracket(Kind<Task_, ? extends A> acquire,
                  Function1<? super A, ? extends Kind<Task_, ? extends B>> use,
                  Function1<? super A, ? extends Kind<Task_, Unit>> release) {
    return Task.bracket(acquire.fix(TaskOf::narrowK), use.andThen(TaskOf::narrowK), release::apply);
  }
}

interface TaskMonadDefer
    extends MonadDefer<Task_>, TaskDefer, TaskBracket {

  TaskMonadDefer INSTANCE = new TaskMonadDefer() {};

  @Override
  default Task<Unit> sleep(Duration duration) {
    return Task.sleep(duration);
  }
}

interface TaskAsync extends Async<Task_>, TaskMonadDefer {

  TaskAsync INSTANCE = new TaskAsync() {};
  
  @Override
  default <A> Task<A> asyncF(Function1<Consumer1<? super Try<? extends A>>, Kind<Task_, Unit>> consumer) {
    return Task.asyncF(consumer.andThen(TaskOf::narrowK));
  }
}

final class TaskConsole implements Console<Task_> {

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

interface TaskRuntime extends Runtime<Task_> {
  
  TaskRuntime INSTANCE = new TaskRuntime() {};

  @Override
  default <T> T run(Kind<Task_, T> value) {
    return value.fix(toTask()).safeRunSync().getOrElseThrow();
  }
  
  @Override
  default <T> Sequence<T> run(Sequence<Kind<Task_, T>> values) {
    return run(Task.traverse(values.map(TaskOf::<T>narrowK)));
  }

  @Override
  default <T> Future<T> parRun(Kind<Task_, T> value, Executor executor) {
    return value.fix(toTask()).runAsync();
  }
  
  @Override
  default <T> Future<Sequence<T>> parRun(Sequence<Kind<Task_, T>> values, Executor executor) {
    return parRun(Task.traverse(values.map(TaskOf::<T>narrowK)), executor);
  }
}