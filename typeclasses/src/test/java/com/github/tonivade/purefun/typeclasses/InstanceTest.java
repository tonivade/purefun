/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.effect.PureIO;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.transformer.EitherT;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Id;

public class InstanceTest {

  @Test
  public void testSimple() {
    Functor<Id<?>> functor = new Instance<Id<?>>() { }.functor();

    Id<Integer> result = functor.map(Id.of(1), x -> x + 1).fix();

    assertEquals(Id.of(2), result);
  }

  @Test
  public void testComplex() {
    Functor<Either<String, ?>> functor = new Instance<Either<String, ?>>() { }.functor();

    Either<String, Integer> result = functor.map(Either.right(1), x -> x + 1).fix();

    assertEquals(Either.right(2), result);
  }

  @Test
  public void testPureIO() {
    Functor<PureIO<Void, String, ?>> functor = new Instance<PureIO<Void, String, ?>>() { }.functor();

    PureIO<Void, String, Integer> result = functor.map(PureIO.pure(1), x -> x + 1).fix();

    assertEquals(Either.right(2), result.provide(null));
  }

  @Test
  public void testEitherT() {
    Functor<EitherT<IO<?>, String, ?>> functor = new Instance<EitherT<IO<?>, String, ?>>() { }.monad(Instances.<IO<?>>monad());

    EitherT<IO<?>, String, Integer> result = functor.map(EitherT.right(Instances.<IO<?>>monad(), 1), x -> x + 1).fix();

    assertEquals(Either.right(2), result.value().<IO<Either<String, Integer>>>fix().unsafeRunSync());
  }

  @Test
  public void notFoundSimple() {
    InstanceNotFoundException exception = assertThrows(InstanceNotFoundException.class, () -> Instances.<Id<?>, String>monadError());

    assertEquals("instance of type MonadError for type com.github.tonivade.purefun.type.Id not found", exception.getMessage());
  }

  @Test
  public void notAllowed() {
    record X<T>() implements Kind<X<?>, T> {};

    var x = new X<>();

    assertThrows(IllegalArgumentException.class, () -> Instances.functor(x));
  }
}
