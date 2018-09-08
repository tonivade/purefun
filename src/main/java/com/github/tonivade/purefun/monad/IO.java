/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.monad.Console.console;
import static com.github.tonivade.purefun.monad.IOKind.narrowK;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Monad;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.data.Sequence;

@FunctionalInterface
public interface IO<T> extends Monad<IOKind.µ, T> {

  T unsafeRunSync();

  @Override
  default <R> IO<R> map(Function1<T, R> map) {
    return () -> map.apply(unsafeRunSync());
  }

  @Override
  default <R> IO<R> flatMap(Function1<T, ? extends Higher<IOKind.µ, R>> map) {
    return () -> narrowK(map.apply(unsafeRunSync())).unsafeRunSync();
  }

  default <R> IO<R> andThen(IO<R> after) {
    return flatMap(ignore -> after);
  }

  static <T> IO<T> unit(T value) {
    return () -> value;
  }

  static IO<Nothing> exec(Runnable task) {
    return () -> { task.run(); return nothing(); };
  }

  static <T> IO<T> of(Producer<T> producer) {
    return producer::get;
  }

  static IO<Nothing> noop() {
    return unit(nothing());
  }

  static IO<Nothing> sequence(Sequence<IO<?>> sequence) {
    return sequence.fold(noop(), IO::andThen).andThen(noop());
  }

  final class ConsoleIO {

    private ConsoleIO() {}

    public static IO<Nothing> println(String message) {
      return exec(() -> console().println(message));
    }

    public static IO<String> readln() {
      return IO.of(() -> console().readln());
    }
  }
}

final class Console {
  private static final ThreadLocal<Console> current = ThreadLocal.withInitial(Console::new);

  private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
  private final PrintWriter writer = new PrintWriter(System.out);

  private Console() {}

  protected static Console console() {
    return current.get();
  }

  protected void println(String message) {
    writer.println(message);
    writer.flush();
  }

  protected String readln() {
    try {
      return reader.readLine();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}