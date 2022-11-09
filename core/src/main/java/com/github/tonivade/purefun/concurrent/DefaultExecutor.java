package com.github.tonivade.purefun.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

final class DefaultExecutor {

  static final Executor EXECUTOR = Executors.newCachedThreadPool();
}
