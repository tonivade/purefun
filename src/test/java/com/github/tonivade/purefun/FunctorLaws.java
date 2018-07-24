/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Function1.identity;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Functor;

public class FunctorLaws {

  private final static Function1<String, String> toUpperCase = String::toUpperCase;
  private final static Function1<String, String> toLowerCase = String::toLowerCase;
  
  public static void verifyLaws(Functor<String> functor) {
    assertAll(() -> assertEquals(functor, 
                                 functor.map(identity()), 
                                 "identity law"),
              () -> assertEquals(functor.map(toUpperCase).map(toLowerCase), 
                                 functor.map(toUpperCase.andThen(toLowerCase)), 
                                 "composition law"),
              () -> assertEquals(functor.map(toUpperCase).map(toLowerCase.andThen(toUpperCase)), 
                                 functor.map(toUpperCase.andThen(toLowerCase)).map(toUpperCase), 
                                 "associativity law")
              );
  }

}
