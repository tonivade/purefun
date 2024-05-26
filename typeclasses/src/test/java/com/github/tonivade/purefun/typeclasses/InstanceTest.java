/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.effect.PureIO;
import com.github.tonivade.purefun.effect.PureIOOf;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.EitherOf;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.IdOf;
import org.junit.jupiter.api.Test;

public class InstanceTest {

  @Test
  public void testSimple() {
    Id<Integer> result = Instances.<Id<?>>functor().map(Id.of(1), x -> x + 1).fix(IdOf::toId);

    assertEquals(Id.of(2), result);
  }

  @Test
  public void testComplex() {
    Instance<Either<String, ?>> instance = new Instance<Either<String, ?>>(){};

    Either<String, Integer> result = instance.functor().map(Either.right(1), x -> x + 1).fix(EitherOf::toEither);

    assertEquals(Either.right(2), result);
  }

  @Test
  public void testPureIO() {
    Instance<PureIO<Void, String, ?>> instance = new Instance<PureIO<Void, String, ?>>(){};

    PureIO<Void, String, Integer> result = instance.functor().map(PureIO.pure(1), x -> x + 1).fix(PureIOOf::toPureIO);

    assertEquals(Either.right(2), result.provide(null));
  }

  @Test
  public void notFoundSimple() {
    InstanceNotFoundException exception = assertThrows(InstanceNotFoundException.class, () -> Instances.<Id<?>, String>monadError());

    assertEquals("instance of type MonadError for type com.github.tonivade.purefun.type.Id not found", exception.getMessage());
  }

  @Test
  public void notFoundComplex() {
    Instance<Kind<Either<?, ?>, String>> instance = new Instance<Kind<Either<?, ?>, String>>(){};

    InstanceNotFoundException exception = assertThrows(InstanceNotFoundException.class, () -> instance.monadDefer());

    assertEquals("instance of type MonadDefer for type com.github.tonivade.purefun.Kind<com.github.tonivade.purefun.type.Either<?, ?>, java.lang.String> not found",
        exception.getMessage());
  }

  @Test
  public void notAllowed() {
    record X() {};

    var x = new X();

    assertThrows(IllegalArgumentException.class, () -> Instances.functor(x));
  }
}
