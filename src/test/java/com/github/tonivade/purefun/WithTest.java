/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.With.with;
import static com.github.tonivade.purefun.typeclasses.DoMonad.with;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.github.tonivade.purefun.type.Id;

public class WithTest {

  @Mock
  private Consumer1<String> consumer;

  @Test
  public void end() {
    with("some string")
      .then(String::toUpperCase)
      .then(String::concat, "other string")
      .end(consumer);

    verify(consumer).accept("SOME STRINGother string");
  }

  @Test
  public void get() {
    String value = with("some string")
      .then(String::toUpperCase)
      .then(String::concat, "other string")
      .get();

    assertEquals("SOME STRINGother string", value);
  }

  @Test
  public void withMonads() {
    Id<String> result = with(Id.monad(), Id.of("value"))
      .map(String::toUpperCase)
      .get(Id::narrowK);

    assertEquals(Id.of("VALUE"), result);
  }

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }
}
