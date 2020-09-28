/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Queue;

import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.effect.RIO;

public interface ZConsole {

  <R extends ZConsole> ZConsole.Service<R> console();

  static <R extends ZConsole> RIO<R, String> readln() {
    return RIO.accessM(env -> env.<R>console().readln());
  }

  static <R extends ZConsole> RIO<R, Unit> println(String text) {
    return RIO.accessM(env -> env.<R>console().println(text));
  }

  interface Service<R extends ZConsole> {
    RIO<R, String> readln();

    RIO<R, Unit> println(String text);
  }

  static ZConsole test(final Queue<String> input, final Queue<String> output) {
    return new ZConsole() {

      @Override
      public <R extends ZConsole> Service<R> console() {
        return new ZConsole.Service<R>() {

          @Override
          public RIO<R, String> readln() {
            return RIO.task(input::poll);
          }

          @Override
          public RIO<R, Unit> println(String text) {
            return RIO.exec(() -> output.offer(text));
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
          public RIO<R, String> readln() {
            return RIO.task(() -> reader().readLine());
          }

          @Override
          public RIO<R, Unit> println(String text) {
            return RIO.exec(() -> writer().println(text));
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
