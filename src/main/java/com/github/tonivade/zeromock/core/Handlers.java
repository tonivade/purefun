/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Bytes.asBytes;
import static com.github.tonivade.zeromock.core.Combinators.lift;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class Handlers {
  
  private Handlers() {}

  public static <T> Function<T, Optional<HttpResponse>> ok() {
    return unit(Responses::ok);
  }

  public static Function<HttpRequest, Optional<HttpResponse>> ok(String body) {
    return ok(asBytes(body));
  }

  public static Function<HttpRequest, Optional<HttpResponse>> ok(Bytes body) {
    return ok(request -> body);
  }

  public static Function<HttpRequest, Optional<HttpResponse>> ok(Function<HttpRequest, Bytes> handler) {
    return handler.andThen(Responses::ok).andThen(lift());
  }
  
  public static Function<HttpRequest, Optional<HttpResponse>> created(String body) {
    return created(asBytes(body));
  }
  
  public static Function<HttpRequest, Optional<HttpResponse>> created(Bytes body) {
    return created(request -> body);
  }
  
  public static Function<HttpRequest, Optional<HttpResponse>> created(Function<HttpRequest, Bytes> handler) {
    return handler.andThen(Responses::created).andThen(lift());
  }
  
  public static <T> Function<T, Optional<HttpResponse>> noContent() {
    return unit(Responses::noContent);
  }
  
  public static <T> Function<T, Optional<HttpResponse>> forbidden() {
    return unit(Responses::forbidden);
  }

  public static <T> Function<T, Optional<HttpResponse>> badRequest() {
    return unit(Responses::badRequest);
  }

  public static Function<HttpRequest, Optional<HttpResponse>> badRequest(String body) {
    return badRequest(asBytes(body));
  }

  public static Function<HttpRequest, Optional<HttpResponse>> badRequest(Bytes body) {
    return badRequest(request -> body);
  }

  public static Function<HttpRequest, Optional<HttpResponse>> badRequest(Function<HttpRequest, Bytes> handler) {
    return handler.andThen(Responses::badRequest).andThen(lift());
  }

  public static <T> Function<T, Optional<HttpResponse>> notFound() {
    return unit(Responses::notFound);
  }

  public static Function<HttpRequest, Optional<HttpResponse>> notFound(String body) {
    return notFound(asBytes(body));
  }

  public static Function<HttpRequest, Optional<HttpResponse>> notFound(Bytes body) {
    return notFound(request -> body);
  }

  public static Function<HttpRequest, Optional<HttpResponse>> notFound(Function<HttpRequest, Bytes> handler) {
    return handler.andThen(Responses::notFound).andThen(lift());
  }

  public static <T> Function<T, Optional<HttpResponse>> error() {
    return unit(Responses::error);
  }

  public static Function<HttpRequest, Optional<HttpResponse>> error(String body) {
    return error(asBytes(body));
  }

  public static Function<HttpRequest, Optional<HttpResponse>> error(Bytes body) {
    return error(request -> body);
  }
  
  public static Function<HttpRequest, Optional<HttpResponse>> error(Function<HttpRequest, Bytes> handler) {
    return handler.andThen(Responses::error).andThen(lift());
  }

  public static Function<HttpRequest, Optional<HttpResponse>> delegate(HttpService service) {
    return dropOneLevel().andThen(service::execute);
  }

  public static <T> Function<T, Optional<HttpResponse>> unit(Supplier<HttpResponse> supplier) {
    return Combinators.<T, HttpResponse>force(supplier).andThen(lift());
  }
  
  private static UnaryOperator<HttpRequest> dropOneLevel() {
    return request -> request.dropOneLevel();
  }
}
