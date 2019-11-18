/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio;

import com.github.tonivade.purefun.Function1;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.github.tonivade.purefun.zio.UIO.from;
import static com.github.tonivade.purefun.zio.UIO.pure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UIOTest {

  @Test
  public void mapRight() {
    Integer result = parseInt("1").map(x -> x + 1).run();

    assertEquals(2, result);
  }

  @Test
  public void mapLeft() {
    UIO<Integer> result = parseInt("lskjdf").map(x -> x + 1);

    assertThrows(NumberFormatException.class, result::run);
  }

  @Test
  public void flatMapRight() {
    Integer result = parseInt("1").flatMap(x -> pure(x + 1)).run();

    assertEquals(2, result);
  }

  @Test
  public void flatMapLeft() {
    UIO<Integer> result = parseInt("kjere").flatMap(x -> pure(x + 1));

    assertThrows(NumberFormatException.class, result::run);
  }

  @Test
  public void redeemRight() {
    Integer result = parseInt("1").recover(e -> -1).run();

    assertEquals(1, result);
  }

  @Test
  public void redeemLeft() {
    Integer result = parseInt("kjsdfdf").recover(e -> -1).run();

    assertEquals(-1, result);
  }

  @Test
  public void bracket() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getString("id")).thenReturn("value");

    UIO<String> bracket = UIO.bracket(open(resultSet), getString("id"));

    assertEquals("value", bracket.run());
    verify(resultSet).close();
  }

  @Test
  public void bracketError() {
    UIO<String> bracket = UIO.bracket(openError(), getString("id"));

    assertThrows(SQLException.class, bracket::run);
  }

  private UIO<Integer> parseInt(String string) {
    return from(() -> Integer.parseInt(string));
  }

  private UIO<ResultSet> open(ResultSet resultSet) {
    return UIO.pure(resultSet);
  }

  private UIO<ResultSet> openError() {
    return UIO.raiseError(new SQLException("error"));
  }

  private Function1<ResultSet, UIO<String>> getString(String column) {
    return resultSet -> UIO.from(() -> resultSet.getString(column));
  }
}
