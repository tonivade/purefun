/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.typeclasses.Instance.concurrent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.FutureOf;
import com.github.tonivade.purefun.concurrent.Future_;
import com.github.tonivade.purefun.concurrent.Par;
import com.github.tonivade.purefun.concurrent.ParOf;
import com.github.tonivade.purefun.concurrent.Par_;
import com.github.tonivade.purefun.type.Either;

@Disabled
public class ConcurrentTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(3);

  @Test
  public void raceA() {
    Concurrent<Future_> concurrent = concurrent(Future_.class);
    
    Kind<Future_, Either<Integer, String>> race = concurrent.race(
        Future.delay(Duration.ofMillis(10), () -> 10),
        Future.delay(Duration.ofMillis(100), () -> "b"));
    
    Either<Integer, String> orElseThrow = race.fix(FutureOf.toFuture()).await(TIMEOUT).getOrElseThrow();
    
    assertEquals(Either.left(10), orElseThrow);
  }

  @Test
  public void raceB() {
    Concurrent<Future_> concurrent = concurrent(Future_.class);
    
    Kind<Future_, Either<Integer, String>> race = concurrent.race(
        Future.delay(Duration.ofMillis(100), () -> 10),
        Future.delay(Duration.ofMillis(10), () -> "b"));
    
    Either<Integer, String> orElseThrow = race.fix(FutureOf.toFuture()).await(TIMEOUT).getOrElseThrow();
    
    assertEquals(Either.right("b"), orElseThrow);
  }

  @Test
  public void parRaceA() {
    Concurrent<Par_> concurrent = concurrent(Par_.class);
    
    Kind<Par_, Either<Integer, String>> race = concurrent.race(
        Par.delay(Duration.ofMillis(10), () -> 10),
        Par.delay(Duration.ofMillis(100), () -> "b"));
    
    Either<Integer, String> orElseThrow = race.fix(ParOf.toPar()).run().await().getOrElseThrow();
    
    assertEquals(Either.left(10), orElseThrow);
  }

  @Test
  public void parRaceB() {
    Concurrent<Par_> concurrent = concurrent(Par_.class);
   
    Kind<Par_, Either<Integer, String>> race = concurrent.race(
        Par.delay(Duration.ofMillis(100), () -> 10),
        Par.delay(Duration.ofMillis(10), () -> "b"));
    
    Either<Integer, String> orElseThrow = race.fix(ParOf.toPar()).run().await(TIMEOUT).getOrElseThrow();
    
    assertEquals(Either.right("b"), orElseThrow);
  }
}
