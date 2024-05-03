/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.effect.PureIOOf.toPureIO;
import static com.github.tonivade.purefun.type.EitherOf.toEither;
import static com.github.tonivade.purefun.type.IdOf.toId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.effect.PureIO;
import com.github.tonivade.purefun.effect.PureIO_;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Either_;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Id_;

public class InstanceTest {

  @Test
  public void testSimple() {
    Id<Integer> result = Instances.<Id_>functor().map(Id.of(1), x -> x + 1).fix(toId());

    assertEquals(Id.of(2), result);
  }

  @Test
  public void testComplex() {
    Instance<Kind<Either_, String>> instance = new Instance<Kind<Either_, String>>(){};

    Either<String, Integer> result = instance.functor().map(Either.right(1), x -> x + 1).fix(toEither());

    assertEquals(Either.right(2), result);
  }

  @Test
  public void testPureIO() {
    Instance<Kind<Kind<PureIO_, Void>, String>> instance = new Instance<Kind<Kind<PureIO_, Void>, String>>(){};

    PureIO<Void, String, Integer> result = instance.functor().map(PureIO.pure(1), x -> x + 1).fix(toPureIO());

    assertEquals(Either.right(2), result.provide(null));
  }

  @Test
  public void notFoundSimple() {
    InstanceNotFoundException exception = assertThrows(InstanceNotFoundException.class, () -> Instances.<Id_, String>monadError());

    assertEquals("instance of type MonadError for type com.github.tonivade.purefun.type.Id_ not found", exception.getMessage());
  }

  @Test
  public void notFoundComplex() {
    Instance<Kind<Either_, String>> instance = new Instance<Kind<Either_, String>>(){};

    InstanceNotFoundException exception = assertThrows(InstanceNotFoundException.class, () -> instance.monadDefer());

    assertEquals("instance of type MonadDefer for type com.github.tonivade.purefun.Kind<com.github.tonivade.purefun.type.Either_, java.lang.String> not found",
        exception.getMessage());
  }

  @Test
  public void notAllowed() {
    record X() {};

    var x = new X();

    assertThrows(IllegalArgumentException.class, () -> Instances.functor(x));
  }
}
