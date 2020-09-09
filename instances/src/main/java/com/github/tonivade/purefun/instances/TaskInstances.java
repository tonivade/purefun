/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import java.time.Duration;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.effect.TaskOf;
import com.github.tonivade.purefun.effect.Task_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;
import com.github.tonivade.purefun.typeclasses.Resource;

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
}

interface TaskFunctor extends Functor<Task_> {

  TaskFunctor INSTANCE = new TaskFunctor() {};

  @Override
  default <A, B> Task<B>
          map(Kind<Task_, A> value, Function1<A, B> map) {
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
          ap(Kind<Task_, A> value,
             Kind<Task_, Function1<A, B>> apply) {
    return TaskOf.narrowK(apply).flatMap(map -> TaskOf.narrowK(value).map(map));
  }
}

interface TaskMonad extends TaskPure, Monad<Task_> {

  TaskMonad INSTANCE = new TaskMonad() {};

  @Override
  default <A, B> Task<B>
          flatMap(Kind<Task_, A> value,
                  Function1<A, ? extends Kind<Task_, B>> map) {
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
  default <A> Task<A>
          handleErrorWith(Kind<Task_, A> value,
                          Function1<Throwable, ? extends Kind<Task_, A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<Throwable, Task<A>> mapError = handler.andThen(TaskOf::narrowK);
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
          defer(Producer<Kind<Task_, A>> defer) {
    return Task.defer(() -> defer.map(TaskOf::narrowK).get());
  }
}

interface TaskBracket extends TaskMonadError, Bracket<Task_, Throwable> {

  @Override
  default <A, B> Task<B>
          bracket(Kind<Task_, A> acquire,
                  Function1<A, ? extends Kind<Task_, B>> use,
                  Consumer1<A> release) {
    return Task.bracket(acquire.fix(TaskOf::narrowK), use.andThen(TaskOf::narrowK), release);
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
