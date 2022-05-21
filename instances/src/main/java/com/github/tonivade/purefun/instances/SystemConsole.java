/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

final class SystemConsole {
  
  protected void println(String message) {
    writer(System.out).println(message);
  }

  protected String readln() {
    try {
      return reader(System.in).readLine();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static BufferedReader reader(InputStream stream) {
    return new BufferedReader(new InputStreamReader(stream));
  }

  private static PrintWriter writer(PrintStream stream) {
    return new PrintWriter(stream, true);
  }
}