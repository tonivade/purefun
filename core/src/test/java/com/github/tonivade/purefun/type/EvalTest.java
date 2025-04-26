/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.core.Unit.unit;
import static com.github.tonivade.purefun.type.Eval.FALSE;
import static com.github.tonivade.purefun.type.Eval.ONE;
import static com.github.tonivade.purefun.type.Eval.TRUE;
import static com.github.tonivade.purefun.type.Eval.UNIT;
import static com.github.tonivade.purefun.type.Eval.ZERO;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.core.Producer;

@ExtendWith(MockitoExtension.class)
public class EvalTest {

  @Spy
  private Producer<String> producer;

  @Test
  public void constants() {
    assertAll(
        () -> assertEquals(unit(), UNIT.value()),
        () -> assertEquals(0, ZERO.value()),
        () -> assertEquals(1, ONE.value()),
        () -> assertEquals(true, TRUE.value()),
        () -> assertEquals(false, FALSE.value())
    );
  }

  @Test
  public void now() {
    Eval<String> eval = Eval.now("Hello World!");

    assertEquals("Hello World!", eval.value());
  }

  @Test
  public void error() {
    Eval<String> eval = Eval.raiseError(new UnsupportedOperationException());

    assertThrows(UnsupportedOperationException.class, eval::value);
  }

  @Test
  public void defer() {
    Eval<String> eval = Eval.defer(() -> Eval.now("Hello World!")).map(ignore -> "Hola Mundo!");

    assertEquals("Hola Mundo!", eval.value());
  }

  @Test
  public void always() throws Throwable {
    when(producer.run())
      .thenReturn("Hello World!");

    Eval<String> eval = Eval.always(producer);

    verify(producer, never()).get();
    assertEquals("Hello World!", eval.value());
    assertEquals("Hello World!", eval.value());
    assertEquals("Hello World!", eval.value());
    verify(producer, times(3)).get();
  }

  @Test
  public void later() throws Throwable {
    when(producer.run())
      .thenReturn("Hello World!");

    Eval<String> eval = Eval.later(producer);

    verify(producer, never()).get();
    assertEquals("Hello World!", eval.value());
    assertEquals("Hello World!", eval.value());
    assertEquals("Hello World!", eval.value());
    verify(producer, times(1)).run();
  }

  @Test
  public void flatMapped() {
    Eval<String> eval = UNIT
        .map(ignore -> "hola")
        .map(ignore -> "hola")
        .map(ignore -> "hola")
        .map(ignore -> "adios");

    assertEquals("adios", eval.value());
  }

  @Test
  public void stackSafety() {
    assertThrows(StackOverflowError.class, () -> sumImpure(100000, 0));
    assertEquals(705082704, sum(100000, 0).value());
  }

  private Eval<Integer> sum(int n, int sum) {
    if (n == 0) {
      return Eval.now(sum);
    }
    return Eval.defer(() -> sum( n - 1, sum + n));
  }

  private Integer sumImpure(int n, int sum) {
    if (n == 0) {
      return sum;
    }
    return sumImpure(n - 1, sum + n);
  }
}
