/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.typeclasses.Instance.concurrent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.FutureOf;
import com.github.tonivade.purefun.concurrent.Future_;
import com.github.tonivade.purefun.type.Either;

public class ConcurrentTest {

  @Test
  public void raceA() {
    Concurrent<Future_> concurrent = concurrent(Future_.class);
    
    Kind<Future_, Either<Integer, String>> race = concurrent.race(
        Future.delay(Duration.ofMillis(10), () -> 10),
        Future.delay(Duration.ofMillis(100), () -> "b"));
    
    Either<Integer, String> orElseThrow = race.fix(FutureOf.toFuture()).await().getOrElseThrow();
    
    assertEquals(Either.left(10), orElseThrow);
  }

  @Test
  public void raceB() {
    Concurrent<Future_> concurrent = concurrent(Future_.class);
    
    Kind<Future_, Either<Integer, String>> race = concurrent.race(
        Future.delay(Duration.ofMillis(100), () -> 10),
        Future.delay(Duration.ofMillis(10), () -> "b"));
    
    Either<Integer, String> orElseThrow = race.fix(FutureOf.toFuture()).await().getOrElseThrow();
    
    assertEquals(Either.right("b"), orElseThrow);
  }
}
