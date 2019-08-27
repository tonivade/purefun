/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.runtimes;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import com.github.tonivade.purefun.monad.IO;

public final class ConsoleExecutor {

  private final StringBuilder input = new StringBuilder();
  private final ByteArrayOutputStream output = new ByteArrayOutputStream();
  
  public ConsoleExecutor read(String string) {
    input.append(string).append('\n');
    return this;
  }
  
  public String getOutput() {
    return new String(output.toByteArray(), UTF_8);
  }
  
  public <T> T run(IO<T> program) {
    InputStream savedInput = System.in;
    PrintStream savedOutput = System.out;
    try {
      System.setIn(mockInput());
      System.setOut(mockOutput());
      return program.unsafeRunSync();
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
