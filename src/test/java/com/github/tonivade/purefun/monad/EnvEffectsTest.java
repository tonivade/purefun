/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.monad.Console.println;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Nothing;

public class EnvEffectsTest {

  @Test
  public void program() {
    ConsoleExecutor executor = new ConsoleExecutor().read("Toni");

    executor.run(echo().provide(Console.live()));

    assertEquals("what's your name?\nHello Toni\n", executor.getOutput());
  }

  private ZIO<Console, Throwable, Nothing> echo() {
    return println("what's your name?")
        .andThen(Console::readln)
        .flatMap(name -> println("Hello " + name));
  }
}

interface Console {

  <R extends Console> Console.Service<R> console();

  static ZIO<Console, Throwable, String> readln() {
    return ZIO.accessM(env -> env.console().readln());
  }

  static ZIO<Console, Throwable, Nothing> println(String text) {
    return ZIO.accessM(env -> env.console().println(text));
  }

  interface Service<R extends Console> {
    ZIO<R, Throwable, String> readln();

    ZIO<R, Throwable, Nothing> println(String text);
  }

  static Console live() {
    return new Console() {
      @Override
      public <R extends Console> Service<R> console() {
        return new Console.Service<R>() {

          @Override
          public ZIO<R, Throwable, String> readln() {
            return ZIO.from(() -> reader().readLine());
          }

          @Override
          public ZIO<R, Throwable, Nothing> println(String text) {
            return ZIO.exec(() -> writer().println(text));
          }

          private BufferedReader reader() {
            return new BufferedReader(new InputStreamReader(System.in));
          }

          private PrintWriter writer() {
            return new PrintWriter(System.out, true);
          }
        };
      }
    };
  }
}

