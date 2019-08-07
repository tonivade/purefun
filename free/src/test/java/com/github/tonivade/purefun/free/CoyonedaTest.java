/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.instances.OptionInstances.functor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.type.Option;

public class CoyonedaTest {

  @Test
  void coyoneda() {
    Coyoneda<Option.µ, String, String> coyoneda = Coyoneda.of(Option.some("string"), identity());

    Coyoneda<Option.µ, String, String> result = coyoneda.map(string -> string + string).map(string -> string + string);

    assertEquals(Option.some("stringstringstringstring"), result.run(functor()).fix1(Option::narrowK));
  }
}
