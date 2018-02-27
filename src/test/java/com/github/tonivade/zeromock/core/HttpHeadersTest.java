/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.zeromock.core.HttpHeaders;

import nl.jqno.equalsverifier.EqualsVerifier;

public class HttpHeadersTest {
  @Test
  public void isEmpty() {
    HttpHeaders headers = HttpHeaders.empty();
    
    assertAll("should be empty and should not contains any key",
              () -> assertTrue(headers.isEmpty()),
              () -> assertFalse(headers.contains("key")),
              () -> assertEquals(emptyList(), headers.get("key")));
  }

  @Test
  public void notEmpty() {
    HttpHeaders headers = HttpHeaders.empty().withHeader("key", "value");
    
    assertAll("should not be empty and should contains a key", 
              () -> assertFalse(headers.isEmpty()),
              () -> assertTrue(headers.contains("key")),
              () -> assertEquals(singletonList("value"), headers.get("key")));
  }

  @Test
  public void multipleValues() {
    HttpHeaders headers = HttpHeaders.empty().withHeader("key", "value1").withHeader("key", "value2");
    
    assertEquals(asList("value1", "value2"), headers.get("key"));
    assertThrows(UnsupportedOperationException.class, () -> headers.get("key").add("other"));
  }

  @Test
  public void inmutable() {
    HttpHeaders headers = HttpHeaders.empty().withHeader("key", "value");
   
    assertThrows(UnsupportedOperationException.class, () -> headers.get("key").add("other"));
  }
  
  @Test
  public void equalsVerifier() {
    EqualsVerifier.forClass(HttpHeaders.class).verify();
  }
}
