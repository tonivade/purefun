/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

public final class Headers {

  private Headers() {}
  
  public static Handler1<HttpResponse, HttpResponse> contentType(String value) {
    return response -> response.withHeader("Content-type", value);
  }
  
  public static Handler1<HttpResponse, HttpResponse> contentPlain() {
    return contentType("text/plain");
  }
  
  public static Handler1<HttpResponse, HttpResponse> contentJson() {
    return contentType("application/json");
  }
  
  public static Handler1<HttpResponse, HttpResponse> contentXml() {
    return contentType("text/xml");
  }
}
