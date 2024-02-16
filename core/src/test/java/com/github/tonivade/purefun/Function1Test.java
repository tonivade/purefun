/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.core.Function1.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

public class Function1Test {

  private final Function1<String, String> identity = identity();
  private final Function1<String, Integer> str2int = string -> string.length();
  private final Function1<Integer, String> int2str = integer -> String.valueOf(integer);

  @Test
  public void andThenTest() {
    String result = str2int.andThen(int2str).apply("asdfg");

    assertEquals("5", result);
  }

  @Test
  public void composeTest() {
    String result = int2str.compose(str2int).apply("asdfg");

    assertEquals("5", result);
  }

  @Test
  public void identityTest() {
    String result = identity.apply("5");

    assertEquals("5", result);
  }

  @Test
  public void liftOptionalTest() {
    Optional<Integer> result = str2int.liftOptional().apply("asdfg");

    assertEquals(Optional.of(5), result);
  }

  @Test
  public void liftOptionTest() {
    Option<Integer> result = str2int.liftOption().apply("asdfg");

    assertEquals(Option.some(5), result);
  }

  @Test
  public void liftTryTest() {
    Try<Integer> result = str2int.liftTry().apply("asdfg");

    assertEquals(Try.success(5), result);
  }

  @Test
  public void memoization() {
    Function1<String, String> toUpperCase = spy(new Function1<String, String>() {
      @Override
      public String run(String value) {
        return value.toUpperCase();
      }
    });
    Function1<String, String> memoized = toUpperCase.memoized();

    assertEquals("HOLA", memoized.apply("hola"));
    assertEquals("HOLA", memoized.apply("hola"));

    verify(toUpperCase, times(1)).apply("hola");
  }
}
