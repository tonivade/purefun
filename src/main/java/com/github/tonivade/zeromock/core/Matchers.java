/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Bytes.asString;
import static com.github.tonivade.zeromock.core.HttpMethod.DELETE;
import static com.github.tonivade.zeromock.core.HttpMethod.GET;
import static com.github.tonivade.zeromock.core.HttpMethod.PATCH;
import static com.github.tonivade.zeromock.core.HttpMethod.POST;
import static com.github.tonivade.zeromock.core.HttpMethod.PUT;

import java.lang.reflect.Type;

public final class Matchers {

  private Matchers() {}
  
  public static Matcher all() {
    return request -> true;
  }

  public static Matcher method(HttpMethod method) {
    return request -> request.method().equals(method);
  }
  
  public static Matcher path(String url) {
    return request -> request.path().match(HttpPath.from(url));
  }
  
  public static Matcher startsWith(String url) {
    return request -> request.path().startsWith(HttpPath.from(url));
  }
  
  public static Matcher param(String name) {
    return request -> request.params().contains(name);
  }
  
  public static Matcher param(String name, String value) {
    return request -> request.params().get(name).map(param -> value.equals(param)).orElse(false);
  }
  
  public static Matcher header(String key, String value) {
    return request -> request.headers().get(key).contains(value);
  }
  
  public static Matcher get() {
    return method(GET);
  }
  
  public static Matcher put() {
    return method(PUT);
  }
  
  public static Matcher post() {
    return method(POST);
  }
  
  public static Matcher delete() {
    return method(DELETE);
  }
  
  public static Matcher patch() {
    return method(PATCH);
  }
  
  public static <T> Matcher equalTo(T value) {
    return request -> json(request, value.getClass()).equals(value);
  }
  
  public static Matcher body(String body) {
    return request -> asString(request.body()).equals(body);
  }
  
  public static Matcher accept(String contentType) {
    return header("Accept", contentType);
  }
  
  public static Matcher acceptsXml() {
    return accept("text/xml");
  }
  
  public static Matcher acceptsJson() {
    return accept("application/json");
  }
  
  public static Matcher get(String path) {
    return get().and(path(path));
  }
  
  public static Matcher put(String path) {
    return put().and(path(path));
  }
  
  public static Matcher post(String path) {
    return post().and(path(path));
  }
  
  public static Matcher patch(String path) {
    return patch().and(path(path));
  }
  
  public static Matcher delete(String path) {
    return delete().and(path(path));
  }

  private static <T> T json(HttpRequest request, Type type) {
    return Extractors.body().andThen(Deserializers.<T>json(type)).handle(request);
  }
}
