/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Matchers.body;
import static com.github.tonivade.zeromock.core.Matchers.delete;
import static com.github.tonivade.zeromock.core.Matchers.get;
import static com.github.tonivade.zeromock.core.Matchers.header;
import static com.github.tonivade.zeromock.core.Matchers.param;
import static com.github.tonivade.zeromock.core.Matchers.patch;
import static com.github.tonivade.zeromock.core.Matchers.post;
import static com.github.tonivade.zeromock.core.Matchers.put;
import static com.github.tonivade.zeromock.core.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class MatchersTest {
  @Test
  public void methods() {
    assertAll(() -> assertTrue(get("/test")   .match(Requests.get("/test"))),
              () -> assertTrue(post("/test")  .match(Requests.post("/test"))),
              () -> assertTrue(delete("/test").match(Requests.delete("/test"))),
              () -> assertTrue(put("/test")   .match(Requests.put("/test"))),
              () -> assertTrue(patch("/test") .match(Requests.patch("/test"))));
  }

  @Test
  public void parameters() {
    assertAll(() -> assertTrue(param("key")             .match(Requests.get("/test").withParam("key", "value"))),
              () -> assertTrue(param("key", "value")    .match(Requests.get("/test").withParam("key", "value"))),
              () -> assertTrue(startsWith("/path")      .match(Requests.get("/path/test"))),
              () -> assertTrue(get("/test/:id")         .match(Requests.get("/test/1"))),
              () -> assertTrue(body("asdfg")            .match(Requests.get("/test").withBody("asdfg"))),
              () -> assertTrue(header("header", "value").match(Requests.get("/test").withHeader("header", "value"))));
  }
}
