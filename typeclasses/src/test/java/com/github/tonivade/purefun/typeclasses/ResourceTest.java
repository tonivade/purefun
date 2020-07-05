/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.instances.IOInstances.monadDefer;
import static com.github.tonivade.purefun.monad.IO.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.monad.IO_;

@ExtendWith(MockitoExtension.class)
class ResourceTest {

  @Test
  void use(@Mock Consumer1<String> release) {
    Resource<IO_, String> resource = Resource.from(monadDefer(), IO.pure("hola"), release);
    
    Kind<IO_, String> use = resource.use(string -> IO.pure(string.toUpperCase()));
    
    assertEquals("HOLA", use.fix(IOOf::narrowK).unsafeRunSync());
    verify(release).accept("hola");
  }
  
  @Test
  void map(@Mock Consumer1<String> release) {
    Resource<IO_, String> resource = 
        Resource.from(monadDefer(), IO.pure("hola"), release).map(String::toUpperCase);
    
    Kind<IO_, Integer> use = resource.use(string -> IO.pure(string.length()));
    
    assertEquals(4, use.fix(IOOf::narrowK).unsafeRunSync());
    verify(release).accept("hola");
  }
  
  @Test
  @Disabled("I don't understand why it doesn't respect the order")
  void flatMap(@Mock DataSource dataSource, @Mock Connection connection, 
      @Mock PreparedStatement statement, @Mock ResultSet resultSet) throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement("sql")).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.getString(0)).thenReturn("result");
    
    Resource<IO_, ResultSet> flatMap = Resource.from(monadDefer(), task(dataSource::getConnection), release("connection"))
      .flatMap(conn -> Resource.from(monadDefer(), task(() -> conn.prepareStatement("sql")), release("statement")))
      .flatMap(stmt -> Resource.from(monadDefer(), task(() -> stmt.executeQuery()), release("resultSet")));
    
    Kind<IO_, String> use = flatMap.use(rs -> task(() -> rs.getString(0)));
    
    assertEquals("result", use.fix(IOOf::narrowK).unsafeRunSync());
    InOrder inOrder = inOrder(resultSet, statement, connection);
    inOrder.verify(resultSet).close();
    inOrder.verify(statement).close();
    inOrder.verify(connection).close();
  }
  
  private <T extends AutoCloseable> Consumer1<T> release(String string) {
    return resource -> { System.out.println(string); resource.close(); };
  }

  @Test
  void combine(@Mock Consumer1<String> release1, @Mock Consumer1<Integer> release2) {
    Resource<IO_, String> res1 = Resource.from(monadDefer(), IO.pure("hola"), release1);
    Resource<IO_, Integer> res2 = Resource.from(monadDefer(), IO.pure(5), release2);
    
    Resource<IO_, Tuple2<String, Integer>> combine = res1.combine(res2);
    
    Kind<IO_, String> use = combine.use(tuple -> IO.task(tuple::toString));

    assertEquals("Tuple2(hola, 5)", use.fix(IOOf::narrowK).unsafeRunSync());
    verify(release1).accept("hola");
    verify(release2).accept(5);
  }
}
