/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

final class DefaultExecutor {

  static final Executor EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

}
