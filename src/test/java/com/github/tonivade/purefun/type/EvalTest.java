/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.github.tonivade.purefun.Producer;

public class EvalTest {

  @Spy
  private Producer<String> producer;


  @Test
  void now() {
    Eval<String> eval = Eval.now("Hello World!");

    assertEquals("Hello World!", eval.value());
  }

  @Test
  public void always() {
    when(producer.get())
      .thenReturn("Hello World!");

    Eval<String> eval = Eval.always(producer);

    verify(producer, never()).get();
    assertEquals("Hello World!", eval.value());
    assertEquals("Hello World!", eval.value());
    assertEquals("Hello World!", eval.value());
    verify(producer, times(3)).get();
  }

  @Test
  public void later() {
    when(producer.get())
      .thenReturn("Hello World!");

    Eval<String> eval = Eval.later(producer);

    verify(producer, never()).get();
    assertEquals("Hello World!", eval.value());
    assertEquals("Hello World!", eval.value());
    assertEquals("Hello World!", eval.value());
    verify(producer, times(1)).get();
  }

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }
}
