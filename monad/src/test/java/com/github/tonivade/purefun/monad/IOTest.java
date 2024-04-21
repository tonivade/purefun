/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.monad.IO.unit;
import static com.github.tonivade.purefun.monad.IOOf.narrowK;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.runtimes.ConsoleExecutor;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.Fiber;
import com.github.tonivade.purefun.typeclasses.Instances;
import com.github.tonivade.purefun.typeclasses.Reference;

@ExtendWith(MockitoExtension.class)
public class IOTest {

  private final Console<IO_> console = IOInstances.console();

  @Test
  public void pure() {
    IO<String> pure = IO.pure("hola mundo");

    assertAll(
        () -> assertEquals("hola mundo", pure.unsafeRunSync()),
        () -> assertEquals("HOLA MUNDO", pure.map(String::toUpperCase).unsafeRunSync()),
        () -> assertArrayEquals(new String[] { "hola", "mundo" },
            pure.flatMap(string -> IO.task(() -> string.split(" "))).unsafeRunSync()),
        () -> assertEquals(Integer.valueOf(100), pure.andThen(IO.task(() -> 100)).unsafeRunSync()));
  }

  @Test
  public void asyncSuccess() {
    IO<String> async = IO.async(callback -> {
      System.out.println(Thread.currentThread().threadId());
      Thread.sleep(100);
      callback.accept(Try.success("1"));
    });

    Future<String> foldMap = IO.forked().andThen(async).runAsync();

    assertEquals("1", foldMap.getOrElseThrow());
  }

  @Test
  public void asyncFailure() {
    IO<String> async = IO.async(callback -> {
      Thread.sleep(100);
      callback.accept(Try.failure(new UnsupportedOperationException()));
    });

    Future<String> foldMap = IO.forked().andThen(async).runAsync();

    assertThrows(UnsupportedOperationException.class, foldMap::getOrElseThrow);
  }

  @Test
  public void echo() {
    IO<Unit> echo = narrowK(console.println("write your name"))
        .andThen(narrowK(console.readln()))
        .flatMap(name -> narrowK(console.println("Hello " + name)))
        .andThen(narrowK(console.println("end")));

    ConsoleExecutor executor = new ConsoleExecutor().read("Toni");

    executor.run(echo);

    assertEquals("write your name\nHello Toni\nend\n", executor.getOutput());
  }

  @Test
  public void safeRunAsync() {
    IO<ImmutableList<String>> program = currentThreadIO();

    Try<ImmutableList<String>> result = program.runAsync().await(Duration.ofSeconds(1));

    assertEquals(Try.success(5), result.map(ImmutableList::size));
  }

  @Test
  public void bracket() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getString("id")).thenReturn("value");

    IO<String> bracket = IO.bracket(open(resultSet), IO.liftTry(tryGetString("id")));

    assertEquals("value", bracket.unsafeRunSync());
    verify(resultSet).close();
  }

  @Test
  public void safeRunAsyncSuccess(@Mock Consumer1<? super Try<? extends String>> callback) {
    IO.pure("hola").safeRunAsync(callback);

    verify(callback, timeout(1000)).accept(Try.success("hola"));
  }

  @Test
  public void unsafeRunAsyncFailure(@Mock Consumer1<? super Try<? extends String>> callback) {
    RuntimeException error = new RuntimeException();

    IO.<String>raiseError(error).safeRunAsync(callback);

    verify(callback, timeout(1000)).accept(Try.failure(error));
  }

  @Test
  public void recover() {
    IO<String> recover = IO.<String>raiseError(new RuntimeException()).recover(error -> "hola mundo");

    assertEquals("hola mundo", recover.unsafeRunSync());
  }

  @Test
  public void recoverWith() {
    IO<String> recover = IO.<String>raiseError(new IllegalArgumentException())
        .recover(IllegalArgumentException.class, error -> "hola mundo");

    assertEquals("hola mundo", recover.unsafeRunSync());
  }

  @Test
  public void recoverWithNotMatch() {
    IO<String> recover = IO.<String>raiseError(new IllegalArgumentException())
        .recover(NoSuchElementException.class, error -> "hola mundo");

    assertThrows(IllegalArgumentException.class, recover::unsafeRunSync);
  }

  @Test
  public void retry(@Mock Producer<String> computation) {
    when(computation.get()).thenThrow(UnsupportedOperationException.class);

    Try<String> retry = IO.task(computation).retry().safeRunSync();

    assertTrue(retry.isFailure());
    verify(computation, times(2)).get();
  }

  @Test
  public void retryFailure(@Mock Producer<String> computation) {
    when(computation.get()).thenThrow(UnsupportedOperationException.class);

    Try<String> retry = IO.task(computation).retry(Duration.ofMillis(100), 3).safeRunSync();

    assertTrue(retry.isFailure());
    verify(computation, times(4)).get();
  }

  @Test
  public void retrySuccess(@Mock Producer<String> computation) {
    when(computation.get())
        .thenThrow(UnsupportedOperationException.class)
        .thenThrow(UnsupportedOperationException.class)
        .thenThrow(UnsupportedOperationException.class)
        .thenReturn("hola");

    Try<String> retry = IO.task(computation).retry(Duration.ofMillis(100), 3).safeRunSync();

    assertEquals("hola", retry.getOrElseThrow());
    verify(computation, times(4)).get();
  }

  @Test
  public void repeatSuccess(@Mock Producer<String> computation) {
    when(computation.get()).thenReturn("hola");

    Try<String> repeat = IO.task(computation).repeat(Duration.ofMillis(100), 3).safeRunSync();

    assertEquals("hola", repeat.getOrElseThrow());
    verify(computation, times(4)).get();
  }

  @Test
  public void repeatStackSafe(@Mock Producer<String> computation) {
    when(computation.get()).thenReturn("hola");

    Try<String> repeat = IO.task(computation).repeat(10000).safeRunSync();

    assertEquals("hola", repeat.getOrElseThrow());
    verify(computation, times(10001)).get();
  }

  @Test
  public void repeatFailure(@Mock Producer<String> computation) {
    when(computation.get()).thenReturn("hola").thenThrow(UnsupportedOperationException.class);

    Try<String> repeat = IO.task(computation).repeat(Duration.ofMillis(100), 3).safeRunSync();

    assertTrue(repeat.isFailure());
    verify(computation, times(2)).get();
  }

  @Test
  public void repeat(@Mock Producer<String> computation) {
    when(computation.get()).thenReturn("hola");

    Try<String> repeat = IO.task(computation).repeat().safeRunSync();

    assertEquals("hola", repeat.getOrElseThrow());
    verify(computation, times(2)).get();
  }

  @Test
  public void flatMapped() {
    IO<String> io = unit()
        .map(ignore -> "hola")
        .map(ignore -> "hola")
        .map(ignore -> "hola")
        .map(ignore -> "adios");

    assertEquals("adios", io.unsafeRunSync());
  }

  @Test
  public void stackSafety() {
    IO<Integer> sum = sum(100000, 0);

    Future<Integer> futureSum = sum.runAsync();

    assertEquals(705082704, sum.unsafeRunSync());
    assertEquals(Try.success(705082704), futureSum.await(Duration.ofSeconds(1)));
  }

  @Test
  public void timed() {
    IO<Tuple2<Duration, Integer>> sum = sum(100000, 0).timed();

    Tuple2<Duration, Integer> result = sum.unsafeRunSync();

    assertEquals(705082704, result.get2());
    assertTrue(result.get1().toMillis() > 0);
  }

  @Test
  public void timeoutFail() {
    assertThrows(TimeoutException.class, IO.never().timeout(Duration.ofSeconds(1))::unsafeRunSync);
  }

  @Test
  public void timeoutSuccess() {
    assertEquals(1, IO.pure(1).timeout(Duration.ofSeconds(1)).unsafeRunSync());
  }

  @Test
  public void traverse() {
    IO<String> left = IO.task(() -> "left");
    IO<String> right = IO.task(() -> "right");

    IO<Sequence<String>> traverse = IO.traverse(listOf(left, right));

    assertEquals(listOf("left", "right"), traverse.unsafeRunSync());
  }

  @Test
  public void raceA() {
    IO<Either<Integer, String>> race = IO.race(
        IO.delay(Duration.ofMillis(10), () -> 10),
        IO.delay(Duration.ofMillis(100), () -> "b"));

    Either<Integer, String> orElseThrow = race.unsafeRunSync();

    assertEquals(Either.left(10), orElseThrow);
  }

  @Test
  public void raceB() {
    IO<Either<Integer, String>> race = IO.race(
        IO.delay(Duration.ofMillis(100), () -> 10),
        IO.delay(Duration.ofMillis(10), () -> "b"));

    Either<Integer, String> orElseThrow = race.unsafeRunSync();

    assertEquals(Either.right("b"), orElseThrow);
  }

  @Test
  public void fork() {
    IO<String> result = Instances.<IO_>monad().use()
      .then(IO.pure("hola"))
      .flatMap(hello -> IO.delay(Duration.ofSeconds(1), () -> hello + " toni").fork())
      .flatMap(Fiber::join).fix(toIO());

    String orElseThrow = result.runAsync().getOrElseThrow();

    assertEquals("hola toni", orElseThrow);
  }

  @Test
  public void memoize(@Mock Function1<String, String> toUpperCase) {
    when(toUpperCase.apply(any()))
      .thenAnswer(args -> args.getArgument(0, String.class).toUpperCase());

    IO<Function1<String, IO<String>>> memoized = IO.memoize((String str) -> IO.pure(toUpperCase.apply(str)));

    IO<String> flatMap = memoized.flatMap(x -> x.apply("hola"));
    flatMap.unsafeRunSync();
    flatMap.unsafeRunSync();
    flatMap.unsafeRunSync();
    flatMap.unsafeRunSync();

    verify(toUpperCase).apply("hola");
  }

  @Test
  public void fibSyncTest() {
    assertAll(
        () -> assertEquals(1, fibSync(1).unsafeRunSync()),
        () -> assertEquals(1, fibSync(2).unsafeRunSync()),
        () -> assertEquals(2, fibSync(3).unsafeRunSync()),
        () -> assertEquals(3, fibSync(4).unsafeRunSync()),
        () -> assertEquals(5, fibSync(5).unsafeRunSync()),
        () -> assertEquals(8, fibSync(6).unsafeRunSync()),
        () -> assertEquals(13, fibSync(7).unsafeRunSync()),
        () -> assertEquals(21, fibSync(8).unsafeRunSync()),
        () -> assertEquals(55, fibSync(10).unsafeRunSync()),
        () -> assertEquals(6765, fibSync(20).unsafeRunSync())
        );
  }

  @Test
  public void fibAsyncTest() {
    assertAll(
        () -> assertEquals(1, fibAsync(1).unsafeRunSync()),
        () -> assertEquals(1, fibAsync(2).unsafeRunSync()),
        () -> assertEquals(2, fibAsync(3).unsafeRunSync()),
        () -> assertEquals(3, fibAsync(4).unsafeRunSync()),
        () -> assertEquals(5, fibAsync(5).unsafeRunSync()),
        () -> assertEquals(8, fibAsync(6).unsafeRunSync()),
        () -> assertEquals(13, fibAsync(7).unsafeRunSync()),
        () -> assertEquals(21, fibAsync(8).unsafeRunSync()),
        () -> assertEquals(55, fibAsync(10).unsafeRunSync()),
        () -> assertEquals(6765, fibAsync(20).unsafeRunSync())
        );
  }

  private IO<Integer> fibSync(int number) {
    if (number < 2) {
      return IO.pure(number);
    }
    var number1 = fibSync(number - 1);
    var number2 = fibSync(number - 2);
    return number1.flatMap(x -> number2.map(y -> x + y));
  }

  private IO<Integer> fibAsync(int number) {
    if (number < 2) {
      return IO.pure(number);
    }
    return IO.parMap2(fibAsync(number - 1), fibAsync(number - 2), Integer::sum);
  }

  private IO<ResultSet> open(ResultSet resultSet) {
    return IO.pure(resultSet);
  }

  private Function1<ResultSet, Try<String>> tryGetString(String column) {
    return getString(column).liftTry();
  }

  private Function1<ResultSet, String> getString(String column) {
    return resultSet -> resultSet.getString(column);
  }

  private IO<Integer> sum(Integer n, Integer sum) {
    if (n == 0) {
      return IO.pure(sum);
    }
    return IO.suspend(() -> sum(n - 1, sum + n));
  }

  private IO<ImmutableList<String>> currentThreadIO() {
    Reference<IO_, ImmutableList<String>> ref = IOInstances.monadDefer().ref(ImmutableList.empty());
    IO<ImmutableList<String>> currentThread =
        ref.updateAndGet(list -> list.append("thread-" + Thread.currentThread().threadId())).fix(toIO());

    return currentThread
        .andThen(currentThread
            .andThen(currentThread
                .andThen(currentThread
                    .andThen(currentThread))));
  }
}
