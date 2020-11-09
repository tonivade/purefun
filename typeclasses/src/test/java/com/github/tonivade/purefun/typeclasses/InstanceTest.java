/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.effect.ZIOOf.toZIO;
import static com.github.tonivade.purefun.type.EitherOf.toEither;
import static com.github.tonivade.purefun.type.IdOf.toId;
import static com.github.tonivade.purefun.typeclasses.Instance.functor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.effect.ZIO;
import com.github.tonivade.purefun.effect.ZIO_;
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
    Instance<Kind<Either_, String>> instance = new Instance<Kind<Either_, String>>(){};

    Either<String, Integer> result = instance.functor().map(Either.right(1), x -> x + 1).fix(toEither());
    
    assertEquals(Either.right(2), result);
  }

  @Test
  public void testZIO() {
    Instance<Kind<Kind<ZIO_, Nothing>, String>> instance = new Instance<Kind<Kind<ZIO_, Nothing>, String>>(){};

    ZIO<Nothing, String, Integer> result = instance.functor().map(ZIO.pure(1), x -> x + 1).fix(toZIO());
    
    assertEquals(Either.right(2), result.provide(nothing()));
  }
  
  @Test
  public void notFoundSimple() {
    InstanceNotFoundException exception = assertThrows(InstanceNotFoundException.class, () -> Instance.monadError(Id.class));
    
    assertEquals("instance of type MonadError for type com.github.tonivade.purefun.type.Id", exception.getMessage());
  }
  
  @Test
  public void notFoundComplex() {
    Instance<Kind<Either_, String>> instance = new Instance<Kind<Either_, String>>(){};

    InstanceNotFoundException exception = assertThrows(InstanceNotFoundException.class, () -> instance.monadDefer());
    
    assertEquals("instance of type MonadDefer for type com.github.tonivade.purefun.Kind<com.github.tonivade.purefun.type.Either_, java.lang.String>", 
        exception.getMessage());
  }
}
