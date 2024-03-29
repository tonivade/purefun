/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Queue;

import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.effect.RIO;

public interface PureConsole {

  <R extends PureConsole> PureConsole.Service<R> console();

  static <R extends PureConsole> RIO<R, String> readln() {
    return RIO.accessM(env -> env.<R>console().readln());
  }

  static <R extends PureConsole> RIO<R, Unit> println(String text) {
    return RIO.accessM(env -> env.<R>console().println(text));
  }

  interface Service<R extends PureConsole> {
    RIO<R, String> readln();

    RIO<R, Unit> println(String text);
  }

  static PureConsole test(final Queue<String> input, final Queue<String> output) {
    return new PureConsole() {

      @Override
      public <R extends PureConsole> Service<R> console() {
        return new PureConsole.Service<>() {

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

  static PureConsole live() {
    return new PureConsole() {

      @Override
      public <R extends PureConsole> Service<R> console() {
        return new PureConsole.Service<>() {

          @Override
          public RIO<R, String> readln() {
            return RIO.task(() -> reader().readLine());
          }

          @Override
          public RIO<R, Unit> println(String text) {
            return RIO.exec(() -> writer().println(text));
          }

          private BufferedReader reader() {
            return new BufferedReader(new InputStreamReader(System.in, UTF_8));
          }

          private PrintWriter writer() {
            return new PrintWriter(System.out, true, UTF_8);
          }
        };
      }
    };
  }
}
