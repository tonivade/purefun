/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.instances.OptionInstances.functor;
import static com.github.tonivade.purefun.type.Option.some;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.type.Option;

public class YonedaTest {

  private final Operator1<String> concat = string -> string.concat(string);

  @Test
  public void yoneda() {
    Yoneda<Option<?>, String> yoneda = Yoneda.of(some("string"), functor());

    Yoneda<Option<?>, String> result = yoneda.map(concat).map(concat);

    assertEquals(Option.some("stringstringstringstring"), result.lower());
  }
}
