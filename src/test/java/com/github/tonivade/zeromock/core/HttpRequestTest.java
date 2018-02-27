/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import org.junit.jupiter.api.Test;

import com.github.tonivade.zeromock.core.HttpRequest;

import nl.jqno.equalsverifier.EqualsVerifier;

public class HttpRequestTest {
  
  @Test
  public void equalsVerifier() {
    EqualsVerifier.forClass(HttpRequest.class).verify();
  }
}
