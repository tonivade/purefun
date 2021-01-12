/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Function1.identity;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;

public class EitherTest {

  private final Function1<String, String> toUpperCase = String::toUpperCase;
  private final Function1<String, String> toLowerCase = String::toLowerCase;
  private final Function1<Integer, Integer> intDouble = i -> i * 2;

  @Test
  public void bimapRight() {
    Either<Integer, String> either =
        Either.<Integer, String>right("Hola mundo").bimap(intDouble, toUpperCase);

    assertEquals(Either.right("HOLA MUNDO"), either);
  }

  @Test
  public void bimapLeft() {
    Either<Integer, String> either =
        Either.<Integer, String>left(10).bimap(intDouble, toUpperCase);

    assertEquals(Either.left(20), either);
  }

  @Test
  public void mapRight() {
    Either<Integer, String> either = Either.<Integer, String>right("Hola mundo").map(toUpperCase);

    assertEquals(Either.right("HOLA MUNDO"), either);
  }

  @Test
  public void mapRightLeft() {
    Either<Integer, String> either = Either.<Integer, String>left(10).map(toUpperCase);

    assertEquals(Either.left(10), either);
  }

  @Test
  public void mapLeft() {
    Either<Integer, String> either = Either.<Integer, String>left(10).mapLeft(intDouble);

    assertEquals(Either.left(20), either);
  }

  @Test
  public void mapLeftRight() {
    Either<Integer, String> either =
        Either.<Integer, String>right("Hola mundo").mapLeft(intDouble);

    assertEquals(Either.right("Hola mundo"), either);
  }

  @Test
  public void flatMapRight() {
    Either<Integer, String> either =
        Either.<Integer, String>right("Hola mundo").flatMap(toUpperCase.liftRight());

    assertEquals(Either.right("HOLA MUNDO"), either);
  }

  @Test
  public void flatMapRightLeft() {
    Either<Integer, String> either =
        Either.<Integer, String>left(10).flatMap(toUpperCase.liftRight());

    assertEquals(Either.left(10), either);
  }

  @Test
  public void flatMapLeft() {
    Either<Integer, String> either =
        Either.<Integer, String>left(10).flatMapLeft(intDouble.liftLeft());

    assertEquals(Either.left(20), either);
  }

  @Test
  public void flatMapLeftRight() {
    Either<Integer, String> either =
        Either.<Integer, String>right("Hola mundo").flatMapLeft(intDouble.liftLeft());

    assertEquals(Either.right("Hola mundo"), either);
  }

  @Test
  public void filter() {
    Option<Either<Integer, String>> option = Either.<Integer, String>right("Hola mundo")
        .filter(string -> string.startsWith("Hola"));

    assertEquals(Option.some(Either.right("Hola mundo")), option);
  }

  @Test
  public void notFilter() {
    Option<Either<Integer, String>> option = Either.<Integer, String>right("Hola mundo")
        .filter(string -> string.startsWith("hola"));

    assertEquals(Option.none(), option);
  }

  @Test
  public void filterLeft() {
    Option<Either<Integer, String>> option = Either.<Integer, String>left(10)
        .filter(string -> string.startsWith("Hola"));

    assertEquals(Option.none(), option);
  }

  @Test
  public void filterOrElse() {
    Either<Integer, String> either = Either.<Integer, String>right("Hola mundo")
        .filterOrElse(string -> string.startsWith("Hola"), () -> Either.right("or else"));

    assertEquals(Either.right("Hola mundo"), either);
  }

  @Test
  public void notFilterOrElse() {
    Either<Integer, String> either = Either.<Integer, String>right("Hola mundo")
        .filterOrElse(string -> string.startsWith("hola"), () -> Either.right("or else"));

    assertEquals(Either.right("or else"), either);
  }

  @Test
  public void filterOrElseLeft() {
    Either<Integer, String> either = Either.<Integer, String>left(10)
        .filterOrElse(string -> string.startsWith("hola"), () -> Either.right("or else"));

    assertEquals(Either.left(10), either);
  }

  @Test
  public void orElseRight() {
    String value = Either.<Integer, String>right("Hola mundo").getOrElse("or else");

    assertEquals("Hola mundo", value);
  }

  @Test
  public void orElseLeft() {
    String value = Either.<Integer, String>left(10).getOrElse("or else");

    assertEquals("or else", value);
  }

  @Test
  public void foldRight() {
    String value = Either.<Integer, String>right("Hola mundo").fold(String::valueOf, toUpperCase);

    assertEquals("HOLA MUNDO", value);
  }

  @Test
  public void foldLeft() {
    String value = Either.<Integer, String>left(10).fold(String::valueOf, toUpperCase);

    assertEquals("10", value);
  }

  @Test
  public void swapRight() {
    Either<String, Integer> either = Either.<Integer, String>right("Hola mundo").swap();

    assertEquals(Either.left("Hola mundo"), either);
  }

  @Test
  public void swapLeft() {
    Either<Integer, String> either = Either.<String, Integer>left("Hola mundo").swap();

    assertEquals(Either.right("Hola mundo"), either);
  }

  @Test
  public void right() {
    Either<Integer, String> right = Either.right("Hola mundo");

    assertAll(
        () -> assertTrue(right.isRight()),
        () -> assertFalse(right.isLeft()),
        () -> assertEquals("Right(Hola mundo)", right.toString()),
        () -> assertEquals("Hola mundo", right.get()),
        () -> assertEquals("Hola mundo", right.getOrElseNull()),
        () -> assertEquals("Hola mundo", right.getOrElse("adios")),
        () -> assertEquals("Hola mundo", right.getRight()),
        () -> assertEquals(Option.some("Hola mundo"), right.right()),
        () -> assertEquals(Option.none(), right.left()),
        () -> assertEquals(Either.right("Hola mundo"), right),
        () -> assertEquals(Option.some("Hola mundo"), right.toOption()),
        () -> assertEquals(Validation.valid("Hola mundo"), right.toValidation()),
        () -> assertEquals(singletonList("Hola mundo"), right.stream().collect(toList())),
        () -> assertThrows(NoSuchElementException.class, right::getLeft));
  }

  @Test
  public void left() {
    Either<Integer, String> left = Either.left(10);

    assertAll(
        () -> assertTrue(left.isLeft()),
        () -> assertFalse(left.isRight()),
        () -> assertEquals("Left(10)", left.toString()),
        () -> assertEquals(Integer.valueOf(10), left.getLeft()),
        () -> assertEquals(Option.some(10), left.left()),
        () -> assertEquals(Option.none(), left.right()),
        () -> assertEquals(Either.left(10), left),
        () -> assertEquals(Option.none(), left.toOption()),
        () -> assertEquals(Validation.invalid(10), left.toValidation()),
        () -> assertEquals(emptyList(), left.stream().collect(toList())),
        () -> assertThrows(NoSuchElementException.class, left::get),
        () -> assertNull(left.getOrElseNull()),
        () -> assertEquals("adios", left.getOrElse("adios")),
        () -> assertThrows(NoSuchElementException.class, left::getRight));
  }

  @Test
  public void leftLaws() {
    Either<String, Integer> either = Either.left("Hola");

    assertAll(
        () -> assertEquals(either,
            either.mapLeft(identity()),
            "identity law"),
        () -> assertEquals(either.mapLeft(toUpperCase).mapLeft(toLowerCase),
            either.mapLeft(toUpperCase.andThen(toLowerCase)),
            "composition law"),
        () -> assertEquals(either.mapLeft(toUpperCase).mapLeft(toLowerCase.andThen(toUpperCase)),
            either.mapLeft(toUpperCase.andThen(toLowerCase)).mapLeft(toUpperCase),
            "associativity law")
    );
  }
}
