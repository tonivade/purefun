/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Bytes.asBytes;
import static com.github.tonivade.zeromock.core.Bytes.asString;
import static com.github.tonivade.zeromock.core.Bytes.fromArray;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.github.tonivade.zeromock.core.Bytes;

import nl.jqno.equalsverifier.EqualsVerifier;

public class BytesTest {
  @Test
  public void notNull() {
    assertThrows(NullPointerException.class, () -> fromArray(null));
  }
  
  @Test
  public void bytesProperties() {
    Bytes bytes = asBytes("Ñ");
    
    assertAll(() -> assertArrayEquals(new byte[] {-61, -111}, bytes.toArray()),
              () -> assertArrayEquals(new byte[] {-61, -111}, bytes.getBuffer().array()),
              () -> assertEquals(2, bytes.size()),
              () -> assertFalse(bytes.isEmpty()));
  }
  
  @Test
  public void empty() {
    Bytes bytes = Bytes.empty();
    
    assertAll(() -> assertArrayEquals(new byte[] {}, bytes.toArray()),
              () -> assertArrayEquals(new byte[] {}, bytes.getBuffer().array()),
              () -> assertEquals(0, bytes.size()),
              () -> assertTrue(bytes.isEmpty()));
  }
  
  @TestFactory
  public Stream<DynamicNode> fromStringTest() {
    return randomStrings(10)
        .map(string -> dynamicTest("fromStringToString property", () -> fromStringToString(string)));
  }
  
  @TestFactory
  public Stream<DynamicNode> fromArrayTest() {
    return randomStrings(10).map(string -> string.getBytes(UTF_8))
        .map(string -> dynamicTest("fromArrayToArray property", () -> fromArrayToArray(string)));
  }
  
  @TestFactory
  public Stream<DynamicNode> fromStreamTest() {
    return randomStrings(10).map(string -> string.getBytes(UTF_8))
        .map(string -> dynamicTest("fromStringToString property", () -> fromStreamToArray(string)));
  }
  
  @Test
  public void equalsVerifier() {
    EqualsVerifier.forClass(Bytes.class).withNonnullFields("buffer").verify();
  }

  private void fromStringToString(String string) {
    assertEquals(string, asString(asBytes(string)));
  }

  private void fromArrayToArray(byte[] array) {
    assertArrayEquals(array, fromArray(array).toArray());
  }

  private void fromStreamToArray(byte[] array) throws IOException {
    assertArrayEquals(array, asBytes(new ByteArrayInputStream(array)).toArray());
  }

  private static Stream<String> randomStrings(int limit) {
    return IntStream.iterate(0, i -> i + 1).limit(limit).mapToObj(i -> randomString());
  }

  private static String randomString() {
    SecureRandom random = new SecureRandom();
    return new BigInteger(130, random).toString(32);
  }
}
