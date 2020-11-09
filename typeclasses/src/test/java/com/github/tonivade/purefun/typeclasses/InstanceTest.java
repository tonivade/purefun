/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.type.EitherOf.toEither;
import static com.github.tonivade.purefun.type.IdOf.toId;
import static com.github.tonivade.purefun.typeclasses.Instance.functor;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Either_;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Id_;

public class InstanceTest {

  @Test
  public void testSimple() {
    Id<Integer> result = functor(Id_.class).map(Id.of(1), x -> x + 1).fix(toId());
    
    assertEquals(Id.of(2), result);
  }

  @Test
  public void testComplex() {
    Either<String, Integer> result = functor(
        new Instance<Kind<Either_, String>>(){}).map(Either.right(1), x -> x + 1).fix(toEither());
    
    assertEquals(Either.right(2), result);
  }
  
  @Test
  public void notFound() {
    assertThrows(InstanceNotFoundException.class, () -> Instance.monadError(Id.class));
  }
}
