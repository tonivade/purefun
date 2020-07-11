/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.type.Either.right;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
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

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Either;

@ExtendWith(MockitoExtension.class)
public class ZManagedTest {
  
  @Test
  public void use(@Mock Consumer1<String> release) {
    ZManaged<Nothing, Throwable, String> resource = ZManaged.from(ZIO.pure("hola"), release);
    
    ZIO<Nothing, Throwable, String> use = resource.use(string -> ZIO.pure(string.toUpperCase()));
    
    assertEquals(right("HOLA"), use.provide(nothing()));
    verify(release).accept("hola");
  }
  
  @Test
  public void map(@Mock Consumer1<String> release) {
    ZManaged<Nothing, Throwable, String> resource = ZManaged.from(ZIO.pure("hola"), release);
    
    ZIO<Nothing, Throwable, Integer> use = resource.map(String::toUpperCase).use(string -> ZIO.pure(string.length()));
    
    assertEquals(right(4), use.provide(nothing()));
    verify(release).accept("hola");
  }
  
  @Test
  public void flatMap(@Mock DataSource dataSource, @Mock Connection connection, 
      @Mock PreparedStatement statement, @Mock ResultSet resultSet) throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement("sql")).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.getString(0)).thenReturn("result");
    ZManaged<Nothing, Throwable, ResultSet> flatMap = ZManaged.<Nothing, Throwable, Connection>from(ZIO.task(dataSource::getConnection))
      .flatMap(conn -> ZManaged.from(ZIO.task(() -> conn.prepareStatement("sql"))))
      .flatMap(stmt -> ZManaged.from(ZIO.task(() -> stmt.executeQuery())));
    
    ZIO<Nothing, Throwable, String> use = flatMap.use(rs -> ZIO.task(() -> rs.getString(0)));
    
    assertEquals(right("result"), use.provide(nothing()));
    InOrder inOrder = inOrder(resultSet, statement, connection);
    inOrder.verify(resultSet).close();
    inOrder.verify(statement).close();
    inOrder.verify(connection).close();
  }
  
  @Test
  public void andThen(@Mock DataSource dataSource, @Mock Connection connection, 
      @Mock PreparedStatement statement, @Mock ResultSet resultSet) throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement("sql")).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.getString(0)).thenReturn("result");
    
    ZManaged<DataSource, Throwable, Connection> a = ZManaged.from(DataSource::getConnection);
    ZManaged<Connection, Throwable, PreparedStatement> b = ZManaged.from(conn -> conn.prepareStatement("sql"));
    ZManaged<PreparedStatement, Throwable, ResultSet> c = ZManaged.from(PreparedStatement::executeQuery);
    
    ZManaged<DataSource, Throwable, ResultSet> andThen = a.andThen(b).andThen(c);
    
    ZIO<DataSource, Throwable, String> use = andThen.use(rs -> ZIO.task(() -> rs.getString(0)));
    
    assertEquals(right("result"), use.provide(dataSource));
    InOrder inOrder = inOrder(resultSet, statement, connection);
    inOrder.verify(resultSet).close();
    inOrder.verify(statement).close();
    inOrder.verify(connection).close();
  }

  @Test
  public void combine(@Mock Consumer1<String> release1, @Mock Consumer1<Integer> release2) {
    ZManaged<Nothing, Throwable, String> res1 = ZManaged.from(ZIO.pure("hola"), release1);
    ZManaged<Nothing, Throwable, Integer> res2 = ZManaged.from(ZIO.pure(5), release2);
    
    ZManaged<Nothing, Throwable, Tuple2<String, Integer>> combine = res1.combine(res2);
    
    ZIO<Nothing, Throwable, String> use = combine.use(tuple -> ZIO.task(tuple::toString));

    assertEquals(right("Tuple2(hola, 5)"), use.provide(nothing()));
    verify(release1).accept("hola");
    verify(release2).accept(5);
  }
  
  @Test
  public void andThenLeft(@Mock Consumer1<String> release1, @Mock Consumer1<Integer> release2) {
    ZManaged<Nothing, Throwable, String> res1 = ZManaged.from(ZIO.pure("hola"), release1);
    ZManaged<Nothing, Throwable, Integer> res2 = ZManaged.from(ZIO.pure(5), release2);
    
    ZManaged<Nothing, Throwable, Either<String, Integer>> either = res1.either(res2);

    ZIO<Nothing, Throwable, String> use = either.use(tuple -> ZIO.task(tuple::toString));

    assertEquals(right("Left(hola)"), use.provide(nothing()));
    verify(release1).accept("hola");
    verify(release2, never()).accept(5);
  }
  
  @Test
  public void andThenRight(@Mock Consumer1<String> release1, @Mock Consumer1<Integer> release2) {
    ZManaged<Nothing, Throwable, String> res1 = ZManaged.from(ZIO.raiseError(new UnsupportedOperationException()), release1);
    ZManaged<Nothing, Throwable, Integer> res2 = ZManaged.from(ZIO.pure(5), release2);
    
    ZManaged<Nothing, Throwable, Either<String, Integer>> either = res1.either(res2);

    ZIO<Nothing, Throwable, String> use = either.use(tuple -> ZIO.task(tuple::toString));

    assertEquals(right("Right(5)"), use.provide(nothing()));
    verify(release1, never()).accept("hola");
    verify(release2).accept(5);
  }
}
