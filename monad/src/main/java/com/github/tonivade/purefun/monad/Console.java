/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Unit.unit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableList;

public interface Console<F extends Kind> {

  Higher1<F, String> readln();

  Higher1<F, Unit> println(String text);

  static Console<IO.µ> io() {
    return new ConsoleIO();
  }

  static Console<Higher1<State.µ, ImmutableList<String>>> state() {
    return new ConsoleState();
  }
}

final class ConsoleState implements Console<Higher1<State.µ, ImmutableList<String>>> {

  @Override
  public State<ImmutableList<String>, String> readln() {
    return State.<ImmutableList<String>, String>state(list -> Tuple.of(list.tail(), list.head().get()));
  }

  @Override
  public State<ImmutableList<String>, Unit> println(String text) {
    return State.<ImmutableList<String>, Unit>state(list -> Tuple.of(list.append(text), unit()));
  }
}

final class ConsoleIO implements Console<IO.µ> {

  private final SystemConsole console = new SystemConsole();

  @Override
  public IO<String> readln() {
    return IO.task(console::readln);
  }

  @Override
  public IO<Unit> println(String text) {
    return IO.exec(() -> console.println(text));
  }
}

final class SystemConsole {

  void println(String message) {
    writer().println(message);
  }

  String readln() {
    try {
      return reader().readLine();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private BufferedReader reader() {
    return new BufferedReader(new InputStreamReader(System.in));
  }

  private PrintWriter writer() {
    return new PrintWriter(System.out, true);
  }
}