package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Handler1.identity;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FunctorLaws {

  private final static Handler1<String, String> toUpperCase = String::toUpperCase;
  private final static Handler1<String, String> toLowerCase = String::toLowerCase;
  
  static <T> void verifyLaws(Functor<String> functor) {
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
