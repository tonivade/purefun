/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio;

import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.runtimes.ConsoleExecutor;
import com.github.tonivade.purefun.zio.util.ZConsole;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.Queue;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnvEffectsTest {

  @Test
  public void programLive() {
    ConsoleExecutor executor = new ConsoleExecutor().read("Toni");

    executor.run(IO.task(() -> echo().provide(ZConsole.live())));

    assertEquals("what's your name?\nHello Toni\n", executor.getOutput());
  }

  @Test
  public void programTest() {
    Queue<String> input = new LinkedList<>(asList("Toni"));
    Queue<String> output = new LinkedList<>();

    echo().provide(ZConsole.test(input, output));

    assertEquals(asList("what's your name?", "Hello Toni"), output);
  }

  private ZIO<ZConsole, Throwable, Unit> echo() {
    return ZConsole.println("what's your name?")
        .andThen(ZConsole.readln())
        .flatMap(name -> ZConsole.println("Hello " + name));
  }
}
