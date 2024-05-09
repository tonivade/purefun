/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
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
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.OptionOf;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryOf;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Instances;

class EitherKTest {

  @Test
  public void left() {
    EitherK<Option<?>, Try<?>, String> left = EitherK.left(Option.some("hello"));

    assertAll(
        () -> assertTrue(left.isLeft()),
        () -> assertFalse(left.isRight()),
        () -> assertEquals(Option.some("hello"), left.getLeft()),
        () -> assertThrows(NoSuchElementException.class, left::getRight)
    );
  }

  @Test
  public void right() {
    EitherK<Option<?>, Try<?>, String> right = EitherK.right(Try.success("hello"));

    assertAll(
        () -> assertFalse(right.isLeft()),
        () -> assertTrue(right.isRight()),
        () -> assertEquals(Try.success("hello"), right.getRight()),
        () -> assertThrows(NoSuchElementException.class, right::getLeft)
    );
  }

  @Test
  public void extract() {
    EitherK<Id<?>, Producer<?>, String> left = EitherK.left(Id.of("hola"));
    EitherK<Id<?>, Producer<?>, String> right = EitherK.right(Producer.cons("hola"));

    assertAll(
        () -> assertEquals("hola", left.extract(Instances.<Id<?>>comonad(), Instances.<Producer<?>>comonad())),
        () -> assertEquals("hola", right.extract(Instances.<Id<?>>comonad(), Instances.<Producer<?>>comonad()))
    );
  }

  @Test
  public void coflatMap() {
    EitherK<Id<?>, Producer<?>, String> left = EitherK.left(Id.of("hola"));

    assertAll(
        () -> assertEquals(
                EitherK.left(Id.of("left")),
                left.coflatMap(Instances.<Id<?>>comonad(), Instances.<Producer<?>>comonad(), eitherK -> "left")),
        () -> assertEquals(
                EitherK.right(Id.of("right")),
                left.swap().coflatMap(Instances.<Producer<?>>comonad(), Instances.<Id<?>>comonad(), eitherK -> "right"))
    );
  }

  @Test
  public void mapLeft() {
    EitherK<Option<?>, Try<?>, String> eitherK = EitherK.left(Option.some("hello"));

    EitherK<Option<?>, Try<?>, Integer> result = eitherK.map(Instances.<Option<?>>functor(), Instances.<Try<?>>functor(), String::length);

    assertEquals(Option.some(5), result.getLeft());
  }

  @Test
  public void mapRight() {
    EitherK<Option<?>, Try<?>, String> eitherK = EitherK.right(Try.success("hello"));

    EitherK<Option<?>, Try<?>, Integer> result = eitherK.map(Instances.<Option<?>>functor(), Instances.<Try<?>>functor(), String::length);

    assertEquals(Try.success(5), result.getRight());
  }

  @Test
  public void mapK() {
    EitherK<Option<?>, Try<?>, String> eitherK = EitherK.right(Try.success("hello"));

    EitherK<Option<?>, Option<?>, String> result = eitherK.mapK(new FunctionK<>() {
      @Override
      public <T> Option<T> apply(Kind<Try<?>, ? extends T> from) {
        return from.fix(TryOf::<T>toTry).toOption();
      }
    });

    assertEquals(Option.some("hello"), result.getRight());
  }

  @Test
  public void mapLeftK() {
    EitherK<Option<?>, Try<?>, String> eitherK = EitherK.left(Option.some("hello"));

    EitherK<Try<?>, Try<?>, String> result = eitherK.mapLeftK(new FunctionK<>() {
      @Override
      public <T> Try<T> apply(Kind<Option<?>, ? extends T> from) {
        return from.fix(OptionOf::<T>toOption).fold(Try::failure, Try::success);
      }
    });

    assertEquals(Try.success("hello"), result.getLeft());
  }

  @Test
  public void swap() {
    EitherK<Option<?>, Try<?>, String> original = EitherK.left(Option.some("hello"));
    EitherK<Try<?>, Option<?>, String> expected = EitherK.right(Option.some("hello"));

    assertAll(
        () -> assertEquals(expected, original.swap()),
        () -> assertEquals(original, original.swap().swap())
    );
  }
}