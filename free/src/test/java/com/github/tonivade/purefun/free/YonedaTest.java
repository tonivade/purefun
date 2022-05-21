/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.instances.OptionInstances.functor;
import static com.github.tonivade.purefun.type.Option.some;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Option_;

public class YonedaTest {

  private final Operator1<String> concat = string -> string.concat(string);

  @Test
  public void yoneda() {
    Yoneda<Option_, String> yoneda = Yoneda.of(some("string"), functor());

    Yoneda<Option_, String> result = yoneda.map(concat).map(concat);

    assertEquals(Option.some("stringstringstringstring"), result.lower());
  }
}
