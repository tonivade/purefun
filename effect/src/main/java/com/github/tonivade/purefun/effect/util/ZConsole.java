/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect.util;

import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.effect.ZIO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Queue;

public interface ZConsole {

  <R extends ZConsole> ZConsole.Service<R> console();

  static <R extends ZConsole> ZIO<R, Throwable, String> readln() {
    return ZIO.accessM(env -> env.<R>console().readln());
  }

  static <R extends ZConsole> ZIO<R, Throwable, Unit> println(String text) {
    return ZIO.accessM(env -> env.<R>console().println(text));
  }

  interface Service<R extends ZConsole> {
    ZIO<R, Throwable, String> readln();

    ZIO<R, Throwable, Unit> println(String text);
  }

  static ZConsole test(final Queue<String> input, final Queue<String> output) {
    return new ZConsole() {

      @Override
      public <R extends ZConsole> Service<R> console() {
        return new ZConsole.Service<R>() {

          @Override
          public ZIO<R, Throwable, String> readln() {
            return ZIO.task(input::poll);
          }

          @Override
          public ZIO<R, Throwable, Unit> println(String text) {
            return ZIO.exec(() -> output.offer(text));
          }
        };
      }
    };
  }

  static ZConsole live() {
    return new ZConsole() {

      @Override
      public <R extends ZConsole> Service<R> console() {
        return new ZConsole.Service<R>() {

          @Override
          public ZIO<R, Throwable, String> readln() {
            return ZIO.task(() -> reader().readLine());
          }

          @Override
          public ZIO<R, Throwable, Unit> println(String text) {
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
