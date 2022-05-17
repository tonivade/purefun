/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.effect.PureIOOf.toPureIO;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.effect.PureIO;
import com.github.tonivade.purefun.effect.PureIO_;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.type.Either;

public class ConcurrentTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(3);

  @Test
  public void ioRaceA() {
    Concurrent<IO_> concurrent = Instances.<IO_>concurrent();
    
    Kind<IO_, Either<Integer, String>> race = concurrent.race(
        IO.delay(Duration.ofMillis(10), () -> 10),
        IO.delay(Duration.ofMillis(100), () -> "b"));
    
    Either<Integer, String> orElseThrow = race.fix(toIO()).runAsync().await(TIMEOUT).getOrElseThrow();
    
    assertEquals(Either.left(10), orElseThrow);
  }

  @Test
  public void ioRaceB() {
    Concurrent<IO_> concurrent = Instances.<IO_>concurrent();
    
    Kind<IO_, Either<Integer, String>> race = concurrent.race(
        IO.delay(Duration.ofMillis(100), () -> 10),
        IO.delay(Duration.ofMillis(10), () -> "b"));
    
    Either<Integer, String> orElseThrow = race.fix(toIO()).runAsync().await(TIMEOUT).getOrElseThrow();
    
    assertEquals(Either.right("b"), orElseThrow);
  }

  @Test
  public void PureIORaceA() {
    Concurrent<Kind<Kind<PureIO_, Nothing>, Throwable>> concurrent = 
        new Instance<Kind<Kind<PureIO_, Nothing>, Throwable>>() { }.concurrent();
    
    Kind<Kind<Kind<PureIO_, Nothing>, Throwable>, Either<Integer, String>> race = concurrent.race(
        PureIO.<Nothing, Throwable>sleep(Duration.ofMillis(10)).andThen(PureIO.task(() -> 10)),
        PureIO.<Nothing, Throwable>sleep(Duration.ofMillis(100)).andThen(PureIO.task(() -> "b")));
    
    Either<Throwable, Either<Integer, String>> orElseThrow = 
        race.fix(toPureIO()).runAsync(nothing()).await(TIMEOUT).getOrElseThrow();
    
    assertEquals(Either.right(Either.left(10)), orElseThrow);
  }

  @Test
  public void PureIORaceB() {
    Concurrent<Kind<Kind<PureIO_, Nothing>, Throwable>> concurrent = 
        new Instance<Kind<Kind<PureIO_, Nothing>, Throwable>>() { }.concurrent();
    
    Kind<Kind<Kind<PureIO_, Nothing>, Throwable>, Either<Integer, String>> race = concurrent.race(
        PureIO.<Nothing, Throwable>sleep(Duration.ofMillis(100)).andThen(PureIO.task(() -> 10)),
        PureIO.<Nothing, Throwable>sleep(Duration.ofMillis(10)).andThen(PureIO.task(() -> "b")));
    
    Either<Throwable, Either<Integer, String>> orElseThrow = 
        race.fix(toPureIO()).runAsync(nothing()).await(TIMEOUT).getOrElseThrow();
    
    assertEquals(Either.right(Either.right("b")), orElseThrow);
  }
}
