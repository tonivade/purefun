/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.effect.util.PureConsole;
import com.github.tonivade.purefun.runtimes.ConsoleExecutor;

public class EnvEffectsTest {

  @Test
  public void programLive() {
    ConsoleExecutor executor = new ConsoleExecutor().read("Toni");

    executor.run(echo()).apply(PureConsole.live());

    assertEquals("what's your name?\nHello Toni\n", executor.getOutput());
  }

  @Test
  public void programTest() {
    Queue<String> input = new LinkedList<>(asList("Toni"));
    Queue<String> output = new LinkedList<>();

    echo().safeRunSync(PureConsole.test(input, output));

    assertEquals(asList("what's your name?", "Hello Toni"), output);
  }

  private RIO<PureConsole, Unit> echo() {
    return PureConsole.println("what's your name?")
        .andThen(PureConsole.readln())
        .flatMap(name -> PureConsole.println("Hello " + name));
  }
}
