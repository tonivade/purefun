/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple2;

@ExtendWith(MockitoExtension.class)
public abstract class ResourceTest<F extends Kind<F, ?>> {

  private final Instance<F> instance;

  protected ResourceTest(Instance<F> instance) {
    this.instance = instance;
  }

  protected <T extends AutoCloseable> Resource<F, T> makeResource(Kind<F, T> acquire) {
    return makeResource(acquire, AutoCloseable::close);
  }

  protected <T> Resource<F, T> makeResource(Kind<F, T> acquire, Consumer1<T> release) {
    return instance.monadDefer().resource(acquire, release);
  }

  protected <T> T run(Kind<F, T> result) {
    return instance.runtime().run(result);
  }

  protected <T> Kind<F, T> pure(T value) {
    return instance.monadDefer().pure(value);
  }

  protected <T> Kind<F, T> later(Producer<T> value) {
    return instance.monadDefer().later(value);
  }

  @Test
  public void use(@Mock Consumer1<String> release) {
    Resource<F, String> resource = makeResource(pure("hola"), release);

    Kind<F, String> use = resource.use(string -> pure(string.toUpperCase()));

    assertEquals("HOLA", run(use));
    verify(release).accept("hola");
  }

  @Test
  public void map(@Mock Consumer1<String> release) {
    Resource<F, String> resource = makeResource(pure("hola"), release).map(String::toUpperCase);

    Kind<F, Integer> use = resource.use(string -> pure(string.length()));

    assertEquals(4, run(use));
    verify(release).accept("hola");
  }

  @Test
  public void flatMap(@Mock DataSource dataSource, @Mock Connection connection,
      @Mock PreparedStatement statement, @Mock ResultSet resultSet) throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement("sql")).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.getString(0)).thenReturn("result");

    Resource<F, ResultSet> flatMap = makeResource(later(dataSource::getConnection))
      .flatMap(conn -> makeResource(later(() -> conn.prepareStatement("sql"))))
      .flatMap(stmt -> makeResource(later(() -> stmt.executeQuery())));

    Kind<F, String> use = flatMap.use(rs -> later(() -> rs.getString(0)));

    assertEquals("result", run(use));
    InOrder inOrder = inOrder(resultSet, statement, connection);
    inOrder.verify(resultSet).close();
    inOrder.verify(statement).close();
    inOrder.verify(connection).close();
  }

  @Test
  public void combine(@Mock Consumer1<String> release1, @Mock Consumer1<Integer> release2) {
    Resource<F, String> res1 = makeResource(pure("hola"), release1);
    Resource<F, Integer> res2 = makeResource(pure(5), release2);

    Resource<F, Tuple2<String, Integer>> combine = res1.combine(res2);

    Kind<F, String> use = combine.use(tuple -> later(tuple::toString));

    assertEquals("Tuple2(hola, 5)", run(use));
    verify(release1).accept("hola");
    verify(release2).accept(5);
  }
}
