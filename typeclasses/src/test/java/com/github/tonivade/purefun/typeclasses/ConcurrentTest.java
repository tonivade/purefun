/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.effect.ZIOOf.toZIO;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
import static com.github.tonivade.purefun.typeclasses.Instance.concurrent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.effect.ZIO;
import com.github.tonivade.purefun.effect.ZIO_;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.type.Either;

public class ConcurrentTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(3);

  @Test
  public void ioRaceA() {
    Concurrent<IO_> concurrent = concurrent(IO_.class);
    
    Kind<IO_, Either<Integer, String>> race = concurrent.race(
        IO.delay(Duration.ofMillis(10), () -> 10),
        IO.delay(Duration.ofMillis(100), () -> "b"));
    
    Either<Integer, String> orElseThrow = race.fix(toIO()).runAsync().await(TIMEOUT).getOrElseThrow();
    
    assertEquals(Either.left(10), orElseThrow);
  }

  @Test
  public void ioRaceB() {
    Concurrent<IO_> concurrent = concurrent(IO_.class);
    
    Kind<IO_, Either<Integer, String>> race = concurrent.race(
        IO.delay(Duration.ofMillis(100), () -> 10),
        IO.delay(Duration.ofMillis(10), () -> "b"));
    
    Either<Integer, String> orElseThrow = race.fix(toIO()).runAsync().await(TIMEOUT).getOrElseThrow();
    
    assertEquals(Either.right("b"), orElseThrow);
  }

  @Test
  public void zioRaceA() {
    Concurrent<Kind<Kind<ZIO_, Nothing>, Throwable>> concurrent = 
        new Instance<Kind<Kind<ZIO_, Nothing>, Throwable>>() { }.concurrent();
    
    Kind<Kind<Kind<ZIO_, Nothing>, Throwable>, Either<Integer, String>> race = concurrent.race(
        ZIO.<Nothing, Throwable>sleep(Duration.ofMillis(10)).andThen(ZIO.task(() -> 10)),
        ZIO.<Nothing, Throwable>sleep(Duration.ofMillis(100)).andThen(ZIO.task(() -> "b")));
    
    Either<Throwable, Either<Integer, String>> orElseThrow = 
        race.fix(toZIO()).runAsync(nothing()).await(TIMEOUT).getOrElseThrow();
    
    assertEquals(Either.right(Either.left(10)), orElseThrow);
  }

  @Test
  public void zioRaceB() {
    Concurrent<Kind<Kind<ZIO_, Nothing>, Throwable>> concurrent = 
        new Instance<Kind<Kind<ZIO_, Nothing>, Throwable>>() { }.concurrent();
    
    Kind<Kind<Kind<ZIO_, Nothing>, Throwable>, Either<Integer, String>> race = concurrent.race(
        ZIO.<Nothing, Throwable>sleep(Duration.ofMillis(100)).andThen(ZIO.task(() -> 10)),
        ZIO.<Nothing, Throwable>sleep(Duration.ofMillis(10)).andThen(ZIO.task(() -> "b")));
    
    Either<Throwable, Either<Integer, String>> orElseThrow = 
        race.fix(toZIO()).runAsync(nothing()).await(TIMEOUT).getOrElseThrow();
    
    assertEquals(Either.right(Either.right("b")), orElseThrow);
  }
}
