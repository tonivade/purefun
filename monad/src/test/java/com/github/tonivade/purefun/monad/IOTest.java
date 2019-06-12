/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.monad.IO.narrowK;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.github.tonivade.purefun.CheckedFunction1;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Try;

public class IOTest {

  private final Console<IO.µ> console = Console.io();

  @Mock
  private Consumer1<Try<String>> callback;

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
    List<String> result = Collections.synchronizedList(new ArrayList<>());
    IO<Unit> currentThread = IO.exec(() -> result.add(Thread.currentThread().getName()));

    IO<Unit> program = currentThread
        .andThen(currentThread
            .andThen(currentThread
                .andThen(currentThread
                    .andThen(currentThread))));

    program.toFuture().await(Duration.ofSeconds(5));

    assertEquals(5, result.size());
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

    assertEquals(Try.success("value"), bracket.toFuture().get());
    verify(resultSet, timeout(1000)).close();
  }

  @Test
  public void unsafeRunAsyncSuccess() {
    IO.pure("hola").safeRunAsync(callback);

    verify(callback, timeout(1000)).accept(Try.success("hola"));
  }

  @Test
  public void unsafeRunAsyncFailure() {
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

  @BeforeEach
  public void setUp() {
    initMocks(this);
  }

  private IO<ResultSet> open(ResultSet resultSet) {
    return IO.pure(resultSet);
  }

  private Function1<ResultSet, Try<String>> tryGetString(String column) {
    return getString(column).liftTry();
  }

  private CheckedFunction1<ResultSet, String> getString(String column) {
    return resultSet -> resultSet.getString(column);
  }
}
