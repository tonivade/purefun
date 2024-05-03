/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Unit.unit;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.UIOOf;
import com.github.tonivade.purefun.monad.IO;

@ExtendWith(MockitoExtension.class)
public class ScheduleTest {

  private final MonadError<IO<?>, Throwable> monadError = Instances.<IO<?>, Throwable>monadError();

  @Test
  public void repeat(@Mock Consumer1<String> console) {
    IO<Unit> print = IO.exec(() -> console.accept("hola"));
    Schedule<IO<?>, Unit, Unit> schedule = Schedule.<IO<?>, Unit>recurs(2).zipRight(Schedule.identity());

    IO<Unit> repeat = monadError.repeat(print, schedule).fix(toIO());

    Unit result = repeat.unsafeRunSync();

    assertEquals(unit(), result);
    verify(console, times(3)).accept("hola");
  }

  @Test
  public void repeatStackSafeIO(@Mock Consumer1<String> console) {
    IO<Unit> print = IO.exec(() -> console.accept("hola"));
    Schedule<IO<?>, Unit, Unit> schedule = Schedule.<IO<?>, Unit>recurs(10000).zipRight(Schedule.identity());

    IO<Unit> repeat = monadError.repeat(print, schedule).fix(toIO());

    Unit result = repeat.unsafeRunSync();

    assertEquals(unit(), result);
    verify(console, times(10001)).accept("hola");
  }

  @Test
  public void repeatStackSafeUIO(@Mock Consumer1<String> console) {
    UIO<Unit> print = UIO.exec(() -> console.accept("hola"));
    Schedule<UIO<?>, Unit, Unit> schedule = Schedule.<UIO<?>, Unit>recurs(10000).zipRight(Schedule.identity());

    UIO<Unit> repeat = Instances.<UIO<?>, Throwable>monadError().repeat(print, schedule).fix(UIOOf.toUIO());

    Unit result = repeat.unsafeRunSync();

    assertEquals(unit(), result);
    verify(console, times(10001)).accept("hola");
  }

  @Test
  public void repeatDelay(@Mock Consumer1<String> console) {
    IO<Unit> print = IO.exec(() -> console.accept("hola"));
    Schedule<IO<?>, Unit, Unit> recurs = Schedule.<IO<?>, Unit>recurs(2).zipRight(Schedule.identity());
    Schedule<IO<?>, Unit, Integer> spaced = Schedule.spaced(Duration.ofMillis(500));
    Schedule<IO<?>, Unit, Unit> schedule = recurs.zipLeft(spaced);

    IO<Unit> repeat = monadError.repeat(print, schedule).fix(toIO());
    IO<Tuple2<Duration, Unit>> timed = repeat.timed();

    Tuple2<Duration, Unit> result = timed.unsafeRunSync();

    assertTrue(result.get1().toMillis() > 1000);
    verify(console, times(3)).accept("hola");
  }

  @Test
  public void noRepeat(@Mock Consumer1<String> console) {
    IO<Unit> print = IO.exec(() -> console.accept("hola"));
    IO<Unit> repeat = monadError.repeat(print, Schedule.never()).fix(toIO());

    Unit result = repeat.unsafeRunSync();

    assertEquals(unit(), result);
    verify(console).accept("hola");
  }

  @Test
  public void retry(@Mock Producer<String> console) {
    when(console.get())
      .thenThrow(RuntimeException.class)
      .thenReturn("hola");

    IO<String> read = IO.task(console::get);
    IO<String> retry = monadError.retry(read, Schedule.recurs(1)).fix(toIO());

    String provide = retry.unsafeRunSync();

    assertEquals("hola", provide);
    verify(console, times(2)).get();
  }

  @Test
  public void retryDelay(@Mock Producer<String> console) {
    when(console.get()).thenThrow(RuntimeException.class).thenReturn("hola");

    IO<String> read = IO.task(console::get);
    Schedule<IO<?>, Throwable, Integer> recurs = Schedule.recurs(2);
    Schedule<IO<?>, Throwable, Integer> spaced = Schedule.spaced(Duration.ofMillis(500));
    IO<Tuple2<Duration, String>> retry = monadError.retry(read, recurs.zip(spaced)).fix(toIO()).timed();

    Tuple2<Duration, String> result = retry.unsafeRunSync();

    assertTrue(result.get1().toMillis() > 500);
    assertEquals("hola", result.get2());
    verify(console, times(2)).get();
  }

  @Test
  public void noRetry(@Mock Producer<String> console) {
    when(console.get()).thenThrow(UnsupportedOperationException.class).thenReturn("hola");

    IO<String> read = IO.task(console::get);
    IO<String> retry = monadError.retry(read, Schedule.never()).fix(toIO());

    assertThrows(UnsupportedOperationException.class, retry::unsafeRunSync);
  }

  @Test
  public void andThen(@Mock Consumer1<String> console) {
    Schedule<IO<?>, Unit, Integer> two =
        Schedule.<IO<?>, Unit>recurs(1).andThen(Schedule.<IO<?>, Unit>recurs(1));

    IO<Unit> print = IO.exec(() -> console.accept("hola"));
    IO<Integer> repeat = monadError.repeat(print, two).fix(toIO());

    Integer provide = repeat.unsafeRunSync();

    assertEquals(1, provide);
    verify(console, times(3)).accept("hola");
  }

  @Test
  public void compose(@Mock Consumer1<String> console) {
    Schedule<IO<?>, Unit, Integer> two =
      Schedule.<IO<?>, Unit>recurs(2).compose(Schedule.<IO<?>, Integer>recurs(2));

    IO<Unit> print = IO.exec(() -> console.accept("hola"));
    IO<Integer> repeat = monadError.repeat(print, two).fix(toIO());

    Integer provide = repeat.unsafeRunSync();

    assertEquals(2, provide);
    verify(console, times(3)).accept("hola");
  }

  @Test
  public void collect() {
    IO<Unit> pure = IO.unit();

    Schedule<IO<?>, Unit, Sequence<Integer>> schedule = Schedule.<IO<?>, Unit>recurs(5).collectAll().zipLeft(Schedule.identity());
    IO<Sequence<Integer>> repeat = monadError.repeat(pure, schedule).fix(toIO());

    Sequence<Integer> result = repeat.unsafeRunSync();

    assertEquals(listOf(0, 1, 2, 3, 4), result);
  }
}
