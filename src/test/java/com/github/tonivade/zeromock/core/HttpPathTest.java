/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class HttpPathTest {
  
  @Test
  public void path() {
    HttpPath httpPath = HttpPath.from("/path");

    assertAll(() -> assertEquals("/path", httpPath.toPath()),
              () -> assertEquals(1, httpPath.size()),
              () -> assertEquals(Optional.of("path"), httpPath.getAt(0).map(Object::toString)),
              () -> assertEquals(true, httpPath.match(HttpPath.from("/path"))),
              () -> assertEquals(false, httpPath.match(HttpPath.from("/other"))));
  }
  
  @Test
  public void pathAndPath() {
    HttpPath httpPath = HttpPath.from("/path/1");

    assertAll(() -> assertEquals("/path/1", httpPath.toPath()),
              () -> assertEquals(2, httpPath.size()),
              () -> assertEquals(Optional.of("path"), httpPath.getAt(0).map(Object::toString)),
              () -> assertEquals(HttpPath.from("/1"), httpPath.dropOneLevel()),
              () -> assertEquals(true, httpPath.match(HttpPath.from("/path/:id"))),
              () -> assertEquals(true, httpPath.startsWith(HttpPath.from("/path"))),
              () -> assertEquals(false, httpPath.startsWith(HttpPath.from("/other"))));
  }
  
  @Test
  public void root() {
    HttpPath httpPath = HttpPath.from("/");

    assertAll(() -> assertEquals("/", httpPath.toPath()),
              () -> assertEquals(0, httpPath.size()),
              () -> assertEquals(Optional.empty(), httpPath.getAt(0)),
              () -> assertEquals(true, httpPath.match(HttpPath.from("/"))));
  }
  
  @Test
  public void of() {
    HttpPath httpPath = HttpPath.of("path1", "path2");
    
    assertAll(() -> assertEquals("/path1/path2", httpPath.toPath()),
              () -> assertEquals(2, httpPath.size()),
              () -> assertEquals(Optional.of("path1"), httpPath.getAt(0).map(Object::toString)), 
              () -> assertEquals(Optional.of("path2"), httpPath.getAt(1).map(Object::toString)),
              () -> assertEquals(true, httpPath.match(HttpPath.from("/path1/path2"))));
  }
  
  @Test
  public void invalidFrom() {
    assertAll(() -> assertThrows(IllegalArgumentException.class, () -> HttpPath.from(null)),
              () -> assertThrows(IllegalArgumentException.class, () -> HttpPath.from("")),
              () -> assertThrows(IllegalArgumentException.class, () -> HttpPath.from("path")));
  }
  
  @Test
  public void invalidOf() {
    assertThrows(IllegalArgumentException.class, () -> HttpPath.of((String[])null));
  }
  
  @Test
  public void equalsVerifier() {
    EqualsVerifier.forClass(HttpPath.class).verify();
  }
}
