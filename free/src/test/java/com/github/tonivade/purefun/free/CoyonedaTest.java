/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.instances.OptionInstances.functor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.type.Option;

public class CoyonedaTest {
  
  private final Operator1<String> concat = string -> string.concat(string);

  @Test
  public void coyoneda() {
    Coyoneda<Option.µ, String, String> coyoneda = Coyoneda.of(Option.some("string").kind1(), identity());

    Coyoneda<Option.µ, String, String> result = coyoneda.map(concat).map(concat);

    assertEquals(Option.some("stringstringstringstring"), result.run(functor()));
  }
}
