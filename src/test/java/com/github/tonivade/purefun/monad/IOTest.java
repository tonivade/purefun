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
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.github.tonivade.purefun.CheckedFunction1;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.MonadError;

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
            pure.flatMap(string -> IO.of(() -> string.split(" "))).unsafeRunSync()),
        () -> assertEquals(Integer.valueOf(100), pure.andThen(IO.of(() -> 100)).unsafeRunSync()));
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
  public void monadError() {
    RuntimeException error = new RuntimeException("error");
    MonadError<IO.µ, Throwable> monadError = IOInstances.monadError();

    Higher1<IO.µ, String> pure = monadError.pure("is not ok");
    Higher1<IO.µ, String> raiseError = monadError.raiseError(error);
    Higher1<IO.µ, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<IO.µ, String> ensureOk = monadError.ensure(pure, () -> error, value -> "is not ok".equals(value));
    Higher1<IO.µ, String> ensureError = monadError.ensure(pure, () -> error, value -> "is ok?".equals(value));

    assertAll(
        () -> assertThrows(RuntimeException.class, () -> IO.narrowK(raiseError).unsafeRunSync()),
        () -> assertEquals("not an error", IO.narrowK(handleError).unsafeRunSync()),
        () -> assertThrows(RuntimeException.class, () -> IO.narrowK(ensureError).unsafeRunSync()),
        () -> assertEquals("is not ok", IO.narrowK(ensureOk).unsafeRunSync()));
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
