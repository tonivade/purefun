package com.github.tonivade.purefun.monad;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

public class ConsoleExecutor {

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
      System.setIn(new ByteArrayInputStream(input.toString().getBytes(UTF_8)));
      System.setOut(new PrintStream(output));
      return program.unsafeRunSync();
    } finally {
      System.setIn(savedInput);
      System.setOut(savedOutput);
    }
  }
}
