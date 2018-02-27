/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static java.util.Objects.requireNonNull;

import java.util.function.Function;
import java.util.function.Predicate;

public final class Mappings {
  
  private Mappings() {}
  
  public static MappingBuilder get(String path) {
    return mapping(Predicates.get(path));
  }
  
  public static MappingBuilder put(String path) {
    return mapping(Predicates.put(path));
  }
  
  public static MappingBuilder post(String path) {
    return mapping(Predicates.post(path));
  }
  
  public static MappingBuilder patch(String path) {
    return mapping(Predicates.patch(path));
  }
  
  public static MappingBuilder delete(String path) {
    return mapping(Predicates.delete(path));
  }

  public static MappingBuilder mapping(Predicate<HttpRequest> matcher) {
    return new MappingBuilder().when(matcher);
  }

  public static final class Mapping {
    private final Predicate<HttpRequest> predicate;
    private final Function<HttpRequest, HttpResponse> handler;

    private Mapping(Predicate<HttpRequest> predicate, Function<HttpRequest, HttpResponse> handler) {
      this.predicate = requireNonNull(predicate);
      this.handler = requireNonNull(handler);
    }

    public boolean test(HttpRequest request) {
      return predicate.test(request);
    }

    public HttpResponse execute(HttpRequest request) {
      return handler.apply(request);
    }
  }

  public static final class MappingBuilder {
    private Predicate<HttpRequest> matcher;

    private MappingBuilder when(Predicate<HttpRequest> matcher) {
      this.matcher = matcher;
      return this;
    }
    
    public MappingBuilder and(Predicate<HttpRequest> predicate) {
      return when(this.matcher.and(predicate));
    }
    
    public MappingBuilder or(Predicate<HttpRequest> predicate) {
      return when(this.matcher.or(predicate));
    }

    public Mapping then(Function<HttpRequest, HttpResponse> handler) {
      return new Mapping(matcher, handler);
    }
  }
}
