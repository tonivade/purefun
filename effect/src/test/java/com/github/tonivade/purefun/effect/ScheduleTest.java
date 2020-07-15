/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.Unit.unit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Either;

@ExtendWith(MockitoExtension.class)
public class ScheduleTest {

  @Test
  public void repeat(@Mock Consumer1<String> console) {
    ZIO<Nothing, Throwable, Unit> print = ZIO.exec(() -> console.accept("hola"));
    ZIO<Nothing, Throwable, Unit> repeat = print.repeat(Schedule.<Nothing, Unit>recurs(2).zipRight(Schedule.identity()));
    
    Either<Throwable, Unit> provide = repeat.provide(nothing());
    
    assertEquals(Either.right(Unit.unit()), provide);
    verify(console, times(3)).accept("hola");
  }

  @Test
  public void repeatDelay(@Mock Consumer1<String> console) {
    ZIO<Nothing, Throwable, Unit> print = ZIO.exec(() -> console.accept("hola"));
    Schedule<Nothing, Tuple2<Integer, Unit>, Unit, Unit> recurs = Schedule.<Nothing, Unit>recurs(2).zipRight(Schedule.identity());
    Schedule<Nothing, Integer, Unit, Integer> spaced = Schedule.spaced(Duration.ofMillis(500));
    ZIO<Nothing, Throwable, Tuple2<Duration, Unit>> timed = print.repeat(recurs.zipLeft(spaced)).timed();
    
    Either<Throwable, Tuple2<Duration, Unit>> provide = timed.provide(nothing());
    
    assertTrue(provide.map(Tuple2::get1).getRight().toMillis() > 1000);
    verify(console, times(3)).accept("hola");
  }
  
  @Test
  public void noRepeat(@Mock Consumer1<String> console) {
    ZIO<Nothing, Throwable, Unit> print = ZIO.exec(() -> console.accept("hola"));
    ZIO<Nothing, Throwable, Unit> repeat = print.repeat(Schedule.never());
    
    Either<Throwable, Unit> provide = repeat.provide(nothing());
    
    assertEquals(Either.right(unit()), provide);
    verify(console).accept("hola");
  }

  @Test
  public void retry(@Mock Producer<String> console) {
    when(console.get()).thenThrow(RuntimeException.class).thenReturn("hola");

    ZIO<Nothing, Throwable, String> read = ZIO.task(console::get);
    ZIO<Nothing, Throwable, String> retry = read.retry(Schedule.recurs(1));
    
    Either<Throwable, String> provide = retry.provide(nothing());
    
    assertEquals(Either.right("hola"), provide);
    verify(console, times(2)).get();
  }

  @Test
  public void retryDelay(@Mock Producer<String> console) {
    when(console.get()).thenThrow(RuntimeException.class).thenReturn("hola");

    ZIO<Nothing, Throwable, String> read = ZIO.task(console::get);
    Schedule<Nothing, Integer, Throwable, Integer> recurs = Schedule.recurs(2);
    Schedule<Nothing, Integer, Throwable, Integer> spaced = Schedule.spaced(Duration.ofMillis(500));
    ZIO<Nothing, Throwable, Tuple2<Duration, String>> retry = read.retry(recurs.zip(spaced)).timed();
    
    Either<Throwable, Tuple2<Duration, String>> provide = retry.provide(nothing());
    
    assertTrue(provide.map(Tuple2::get1).getRight().toMillis() > 500);
    assertEquals(Either.right("hola"), provide.map(Tuple2::get2));
    verify(console, times(2)).get();
  }
  
  @Test
  public void noRetry(@Mock Producer<String> console) {
    when(console.get()).thenThrow(RuntimeException.class).thenReturn("hola");
    
    ZIO<Nothing, Throwable, String> read = ZIO.task(console::get);
    ZIO<Nothing, Throwable, String> retry = read.retry(Schedule.never());
    
    Either<Throwable, String> provide = retry.provide(nothing());
    
    assertTrue(provide.isLeft());
  }
  
  @Test
  public void andThen(@Mock Consumer1<String> console) {
    Schedule<Nothing, Either<Integer, Integer>, Unit, Integer> two = 
        Schedule.<Nothing, Unit>recurs(1).andThen(Schedule.<Nothing, Unit>recurs(1));

    ZIO<Nothing, Throwable, Unit> print = ZIO.exec(() -> console.accept("hola"));
    ZIO<Nothing, Throwable, Integer> repeat = print.repeat(two);
    
    Either<Throwable, Integer> provide = repeat.provide(nothing());
    
    assertEquals(Either.right(1), provide);
    verify(console, times(3)).accept("hola");
  }
  
  @Test
  @Disabled("I don't understand very well this")
  public void compose(@Mock Consumer1<String> console) {
    Schedule<Nothing, Tuple2<Integer, Integer>, Unit, Integer> two = 
      Schedule.<Nothing, Unit>recurs(1).compose(Schedule.<Nothing, Integer>recurs(1));

    ZIO<Nothing, Throwable, Unit> print = ZIO.exec(() -> console.accept("hola"));
    ZIO<Nothing, Throwable, Integer> repeat = print.repeat(two);
    
    Either<Throwable, Integer> provide = repeat.provide(nothing());
    
    assertEquals(Either.right(1), provide);
    verify(console, times(3)).accept("hola");
  }
}
