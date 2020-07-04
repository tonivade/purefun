/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.instances.IOInstances.monadDefer;
import static com.github.tonivade.purefun.monad.IO.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Kind;
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
  void flatMap(@Mock DataSource dataSource, @Mock Connection connection, 
      @Mock PreparedStatement statement, @Mock ResultSet resultSet) throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement("sql")).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.getString(0)).thenReturn("result");
    
    Resource<IO_, ResultSet> flatMap = Resource.from(monadDefer(), task(dataSource::getConnection))
      .flatMap(conn -> Resource.from(monadDefer(), task(() -> conn.prepareStatement("sql"))))
      .flatMap(stmt -> Resource.from(monadDefer(), task(() -> stmt.executeQuery())));
    
    Kind<IO_, String> use = flatMap.use(rs -> task(() -> rs.getString(0)));
    
    assertEquals("result", use.fix(IOOf::narrowK).unsafeRunSync());
    verify(resultSet).close();
    verify(statement).close();
    verify(connection).close();
  }
}
