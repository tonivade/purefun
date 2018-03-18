/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

public final class Extractors {
  
  private Extractors() {}

  public static Handler1<HttpRequest, HttpRequest> identity() {
    return Handler1.identity();
  }

  public static Handler1<HttpRequest, Bytes> body() {
    return request -> request.body();
  }

  public static Handler1<HttpRequest, String> queryParam(String name) {
    return request -> request.param(name);
  }

  public static Handler1<HttpRequest, String> pathParam(int position) {
    return request -> request.pathParam(position);
  }
  
  public static Handler1<Bytes, String> asString() {
    return Bytes::asString;
  }
  
  public static Handler1<String, Integer> asInteger() {
    return Integer::parseInt;
  }
  
  public static Handler1<String, Long> asLong() {
    return Long::parseLong;
  }
}
