/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Bytes.asBytes;

import java.util.function.Function;
import java.util.function.Supplier;

public final class Handlers {
  
  private Handlers() {}

  public static <T> Function<T, HttpResponse> ok() {
    return adapt(Responses::ok);
  }

  public static Function<HttpRequest, HttpResponse> ok(String body) {
    return ok(asBytes(body));
  }

  public static Function<HttpRequest, HttpResponse> ok(Bytes body) {
    return ok(request -> body);
  }

  public static Function<HttpRequest, HttpResponse> ok(Function<HttpRequest, Bytes> handler) {
    return handler.andThen(Responses::ok);
  }
  
  public static Function<HttpRequest, HttpResponse> created(String body) {
    return created(asBytes(body));
  }
  
  public static Function<HttpRequest, HttpResponse> created(Bytes body) {
    return created(request -> body);
  }
  
  public static Function<HttpRequest, HttpResponse> created(Function<HttpRequest, Bytes> handler) {
    return handler.andThen(Responses::created);
  }
  
  public static <T> Function<T, HttpResponse> noContent() {
    return adapt(Responses::noContent);
  }
  
  public static <T> Function<T, HttpResponse> forbidden() {
    return adapt(Responses::forbidden);
  }

  public static Function<HttpRequest, HttpResponse> badRequest() {
    return adapt(Responses::badRequest);
  }

  public static Function<HttpRequest, HttpResponse> badRequest(String body) {
    return badRequest(asBytes(body));
  }

  public static Function<HttpRequest, HttpResponse> badRequest(Bytes body) {
    return badRequest(request -> body);
  }

  public static Function<HttpRequest, HttpResponse> badRequest(Function<HttpRequest, Bytes> handler) {
    return handler.andThen(Responses::badRequest);
  }

  public static <T> Function<T, HttpResponse> notFound() {
    return adapt(Responses::notFound);
  }

  public static Function<HttpRequest, HttpResponse> notFound(String body) {
    return notFound(asBytes(body));
  }

  public static Function<HttpRequest, HttpResponse> notFound(Bytes body) {
    return notFound(request -> body);
  }

  public static Function<HttpRequest, HttpResponse> notFound(Function<HttpRequest, Bytes> handler) {
    return handler.andThen(Responses::notFound);
  }

  public static <T> Function<T, HttpResponse> error() {
    return adapt(Responses::error);
  }

  public static Function<HttpRequest, HttpResponse> error(String body) {
    return error(asBytes(body));
  }

  public static Function<HttpRequest, HttpResponse> error(Bytes body) {
    return error(request -> body);
  }
  
  public static Function<HttpRequest, HttpResponse> error(Function<HttpRequest, Bytes> handler) {
    return handler.andThen(Responses::error);
  }
  
  private static <T> Function<T, HttpResponse> adapt(Supplier<HttpResponse> supplier) {
    return Combinators.<T, HttpResponse>adapt(supplier);
  }
}
