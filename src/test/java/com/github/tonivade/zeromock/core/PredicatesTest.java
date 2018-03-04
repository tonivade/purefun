/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Predicates.body;
import static com.github.tonivade.zeromock.core.Predicates.delete;
import static com.github.tonivade.zeromock.core.Predicates.get;
import static com.github.tonivade.zeromock.core.Predicates.header;
import static com.github.tonivade.zeromock.core.Predicates.param;
import static com.github.tonivade.zeromock.core.Predicates.patch;
import static com.github.tonivade.zeromock.core.Predicates.post;
import static com.github.tonivade.zeromock.core.Predicates.put;
import static com.github.tonivade.zeromock.core.Predicates.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PredicatesTest {
  @Test
  public void methods() {
    assertAll(() -> assertTrue(get("/test")   .test(Requests.get("/test"))),
              () -> assertTrue(post("/test")  .test(Requests.post("/test"))),
              () -> assertTrue(delete("/test").test(Requests.delete("/test"))),
              () -> assertTrue(put("/test")   .test(Requests.put("/test"))),
              () -> assertTrue(patch("/test") .test(Requests.patch("/test"))));
  }

  @Test
  public void parameters() {
    assertAll(() -> assertTrue(param("key")             .test(Requests.get("/test").withParam("key", "value"))),
              () -> assertTrue(param("key", "value")    .test(Requests.get("/test").withParam("key", "value"))),
              () -> assertTrue(startsWith("/path")      .test(Requests.get("/path/test"))),
              () -> assertTrue(get("/test/:id")         .test(Requests.get("/test/1"))),
              () -> assertTrue(body("asdfg")            .test(Requests.get("/test").withBody("asdfg"))),
              () -> assertTrue(header("header", "value").test(Requests.get("/test").withHeader("header", "value"))));
  }
}
