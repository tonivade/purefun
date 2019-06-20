package com.github.tonivade.purefun.runtimes;

import static com.github.tonivade.purefun.instances.FutureInstances.monadDefer;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.Executor;

import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IORuntime;

public class FutureIORuntime implements IORuntime<Future.µ> {

  private final Executor executor;

  public FutureIORuntime() {
    this(Future.DEFAULT_EXECUTOR);
  }

  public FutureIORuntime(Executor executor) {
    this.executor = requireNonNull(executor);
  }

  @Override
  public <A> Future<A> run(IO<A> effect) {
    return effect.foldMap(monadDefer(executor)).fix1(Future::narrowK);
  }
}