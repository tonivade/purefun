/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.core.Nothing.nothing;
import static com.github.tonivade.purefun.type.Either.right;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Nothing;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.type.Either;

@ExtendWith(MockitoExtension.class)
public class ManagedTest {
  
  @Test
  public void use(@Mock Consumer1<String> release) {
    Managed<Nothing, Throwable, String> resource = Managed.from(PureIO.pure("hola"), release);
    
    PureIO<Nothing, Throwable, String> use = resource.use(string -> PureIO.pure(string.toUpperCase()));
    
    assertEquals(right("HOLA"), use.provide(nothing()));
    verify(release).accept("hola");
  }
  
  @Test
  public void map(@Mock Consumer1<String> release) {
    Managed<Nothing, Throwable, String> resource = Managed.from(PureIO.pure("hola"), release);
    
    PureIO<Nothing, Throwable, Integer> use = resource.map(String::toUpperCase).use(string -> PureIO.pure(string.length()));
    
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
    Managed<Nothing, Throwable, ResultSet> flatMap = Managed.<Nothing, Throwable, Connection>from(PureIO.task(dataSource::getConnection))
      .flatMap(conn -> Managed.from(PureIO.task(() -> conn.prepareStatement("sql"))))
      .flatMap(stmt -> Managed.from(PureIO.task(() -> stmt.executeQuery())));
    
    PureIO<Nothing, Throwable, String> use = flatMap.use(rs -> PureIO.task(() -> rs.getString(0)));
    
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
    
    Managed<DataSource, Throwable, Connection> a = Managed.from(DataSource::getConnection);
    Managed<Connection, Throwable, PreparedStatement> b = Managed.from(conn -> conn.prepareStatement("sql"));
    Managed<PreparedStatement, Throwable, ResultSet> c = Managed.from(PreparedStatement::executeQuery);
    
    Managed<DataSource, Throwable, ResultSet> andThen = a.andThen(b).andThen(c);
    
    PureIO<DataSource, Throwable, String> use = andThen.use(rs -> PureIO.task(() -> rs.getString(0)));
    
    assertEquals(right("result"), use.provide(dataSource));
    InOrder inOrder = inOrder(resultSet, statement, connection);
    inOrder.verify(resultSet).close();
    inOrder.verify(statement).close();
    inOrder.verify(connection).close();
  }

  @Test
  public void combine(@Mock Consumer1<String> release1, @Mock Consumer1<Integer> release2) {
    Managed<Nothing, Throwable, String> res1 = Managed.from(PureIO.pure("hola"), release1);
    Managed<Nothing, Throwable, Integer> res2 = Managed.from(PureIO.pure(5), release2);
    
    Managed<Nothing, Throwable, Tuple2<String, Integer>> combine = res1.combine(res2);
    
    PureIO<Nothing, Throwable, String> use = combine.use(tuple -> PureIO.task(tuple::toString));

    assertEquals(right("Tuple2(hola, 5)"), use.provide(nothing()));
    verify(release1).accept("hola");
    verify(release2).accept(5);
  }
  
  @Test
  public void andThenLeft(@Mock Consumer1<String> release1, @Mock Consumer1<Integer> release2) {
    Managed<Nothing, Throwable, String> res1 = Managed.from(PureIO.pure("hola"), release1);
    Managed<Nothing, Throwable, Integer> res2 = Managed.from(PureIO.pure(5), release2);
    
    Managed<Nothing, Throwable, Either<String, Integer>> either = res1.either(res2);

    PureIO<Nothing, Throwable, String> use = either.use(tuple -> PureIO.task(tuple::toString));

    assertEquals(right("Left(hola)"), use.provide(nothing()));
    verify(release1).accept("hola");
    verify(release2, never()).accept(5);
  }
  
  @Test
  public void andThenRight(@Mock Consumer1<String> release1, @Mock Consumer1<Integer> release2) {
    Managed<Nothing, Throwable, String> res1 = Managed.from(PureIO.raiseError(new UnsupportedOperationException()), release1);
    Managed<Nothing, Throwable, Integer> res2 = Managed.from(PureIO.pure(5), release2);
    
    Managed<Nothing, Throwable, Either<String, Integer>> either = res1.either(res2);

    PureIO<Nothing, Throwable, String> use = either.use(tuple -> PureIO.task(tuple::toString));

    assertEquals(right("Right(5)"), use.provide(nothing()));
    verify(release1, never()).accept("hola");
    verify(release2).accept(5);
  }
}
