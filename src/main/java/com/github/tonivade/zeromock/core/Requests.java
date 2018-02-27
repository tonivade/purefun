/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.HttpMethod.DELETE;
import static com.github.tonivade.zeromock.core.HttpMethod.GET;
import static com.github.tonivade.zeromock.core.HttpMethod.PATCH;
import static com.github.tonivade.zeromock.core.HttpMethod.POST;
import static com.github.tonivade.zeromock.core.HttpMethod.PUT;

public final class Requests {

  private Requests() {}

  public static HttpRequest get(String path) {
    return new HttpRequest(GET, new HttpPath(path));
  }

  public static HttpRequest post(String path) {
    return new HttpRequest(POST, new HttpPath(path));
  }

  public static HttpRequest put(String path) {
    return new HttpRequest(PUT, new HttpPath(path));
  }

  public static HttpRequest delete(String path) {
    return new HttpRequest(DELETE, new HttpPath(path));
  }

  public static HttpRequest patch(String path) {
    return new HttpRequest(PATCH, new HttpPath(path));
  }
}
