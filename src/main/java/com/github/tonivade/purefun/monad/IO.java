/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Nothing.nothing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Functor;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.data.Sequence;

@FunctionalInterface
public interface IO<T> extends Functor<T> {

  T unsafeRunSync();
  
  @Override
  default <R> IO<R> map(Function1<T, R> map) {
    return () -> map.apply(unsafeRunSync());
  }
  
  default <R> IO<R> flatMap(Function1<T, IO<R>> map) {
    return () -> map.apply(unsafeRunSync()).unsafeRunSync();
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
    return () -> producer.get();
  }
  
  static IO<Nothing> noop() {
    return unit(nothing());
  }
  
  static IO<Nothing> sequence(Sequence<IO<?>> sequence) {
    return sequence.fold(noop(), IO::andThen).andThen(noop());
  }
  
  final class Console {
    
    private static final ThreadLocal<Console> console = ThreadLocal.withInitial(Console::new);
    
    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private final PrintWriter writer = new PrintWriter(System.out);
    
    private Console() {}

    public static IO<Nothing> print(String message) {
      return exec(() -> console().println(message));
    }

    public static IO<String> read() {
      return IO.of(() -> console().readLine());
    }

    private static Console console() {
      return console.get();
    }

    private void println(String message) {
      writer.println(message);
      writer.flush();
    }

    private String readLine() {
      try {
        return reader.readLine();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
