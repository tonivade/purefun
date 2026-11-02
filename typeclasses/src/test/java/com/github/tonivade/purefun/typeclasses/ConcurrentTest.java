/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.type.Either;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class ConcurrentTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(3);

  @Test
  public void ioRaceA() {
    Concurrent<IO<?>> concurrent = Instances.concurrent();

    Kind<IO<?>, Either<Integer, String>> race = concurrent.race(
        IO.delay(Duration.ofMillis(10), () -> 10),
        IO.delay(Duration.ofMillis(100), () -> "b"));

    Either<Integer, String> orElseThrow = race.fix(IOOf::toIO).runAsync().await(TIMEOUT).getOrElseThrow();

    assertEquals(Either.left(10), orElseThrow);
  }

  @Test
  public void ioRaceB() {
    Concurrent<IO<?>> concurrent = Instances.concurrent();

    Kind<IO<?>, Either<Integer, String>> race = concurrent.race(
        IO.delay(Duration.ofMillis(100), () -> 10),
        IO.delay(Duration.ofMillis(10), () -> "b"));

    Either<Integer, String> orElseThrow = race.fix(IOOf::toIO).runAsync().await(TIMEOUT).getOrElseThrow();

    assertEquals(Either.right("b"), orElseThrow);
  }
}
