/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.Producer.unit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Monad;

public class EitherTTest {

  final Monad<IO.µ> monad = IO.monad();

  @Test
  public void map() {
    EitherT<IO.µ, Nothing, String> right = EitherT.right(monad, "abc");

    EitherT<IO.µ, Nothing, String> map = right.map(String::toUpperCase);

    assertEquals("ABC", IO.narrowK(map.get()).unsafeRunSync());
  }

  @Test
  public void flatMap() {
    EitherT<IO.µ, Nothing, String> right = EitherT.right(monad, "abc");

    EitherT<IO.µ, Nothing, String> map = right.flatMap(value -> EitherT.right(monad, value.toUpperCase()));

    assertEquals("ABC", IO.narrowK(map.get()).unsafeRunSync());
  }

  @Test
  public void filterOrElse() {
    EitherT<IO.µ, Nothing, String> right = EitherT.right(monad, "abc");

    EitherT<IO.µ, Nothing, String> filter = right.filterOrElse(String::isEmpty, unit(Either.right("not empty")));
    EitherT<IO.µ, Nothing, String> orElse = EitherT.right(monad, "not empty");

    assertEquals(IO.narrowK(orElse.get()).unsafeRunSync(), IO.narrowK(filter.orElse("not empty")).unsafeRunSync());
  }

  @Test
  public void left() {
    EitherT<IO.µ, Nothing, String> left = EitherT.left(monad, nothing());

    assertAll(
        () -> assertTrue(IO.narrowK(left.isLeft()).unsafeRunSync()),
        () -> assertFalse(IO.narrowK(left.isRight()).unsafeRunSync()),
        () -> assertEquals("empty", IO.narrowK(left.orElse("empty")).unsafeRunSync()));
  }

  @Test
  public void right() {
    EitherT<IO.µ, Nothing, String> right = EitherT.right(monad, "abc");

    assertAll(
        () -> assertFalse(IO.narrowK(right.isLeft()).unsafeRunSync()),
        () -> assertTrue(IO.narrowK(right.isRight()).unsafeRunSync()),
        () -> assertEquals("abc", IO.narrowK(right.orElse("empty")).unsafeRunSync()));
  }

  @Test
  public void mapK() {
    EitherT<IO.µ, Nothing, String> rightIo = EitherT.right(monad, "abc");

    EitherT<Try.µ, Nothing, String> rightTry = rightIo.mapK(Try.monad(), new IOToTryTransformer());

    assertEquals(Try.success("abc"), Try.narrowK(rightTry.get()));
  }
}
