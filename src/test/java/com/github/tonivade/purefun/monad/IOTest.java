/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.monad.IO.ConsoleIO.println;
import static com.github.tonivade.purefun.monad.IO.ConsoleIO.readln;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Nothing;

public class IOTest {
  
  @Test
  public void unit() {
    IO<String> unit = IO.unit("hola mundo");

    assertAll(
        () -> assertEquals("hola mundo", unit.unsafeRunSync()),
        () -> assertEquals("HOLA MUNDO", unit.map(String::toUpperCase).unsafeRunSync()),
        () -> assertArrayEquals(new String[] { "hola", "mundo" },
            unit.flatMap(string -> IO.of(() -> string.split(" "))).unsafeRunSync()),
        () -> assertEquals(Integer.valueOf(100), unit.andThen(IO.of(() -> 100)).unsafeRunSync()));
  }
  
  @Test
  public void echo() {
    IO<Nothing> echo = println("write your name")
      .andThen(readln())
      .flatMap(name -> println("Hello " + name))
      .andThen(println("end"));

    ConsoleExecutor executor = new ConsoleExecutor().read("Toni");
    executor.run(echo);

    assertEquals("write your name\nHello Toni\nend\n", executor.getOutput());
  }
}
