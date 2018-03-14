/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class HttpPathTest {
  
  @Test
  public void path() {
    HttpPath httpPath = new HttpPath("/path");

    assertAll(() -> assertEquals("/path", httpPath.toPath()),
              () -> assertEquals(1, httpPath.size()),
              () -> assertEquals(Optional.of("path"), httpPath.getAt(0).map(Object::toString)),
              () -> assertEquals(true, httpPath.match(new HttpPath("/path"))),
              () -> assertEquals(false, httpPath.match(new HttpPath("/other"))));
  }
  
  @Test
  public void pathAndPath() {
    HttpPath httpPath = new HttpPath("/path/1");

    assertAll(() -> assertEquals("/path/1", httpPath.toPath()),
              () -> assertEquals(2, httpPath.size()),
              () -> assertEquals(Optional.of("path"), httpPath.getAt(0).map(Object::toString)),
              () -> assertEquals(new HttpPath("/1"), httpPath.dropOneLevel()),
              () -> assertEquals(true, httpPath.match(new HttpPath("/path/:id"))),
              () -> assertEquals(true, httpPath.startsWith(new HttpPath("/path"))),
              () -> assertEquals(false, httpPath.startsWith(new HttpPath("/other"))));
  }
  
  @Test
  public void root() {
    HttpPath httpPath = new HttpPath("/");

    assertAll(() -> assertEquals("/", httpPath.toPath()),
              () -> assertEquals(0, httpPath.size()),
              () -> assertEquals(Optional.empty(), httpPath.getAt(0)),
              () -> assertEquals(true, httpPath.match(new HttpPath("/"))));
  }
  
  @Test
  public void equalsVerifier() {
    EqualsVerifier.forClass(HttpPath.class).verify();
  }
}
