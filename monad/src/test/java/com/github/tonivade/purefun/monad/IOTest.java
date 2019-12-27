/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.runtimes.ConsoleExecutor;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.Reference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.NoSuchElementException;

import static com.github.tonivade.purefun.monad.IO.narrowK;
import static com.github.tonivade.purefun.monad.IO.unit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IOTest {

  private final Console<IO.µ> console = IOInstances.console();

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
    Reference<IO.µ, ImmutableList<String>> ref = IOInstances.ref(ImmutableList.empty());
    IO<ImmutableList<String>> currentThread =
        ref.updateAndGet(list -> list.append(Thread.currentThread().getName())).fix1(IO::narrowK);

    IO<ImmutableList<String>> program = currentThread
        .andThen(currentThread
            .andThen(currentThread
                .andThen(currentThread
                    .andThen(currentThread))));

    Try<ImmutableList<String>> result =
        program.foldMap(FutureInstances.monadDefer())
          .fix1(Future::narrowK).await();

    assertEquals(Try.success(5), result.map(ImmutableList::size));
  }

  @Test
  public void bracket() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getString("id")).thenReturn("value");

    IO<Try<String>> bracket = IO.bracket(open(resultSet), IO.lift(tryGetString("id")));

    assertEquals(Try.success("value"), bracket.unsafeRunSync());
    verify(resultSet).close();
  }

  @Test
  public void bracketAsync() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getString("id")).thenReturn("value");

    IO<Try<String>> bracket = IO.bracket(open(resultSet), IO.lift(tryGetString("id")));
    Future<Try<String>> future = bracket.foldMap(FutureInstances.monadDefer()).fix1(Future::narrowK);

    assertEquals(Try.success("value"), future.await().get());
    verify(resultSet, timeout(1000)).close();
  }

  @Test
  public void safeRunAsyncSuccess(@Mock Consumer1<Try<String>> callback) {
    IO.pure("hola").safeRunAsync(callback);

    verify(callback, timeout(1000)).accept(Try.success("hola"));
  }

  @Test
  public void unsafeRunAsyncFailure(@Mock Consumer1<Try<String>> callback) {
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
        .recoverWith(IllegalArgumentException.class, error -> "hola mundo");

    assertEquals("hola mundo", recover.unsafeRunSync());
  }

  @Test
  public void recoverWithNotMatch() {
    IO<String> recover = IO.<String>raiseError(new IllegalArgumentException())
        .recoverWith(NoSuchElementException.class, error -> "hola mundo");

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

    assertEquals("hola", retry.get());
    verify(computation, times(4)).get();
  }

  @Test
  public void repeatSuccess(@Mock Producer<String> computation) {
    when(computation.get()).thenReturn("hola");

    Try<String> repeat = IO.task(computation).repeat(Duration.ofMillis(100), 3).safeRunSync();

    assertEquals("hola", repeat.get());
    verify(computation, times(4)).get();
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

    assertEquals("hola", repeat.get());
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

    Future<Integer> futureSum = sum.foldMap(FutureInstances.monadDefer()).fix1(Future::narrowK);

    assertEquals(705082704, sum.unsafeRunSync());
    assertEquals(Try.success(705082704), futureSum.await());
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
    if ( n == 0) {
      return IO.pure(sum);
    }
    return IO.suspend(() -> sum( n - 1, sum + n));
  }
}
