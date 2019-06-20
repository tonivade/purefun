/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.runtimes;

import static com.github.tonivade.purefun.instances.FutureInstances.monadDefer;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.Executor;

import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.zio.ZIO;
import com.github.tonivade.purefun.zio.ZIORuntime;

public class FutureZIORuntime implements ZIORuntime<Future.µ> {

  private Executor executor;

  public FutureZIORuntime() {
    this(Future.DEFAULT_EXECUTOR);
  }

  public FutureZIORuntime(Executor executor) {
    this.executor = requireNonNull(executor);
  }

  @Override
  public <R, E, A> Future<Either<E, A>> run(R env, ZIO<R, E, A> effect) {
    return effect.foldMap(env, monadDefer(executor)).fix1(Future::narrowK);
  }
}
