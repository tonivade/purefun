/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Bytes.asBytes;

import java.util.function.Supplier;

public final class Handlers {
  
  private Handlers() {}

  public static <T> Handler1<T, HttpResponse> ok() {
    return adapt(Responses::ok);
  }

  public static RequestHandler ok(String body) {
    return ok(asBytes(body));
  }

  public static RequestHandler ok(Bytes body) {
    return ok(request -> body);
  }

  public static RequestHandler ok(Handler1<HttpRequest, Bytes> handler) {
    return handler.andThen(Responses::ok)::handle;
  }
  
  public static RequestHandler created(String body) {
    return created(asBytes(body));
  }
  
  public static RequestHandler created(Bytes body) {
    return created(request -> body);
  }
  
  public static RequestHandler created(Handler1<HttpRequest, Bytes> handler) {
    return handler.andThen(Responses::created)::handle;
  }
  
  public static <T> Handler1<T, HttpResponse> noContent() {
    return adapt(Responses::noContent);
  }
  
  public static <T> Handler1<T, HttpResponse> forbidden() {
    return adapt(Responses::forbidden);
  }

  public static RequestHandler badRequest() {
    return adapt(Responses::badRequest)::handle;
  }

  public static RequestHandler badRequest(String body) {
    return badRequest(asBytes(body));
  }

  public static RequestHandler badRequest(Bytes body) {
    return badRequest(request -> body);
  }

  public static RequestHandler badRequest(Handler1<HttpRequest, Bytes> handler) {
    return handler.andThen(Responses::badRequest)::handle;
  }

  public static <T> Handler1<T, HttpResponse> notFound() {
    return adapt(Responses::notFound);
  }

  public static RequestHandler notFound(String body) {
    return notFound(asBytes(body));
  }

  public static RequestHandler notFound(Bytes body) {
    return notFound(request -> body);
  }

  public static RequestHandler notFound(Handler1<HttpRequest, Bytes> handler) {
    return handler.andThen(Responses::notFound)::handle;
  }

  public static <T> Handler1<T, HttpResponse> error() {
    return adapt(Responses::error);
  }

  public static RequestHandler error(String body) {
    return error(asBytes(body));
  }

  public static RequestHandler error(Bytes body) {
    return error(request -> body);
  }
  
  public static RequestHandler error(Handler1<HttpRequest, Bytes> handler) {
    return handler.andThen(Responses::error)::handle;
  }
  
  private static <T> Handler1<T, HttpResponse> adapt(Supplier<HttpResponse> supplier) {
    return Handler1.<T, HttpResponse>adapt(supplier);
  }
}
