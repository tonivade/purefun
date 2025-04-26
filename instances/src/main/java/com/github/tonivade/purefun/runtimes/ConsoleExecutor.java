/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.runtimes;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import com.github.tonivade.purefun.effect.RIO;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.URIO;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.effect.PureIO;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;

public final class ConsoleExecutor {

  private final StringBuilder input = new StringBuilder();
  private final ByteArrayOutputStream output = new ByteArrayOutputStream();

  public ConsoleExecutor read(String string) {
    input.append(string).append('\n');
    return this;
  }

  public String getOutput() {
    return output.toString(UTF_8);
  }

  public <R, E, T> Function1<R, Either<E, T>> run(PureIO<R, E, T> program) {
    return env -> run(() -> program.provide(env));
  }

  public <R, T> Function1<R, Try<T>> run(RIO<R, T> program) {
    return env -> run(() -> program.safeRunSync(env));
  }

  public <R, T> Function1<R, T> run(URIO<R, T> program) {
    return env -> run(() -> program.unsafeRunSync(env));
  }

  public <T> T run(IO<T> program) {
    return run(program::unsafeRunSync);
  }

  public <T> T run(UIO<T> program) {
    return run(program::unsafeRunSync);
  }

  public <T> Try<T> run(Task<T> program) {
    return run(program::safeRunSync);
  }

  public <T> T run(Producer<T> program) {
    InputStream savedInput = System.in;
    PrintStream savedOutput = System.out;
    try {
      System.setIn(mockInput());
      System.setOut(mockOutput());
      return program.get();
    } finally {
      System.setIn(savedInput);
      System.setOut(savedOutput);
    }
  }

  private PrintStream mockOutput() {
    return new PrintStream(output);
  }

  private ByteArrayInputStream mockInput() {
    return new ByteArrayInputStream(input.toString().getBytes(UTF_8));
  }
}
