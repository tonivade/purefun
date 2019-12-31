/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.ProducerInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class EitherKTest {

  @Test
  public void left() {
    EitherK<Option.µ, Try.µ, String> left = EitherK.left(Option.some("hello").kind1());

    assertAll(
        () -> assertTrue(left.isLeft()),
        () -> assertFalse(left.isRight()),
        () -> assertEquals(Option.some("hello"), left.getLeft()),
        () -> assertThrows(NoSuchElementException.class, left::getRight)
    );
  }

  @Test
  public void right() {
    EitherK<Option.µ, Try.µ, String> right = EitherK.right(Try.success("hello").kind1());

    assertAll(
        () -> assertFalse(right.isLeft()),
        () -> assertTrue(right.isRight()),
        () -> assertEquals(Try.success("hello"), right.getRight()),
        () -> assertThrows(NoSuchElementException.class, right::getLeft)
    );
  }

  @Test
  public void extract() {
    EitherK<Id.µ, Producer.µ, String> left = EitherK.left(Id.of("hola").kind1());
    EitherK<Id.µ, Producer.µ, String> right = EitherK.right(Producer.cons("hola").kind1());

    assertAll(
        () -> assertEquals("hola", left.extract(IdInstances.comonad(), ProducerInstances.comonad())),
        () -> assertEquals("hola", right.extract(IdInstances.comonad(), ProducerInstances.comonad()))
    );
  }

  @Test
  public void mapLeft() {
    EitherK<Option.µ, Try.µ, String> eitherK = EitherK.left(Option.some("hello").kind1());

    EitherK<Option.µ, Try.µ, Integer> result = eitherK.map(OptionInstances.functor(), TryInstances.functor(), String::length);

    assertEquals(Option.some(5), result.getLeft());
  }

  @Test
  public void mapRight() {
    EitherK<Option.µ, Try.µ, String> eitherK = EitherK.right(Try.success("hello").kind1());

    EitherK<Option.µ, Try.µ, Integer> result = eitherK.map(OptionInstances.functor(), TryInstances.functor(), String::length);

    assertEquals(Try.success(5), result.getRight());
  }
}