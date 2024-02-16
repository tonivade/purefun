/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Producer_;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.ProducerInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Id_;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.OptionOf;
import com.github.tonivade.purefun.type.Option_;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryOf;
import com.github.tonivade.purefun.type.Try_;
import com.github.tonivade.purefun.typeclasses.FunctionK;

class EitherKTest {

  @Test
  public void left() {
    EitherK<Option_, Try_, String> left = EitherK.left(Option.some("hello"));

    assertAll(
        () -> assertTrue(left.isLeft()),
        () -> assertFalse(left.isRight()),
        () -> assertEquals(Option.some("hello"), left.getLeft()),
        () -> assertThrows(NoSuchElementException.class, left::getRight)
    );
  }

  @Test
  public void right() {
    EitherK<Option_, Try_, String> right = EitherK.right(Try.success("hello"));

    assertAll(
        () -> assertFalse(right.isLeft()),
        () -> assertTrue(right.isRight()),
        () -> assertEquals(Try.success("hello"), right.getRight()),
        () -> assertThrows(NoSuchElementException.class, right::getLeft)
    );
  }

  @Test
  public void extract() {
    EitherK<Id_, Producer_, String> left = EitherK.left(Id.of("hola"));
    EitherK<Id_, Producer_, String> right = EitherK.right(Producer.cons("hola"));

    assertAll(
        () -> assertEquals("hola", left.extract(IdInstances.comonad(), ProducerInstances.comonad())),
        () -> assertEquals("hola", right.extract(IdInstances.comonad(), ProducerInstances.comonad()))
    );
  }

  @Test
  public void coflatMap() {
    EitherK<Id_, Producer_, String> left = EitherK.left(Id.of("hola"));

    assertAll(
        () -> assertEquals(
                EitherK.left(Id.of("left")),
                left.coflatMap(IdInstances.comonad(), ProducerInstances.comonad(), eitherK -> "left")),
        () -> assertEquals(
                EitherK.right(Id.of("right")),
                left.swap().coflatMap(ProducerInstances.comonad(), IdInstances.comonad(), eitherK -> "right"))
    );
  }

  @Test
  public void mapLeft() {
    EitherK<Option_, Try_, String> eitherK = EitherK.left(Option.some("hello"));

    EitherK<Option_, Try_, Integer> result = eitherK.map(OptionInstances.functor(), TryInstances.functor(), String::length);

    assertEquals(Option.some(5), result.getLeft());
  }

  @Test
  public void mapRight() {
    EitherK<Option_, Try_, String> eitherK = EitherK.right(Try.success("hello"));

    EitherK<Option_, Try_, Integer> result = eitherK.map(OptionInstances.functor(), TryInstances.functor(), String::length);

    assertEquals(Try.success(5), result.getRight());
  }

  @Test
  public void mapK() {
    EitherK<Option_, Try_, String> eitherK = EitherK.right(Try.success("hello"));

    EitherK<Option_, Option_, String> result = eitherK.mapK(new FunctionK<>() {
      @Override
      public <T> Option<T> apply(Kind<Try_, ? extends T> from) {
        return from.fix(TryOf::<T>narrowK).toOption();
      }
    });

    assertEquals(Option.some("hello"), result.getRight());
  }

  @Test
  public void mapLeftK() {
    EitherK<Option_, Try_, String> eitherK = EitherK.left(Option.some("hello"));

    EitherK<Try_, Try_, String> result = eitherK.mapLeftK(new FunctionK<>() {
      @Override
      public <T> Try<T> apply(Kind<Option_, ? extends T> from) {
        return from.fix(OptionOf::<T>narrowK).fold(Try::failure, Try::success);
      }
    });

    assertEquals(Try.success("hello"), result.getLeft());
  }

  @Test
  public void swap() {
    EitherK<Option_, Try_, String> original = EitherK.left(Option.some("hello"));
    EitherK<Try_, Option_, String> expected = EitherK.right(Option.some("hello"));

    assertAll(
        () -> assertEquals(expected, original.swap()),
        () -> assertEquals(original, original.swap().swap())
    );
  }
}