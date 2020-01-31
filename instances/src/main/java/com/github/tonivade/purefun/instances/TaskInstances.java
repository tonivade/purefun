/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;
import com.github.tonivade.purefun.effect.Task;

import java.time.Duration;

public interface TaskInstances {

  static Functor<Task.µ> functor() {
    return TaskFunctor.instance();
  }

  static Applicative<Task.µ> applicative() {
    return TaskApplicative.instance();
  }

  static Monad<Task.µ> monad() {
    return TaskMonad.instance();
  }

  static MonadError<Task.µ, Throwable> monadError() {
    return TaskMonadError.instance();
  }

  static MonadThrow<Task.µ> monadThrow() {
    return TaskMonadThrow.instance();
  }

  static MonadDefer<Task.µ> monadDefer() {
    return TaskMonadDefer.instance();
  }

  static <A> Reference<Task.µ, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }
}

@Instance
interface TaskFunctor extends Functor<Task.µ> {

  @Override
  default <A, B> Higher1<Task.µ, B>
          map(Higher1<Task.µ, A> value, Function1<A, B> map) {
    return Task.narrowK(value).map(map).kind1();
  }
}

interface TaskPure extends Applicative<Task.µ> {

  @Override
  default <A> Higher1<Task.µ, A> pure(A value) {
    return Task.pure(value).kind1();
  }
}

@Instance
interface TaskApplicative extends TaskPure {

  @Override
  default <A, B> Higher1<Task.µ, B>
          ap(Higher1<Task.µ, A> value,
             Higher1<Task.µ, Function1<A, B>> apply) {
    return Task.narrowK(apply).flatMap(map -> Task.narrowK(value).map(map)).kind1();
  }
}

@Instance
interface TaskMonad extends TaskPure, Monad<Task.µ> {

  @Override
  default <A, B> Higher1<Task.µ, B>
          flatMap(Higher1<Task.µ, A> value,
                  Function1<A, ? extends Higher1<Task.µ, B>> map) {
    return Task.narrowK(value).flatMap(map.andThen(Task::narrowK)).kind1();
  }
}

@Instance
interface TaskMonadError extends TaskMonad, MonadError<Task.µ, Throwable> {

  @Override
  default <A> Higher1<Task.µ, A> raiseError(Throwable error) {
    return Task.<A>raiseError(error).kind1();
  }

  @Override
  default <A> Higher1<Task.µ, A>
          handleErrorWith(Higher1<Task.µ, A> value,
                          Function1<Throwable, ? extends Higher1<Task.µ, A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<Throwable, Task<A>> mapError = handler.andThen(Task::narrowK);
    Function1<A, Task<A>> map = Task::pure;
    Task<A> task = Task.narrowK(value);
    return task.foldM(mapError, map).kind1();
  }
}

@Instance
interface TaskMonadThrow
    extends TaskMonadError,
            MonadThrow<Task.µ> { }

interface TaskDefer extends Defer<Task.µ> {

  @Override
  default <A> Higher1<Task.µ, A>
          defer(Producer<Higher1<Task.µ, A>> defer) {
    return Task.defer(() -> defer.map(Task::narrowK).get()).kind1();
  }
}

interface TaskBracket extends Bracket<Task.µ> {

  @Override
  default <A, B> Higher1<Task.µ, B>
          bracket(Higher1<Task.µ, A> acquire,
                  Function1<A, ? extends Higher1<Task.µ, B>> use,
                  Consumer1<A> release) {
    return Task.bracket(acquire.fix1(Task::narrowK), use.andThen(Task::narrowK), release).kind1();
  }
}

@Instance
interface TaskMonadDefer
    extends MonadDefer<Task.µ>, TaskMonadThrow, TaskDefer, TaskBracket {
  @Override
  default Higher1<Task.µ, Unit> sleep(Duration duration) {
    return Task.sleep(duration).kind1();
  }
}
