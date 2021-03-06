/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.effect.util.ZConsole;
import com.github.tonivade.purefun.runtimes.ConsoleExecutor;

public class EnvEffectsTest {

  @Test
  public void programLive() {
    ConsoleExecutor executor = new ConsoleExecutor().read("Toni");

    executor.run(echo()).apply(ZConsole.live());

    assertEquals("what's your name?\nHello Toni\n", executor.getOutput());
  }

  @Test
  public void programTest() {
    Queue<String> input = new LinkedList<>(asList("Toni"));
    Queue<String> output = new LinkedList<>();

    echo().safeRunSync(ZConsole.test(input, output));

    assertEquals(asList("what's your name?", "Hello Toni"), output);
  }

  private RIO<ZConsole, Unit> echo() {
    return ZConsole.println("what's your name?")
        .andThen(ZConsole.readln())
        .flatMap(name -> ZConsole.println("Hello " + name));
  }
}
