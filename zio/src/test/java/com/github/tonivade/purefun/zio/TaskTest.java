/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.type.Either;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.github.tonivade.purefun.zio.Task.from;
import static com.github.tonivade.purefun.zio.Task.pure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TaskTest {

  @Test
  public void mapRight() {
    Either<Throwable, Integer> result = parseInt("1").map(x -> x + 1).safeRunSync();

    assertEquals(Either.right(2), result);
  }

  @Test
  public void mapLeft() {
    Either<Throwable, Integer> result = parseInt("lskjdf").map(x -> x + 1).safeRunSync();

    assertEquals(NumberFormatException.class, result.getLeft().getClass());
  }

  @Test
  public void flatMapRight() {
    Either<Throwable, Integer> result = parseInt("1").flatMap(x -> pure(x + 1)).safeRunSync();

    assertEquals(Either.right(2), result);
  }

  @Test
  public void flatMapLeft() {
    Either<Throwable, Integer> result = parseInt("lskjdf").flatMap(x -> pure(x + 1)).safeRunSync();

    assertEquals(NumberFormatException.class, result.getLeft().getClass());
  }

  @Test
  public void foldRight() {
    Integer result = parseInt("1").recover(e -> -1).unsafeRunSync();

    assertEquals(1, result);
  }

  @Test
  public void foldLeft() {
    Integer result = parseInt("kjsdfdf").recover(e -> -1).unsafeRunSync();

    assertEquals(-1, result);
  }

  @Test
  public void orElseRight() {
    Either<Throwable, Integer> result = parseInt("1").orElse(() -> pure(2)).safeRunSync();

    assertEquals(Either.right(1), result);
  }

  @Test
  public void orElseLeft() {
    Either<Throwable, Integer> result = parseInt("kjsdfe").orElse(() -> pure(2)).safeRunSync();

    assertEquals(Either.right(2), result);
  }

  @Test
  public void bracket() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getString("id")).thenReturn("value");

    Task<String> bracket = Task.bracket(open(resultSet), getString("id"));

    assertEquals(Either.right("value"), bracket.safeRunSync());
    verify(resultSet).close();
  }

  private Task<Integer> parseInt(String string) {
    return from(() -> Integer.parseInt(string));
  }

  private Task<ResultSet> open(ResultSet resultSet) {
    return pure(resultSet);
  }

  private Function1<ResultSet, Task<String>> getString(String column) {
    return resultSet -> from(() -> resultSet.getString(column));
  }
}
