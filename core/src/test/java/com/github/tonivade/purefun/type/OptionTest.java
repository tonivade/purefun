/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Unit;

public class OptionTest {

  private final Function1<String, String> toUpperCase = String::toUpperCase;

  @Test
  public void mapSome() {
    Option<String> option = Option.some("Hola mundo").map(toUpperCase);

    assertEquals(Option.some("HOLA MUNDO"), option);
  }

  @Test
  public void mapNone() {
    Option<String> option = Option.<String>none().map(toUpperCase);

    assertTrue(option.isEmpty());
  }

  @Test
  public void flatMapSome() {
    Option<String> option = Option.some("Hola mundo").flatMap(toUpperCase.liftOption());

    assertEquals(Option.some("HOLA MUNDO"), option);
  }

  @Test
  public void flatMapNone() {
    Option<String> option = Option.<String>none().flatMap(toUpperCase.liftOption());

    assertTrue(option.isEmpty());
  }

  @Test
  public void orElseSome() {
    String value = Option.some("Hola mundo").getOrElse("Adios!");

    assertEquals("Hola mundo", value);
  }

  @Test
  public void orElseNone() {
    String value = Option.<String>none().getOrElse("Adios!");

    assertEquals("Adios!", value);
  }

  @Test
  public void notFilter() {
    Option<String> option = Option.some("Hola mundo").filter(string -> string.startsWith("Hola"));

    assertEquals(Option.some("Hola mundo"), option);
  }

  @Test
  public void filter() {
    Option<String> option = Option.some("Hola mundo").filter(string -> string.startsWith("hola"));

    assertTrue(option.isEmpty());
  }

  @Test
  public void filterFailure() {
    Option<String> option = Option.<String>none().filter(string -> string.startsWith("hola"));

    assertTrue(option.isEmpty());
  }

  @Test
  public void foldSome() {
    String value = Option.some("hola").fold(() -> "or else", toUpperCase);

    assertEquals("HOLA", value);
  }

  @Test
  public void foldNone() {
    String value = Option.<String>none().fold(() -> "or else", toUpperCase);

    assertEquals("or else", value);
  }

  @Test
  public void toOptional() {
    Option<String> option = Option.some("hola");

    assertEquals(Optional.of("hola"), option.toOptional());
  }

  @Test
  public void some() {
    Option<String> some = Option.some("Hola mundo");

    assertAll(
        () -> assertTrue(some.isPresent()),
        () -> assertFalse(some.isEmpty()),
        () -> assertEquals("Hola mundo", some.getOrElseThrow()),
        () -> assertEquals("Some(Hola mundo)", some.toString()),
        () -> assertEquals(Optional.of("Hola mundo"), some.toOptional()),
        () -> assertEquals(Option.some("Hola mundo"), some),
        () -> assertEquals(singletonList("Hola mundo"), some.stream().collect(toList())),
        () -> assertEquals("Hola mundo", some.getOrElseNull()),
        () -> {
          AtomicReference<String> ref = new AtomicReference<>();
          some.ifPresent(ref::set);
          assertEquals("Hola mundo", ref.get());
        });
  }

  @Test
  public void failure() {
    Option<String> none = Option.none();

    assertAll(
        () -> assertFalse(none.isPresent()),
        () -> assertTrue(none.isEmpty()),
        () -> assertEquals("None", none.toString()),
        () -> assertEquals(Option.none(), none),
        () -> assertEquals(Optional.empty(), none.toOptional()),
        () -> assertEquals(emptyList(), none.stream().collect(toList())),
        () -> assertThrows(NoSuchElementException.class, none::getOrElseThrow),
        () -> assertNull(none.getOrElseNull()),
        () -> {
          AtomicReference<String> ref = new AtomicReference<>();
          none.ifPresent(ref::set);
          assertNull(ref.get());
        });
  }

  @Test
  public void optionOfNone() {
    Option<String> option = Option.of(this::messageNull);

    assertTrue(option.isEmpty());
  }

  @Test
  public void optionOfSome() {
    Option<String> option = Option.of(this::message);

    assertTrue(option.isPresent());
  }

  @Test
  public void noneSerializable() throws IOException, ClassNotFoundException {
    Option<Unit> option = Option.none();

    byte[] bytes;
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos)) {
      out.writeObject(option);
      out.flush();

      bytes = baos.toByteArray();
    }

    Object result;
    try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      result = in.readObject();
    }

    assertSame(option, result);
  }

  @Test
  public void someSerializable() throws IOException, ClassNotFoundException {
    Option<String> option = Option.some("value");

    byte[] bytes;
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos)) {
      out.writeObject(option);
      out.flush();

      bytes = baos.toByteArray();
    }

    Object result;
    try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      result = in.readObject();
    }

    assertEquals(option, result);
  }

  private String message() {
    return "Hola mundo";
  }

  private String messageNull() {
    return null;
  }
}