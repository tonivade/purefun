/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

final class SystemConsole {

  void println(String message) {
    writer(System.out).println(message);
  }

  String readln() {
    try {
      return reader(System.in).readLine();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static BufferedReader reader(InputStream stream) {
    return new BufferedReader(new InputStreamReader(stream, UTF_8));
  }

  private static PrintWriter writer(PrintStream stream) {
    return new PrintWriter(stream, true, UTF_8);
  }
}