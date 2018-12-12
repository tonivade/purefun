/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.monad.IO.narrowK;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.CheckedFunction1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.algebra.MonadThrow;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Console;

public class IOTest {

  private final Console<IO.µ> console = Console.io();
  private final Function1<String, String> toUpperCase = String::toUpperCase;

  @Test
  public void pure() {
    IO<String> pure = IO.pure("hola mundo");

    assertAll(
        () -> assertEquals("hola mundo", pure.unsafeRunSync()),
        () -> assertEquals("HOLA MUNDO", pure.map(String::toUpperCase).unsafeRunSync()),
        () -> assertArrayEquals(new String[] { "hola", "mundo" },
            pure.flatMap(string -> IO.of(() -> string.split(" "))).unsafeRunSync()),
        () -> assertEquals(Integer.valueOf(100), pure.andThen(IO.of(() -> 100)).unsafeRunSync()));
  }

  @Test
  public void failure() {
    IO<String> failure = IO.failure(new RuntimeException("error"));

    assertThrows(RuntimeException.class, failure::unsafeRunSync);
    assertThrows(RuntimeException.class, failure.map(toUpperCase)::unsafeRunSync);
    assertThrows(RuntimeException.class, failure.flatMap(toUpperCase.andThen(IO::pure))::unsafeRunSync);
  }

  @Test
  public void echo() {
    IO<Nothing> echo = narrowK(console.println("write your name"))
        .andThen(narrowK(console.readln()))
        .flatMap(name -> narrowK(console.println("Hello " + name)))
        .andThen(narrowK(console.println("end")));

    ConsoleExecutor executor = new ConsoleExecutor().read("Toni");

    executor.run(echo);

    assertEquals("write your name\nHello Toni\nend\n", executor.getOutput());
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
  public void monadError() {
    RuntimeException error = new RuntimeException("error");
    MonadThrow<IO.µ> monadThrow = IO.monadThrow();

    Higher1<IO.µ, String> pure = monadThrow.pure("is not ok");
    Higher1<IO.µ, String> raiseError = monadThrow.raiseError(error);
    Higher1<IO.µ, String> handleError = monadThrow.handleError(raiseError, e -> "not an error");
    Higher1<IO.µ, String> ensureOk = monadThrow.ensure(pure, () -> error, value -> "is not ok".equals(value));
    Higher1<IO.µ, String> ensureError = monadThrow.ensure(pure, () -> error, value -> "is ok?".equals(value));

    assertAll(
        () -> assertThrows(RuntimeException.class, IO.narrowK(raiseError)::unsafeRunSync),
        () -> assertEquals("not an error", IO.narrowK(handleError).unsafeRunSync()),
        () -> assertThrows(RuntimeException.class, IO.narrowK(ensureError)::unsafeRunSync),
        () -> assertEquals("is not ok", IO.narrowK(ensureOk).unsafeRunSync()));
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
