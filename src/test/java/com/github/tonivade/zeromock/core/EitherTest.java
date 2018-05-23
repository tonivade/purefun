/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class EitherTest {

  private final Handler1<String, String> toUpperCase = string -> string.toUpperCase();
  private final Handler1<Integer, Integer> intDouble = i -> i * 2;
  
  @Test
  public void bimapRight()
  {
    Either<Integer, String> either = 
        Either.<Integer, String>right("Hola mundo").bimap(intDouble, toUpperCase);

    assertEquals(Either.right("HOLA MUNDO"), either);
  }
  
  @Test
  public void bimapLeft()
  {
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
    String value = Either.<Integer, String>right("Hola mundo").orElse(() -> "or else");
    
    assertEquals("Hola mundo", value);
  }
  
  @Test
  public void orElseLeft() {
    String value = Either.<Integer, String>left(10).orElse(() -> "or else");
    
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
  public void right() {
    Either<Integer, String> either = Either.right("Hola mundo");
   
    assertAll(() -> assertTrue(either.isRight()),
              () -> assertFalse(either.isLeft()),
              () -> assertEquals("Right(Hola mundo)", either.toString()),
              () -> assertEquals("Hola mundo", either.get()),
              () -> assertEquals("Hola mundo", either.getRight()),
              () -> assertEquals(Option.some("Hola mundo"), either.right()),
              () -> assertEquals(Option.none(), either.left()),
              () -> assertEquals(Either.right("Hola mundo"), either),
              () -> assertEquals(Option.some("Hola mundo"), either.toOption()),
              () -> assertEquals(singletonList("Hola mundo"), either.stream().collect(toList())),
              () -> assertThrows(NoSuchElementException.class, () -> either.getLeft()));
  }

  @Test
  public void left() {
    Either<Integer, String> either = Either.left(10);
   
    assertAll(() -> assertTrue(either.isLeft()),
              () -> assertFalse(either.isRight()),
              () -> assertEquals("Left(10)", either.toString()),
              () -> assertEquals(Integer.valueOf(10), either.getLeft()),
              () -> assertEquals(Option.some(10), either.left()),
              () -> assertEquals(Option.none(), either.right()),
              () -> assertEquals(Either.left(10), either),
              () -> assertEquals(Option.none(), either.toOption()),
              () -> assertEquals(emptyList(), either.stream().collect(toList())),
              () -> assertThrows(NoSuchElementException.class, () -> either.get()),
              () -> assertThrows(NoSuchElementException.class, () -> either.getRight()));
  }
}
