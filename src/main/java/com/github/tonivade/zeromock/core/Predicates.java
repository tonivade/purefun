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
import java.util.function.Predicate;

public final class Predicates {

  private Predicates() {}

  public static Predicate<HttpRequest> method(HttpMethod method) {
    return request -> request.method().equals(method);
  }
  
  public static Predicate<HttpRequest> path(String url) {
    return request -> request.path().match(new HttpPath(url));
  }
  
  public static Predicate<HttpRequest> startsWith(String url) {
    return request -> request.path().startsWith(new HttpPath(url));
  }
  
  public static Predicate<HttpRequest> param(String name) {
    return request -> request.params().contains(name);
  }
  
  public static Predicate<HttpRequest> param(String name, String value) {
    return request -> request.params().get(name).map(param -> value.equals(param)).orElse(false);
  }
  
  public static Predicate<HttpRequest> header(String key, String value) {
    return request -> request.headers().get(key).contains(value);
  }
  
  public static Predicate<HttpRequest> get() {
    return method(GET);
  }
  
  public static Predicate<HttpRequest> put() {
    return method(PUT);
  }
  
  public static Predicate<HttpRequest> post() {
    return method(POST);
  }
  
  public static Predicate<HttpRequest> delete() {
    return method(DELETE);
  }
  
  public static Predicate<HttpRequest> patch() {
    return method(PATCH);
  }
  
  public static <T> Predicate<HttpRequest> equalTo(T value) {
    return request -> json(request, value.getClass()).equals(value);
  }
  
  public static Predicate<HttpRequest> body(String body) {
    return request -> asString(request.body()).equals(body);
  }
  
  public static Predicate<HttpRequest> accept(String contentType) {
    return header("Accept", contentType);
  }
  
  public static Predicate<HttpRequest> acceptsXml() {
    return accept("text/xml");
  }
  
  public static Predicate<HttpRequest> acceptsJson() {
    return accept("application/json");
  }
  
  public static Predicate<HttpRequest> get(String path) {
    return get().and(path(path));
  }
  
  public static Predicate<HttpRequest> put(String path) {
    return put().and(path(path));
  }
  
  public static Predicate<HttpRequest> post(String path) {
    return post().and(path(path));
  }
  
  public static Predicate<HttpRequest> patch(String path) {
    return patch().and(path(path));
  }
  
  public static Predicate<HttpRequest> delete(String path) {
    return delete().and(path(path));
  }

  private static <T> T json(HttpRequest request, Type type) {
    return Extractors.body().andThen(Deserializers.<T>json(type)).apply(request);
  }
}
