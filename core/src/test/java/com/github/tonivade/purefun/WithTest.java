/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.core.With.with;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.core.Consumer1;

@ExtendWith(MockitoExtension.class)
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
}
