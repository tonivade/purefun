/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio;

import static com.github.tonivade.purefun.zio.EIO.from;
import static com.github.tonivade.purefun.zio.EIO.pure;
import static com.github.tonivade.purefun.zio.EIO.raiseError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.type.Either;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

public class EIOTest {

  @Test
  public void mapRight() {
    Either<Throwable, Integer> result = parseInt("1").map(x -> x + 1).run();

    assertEquals(Either.right(2), result);
  }

  @Test
  public void mapLeft() {
    Either<Throwable, Integer> result = parseInt("lskjdf").map(x -> x + 1).run();

    assertEquals(NumberFormatException.class, result.getLeft().getClass());
  }

  @Test
  public void mapError() {
    Either<String, Integer> result = parseInt("lskjdf").mapError(Throwable::getMessage).run();

    assertEquals(Either.left("For input string: \"lskjdf\""), result);
  }

  @Test
  public void flatMapRight() {
    Either<Throwable, Integer> result = parseInt("1").flatMap(x -> pure(x + 1)).run();

    assertEquals(Either.right(2), result);
  }

  @Test
  public void flatMapLeft() {
    Either<Throwable, Integer> result = parseInt("lskjdf").flatMap(x -> pure(x + 1)).run();

    assertEquals(NumberFormatException.class, result.getLeft().getClass());
  }

  @Test
  public void flatMapError() {
    Either<String, Integer> result = parseInt("lskjdf").flatMapError(e -> raiseError(e.getMessage())).run();

    assertEquals(Either.left("For input string: \"lskjdf\""), result);
  }

  @Test
  public void bimapRight() {
    Either<String, Integer> result =
        parseInt("1").bimap(Throwable::getMessage, x -> x + 1).run();

    assertEquals(Either.right(2), result);
  }

  @Test
  public void bimapLeft() {
    Either<String, Integer> result =
        parseInt("lskjdf").bimap(Throwable::getMessage, x -> x + 1).run();

    assertEquals(Either.left("For input string: \"lskjdf\""), result);
  }

  @Test
  public void foldRight() {
    Integer result = parseInt("1").recover(e -> -1).run();

    assertEquals(1, result);
  }

  @Test
  public void foldLeft() {
    Integer result = parseInt("kjsdfdf").recover(e -> -1).run();

    assertEquals(-1, result);
  }

  @Test
  public void orElseRight() {
    Either<Throwable, Integer> result = parseInt("1").orElse(() -> pure(2)).run();

    assertEquals(Either.right(1), result);
  }

  @Test
  public void orElseLeft() {
    Either<Throwable, Integer> result = parseInt("kjsdfe").orElse(() -> pure(2)).run();

    assertEquals(Either.right(2), result);
  }

  @Test
  public void bracket() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getString("id")).thenReturn("value");

    EIO<Throwable, String> bracket = EIO.bracket(open(resultSet), getString("id"));

    assertEquals(Either.right("value"), bracket.run());
    verify(resultSet).close();
  }

  private EIO<Throwable, Integer> parseInt(String string) {
    return from(() -> Integer.parseInt(string));
  }

  private EIO<Throwable, ResultSet> open(ResultSet resultSet) {
    return pure(resultSet);
  }

  private Function1<ResultSet, EIO<Throwable, String>> getString(String column) {
    return resultSet -> from(() -> resultSet.getString(column));
  }
}
