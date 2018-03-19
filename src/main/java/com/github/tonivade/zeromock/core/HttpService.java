/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Handler1.adapt;
import static com.github.tonivade.zeromock.core.Matchers.all;
import static com.github.tonivade.zeromock.core.Matchers.startsWith;
import static java.util.Objects.requireNonNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class HttpService {
  
  private final String name;
  private final List<Mapping> mappings;
  
  public HttpService(String name) {
    this(name, new LinkedList<>());
  }
  
  private HttpService(String name, List<Mapping> mappings) {
    this.name = requireNonNull(name);
    this.mappings = requireNonNull(mappings);
  }
  
  public String name() {
    return name;
  }

  public HttpService mount(String path, HttpService service) {
    addMapping(startsWith(path), adapt(HttpRequest::dropOneLevel).andThen(service::execute)::handle);
    return this;
  }
  
  public HttpService exec(Handler1<HttpRequest, HttpResponse> handler) {
    addMapping(all(), handler.lift());
    return this;
  }
  
  public HttpService add(Matcher matcher, Handler1<HttpRequest, HttpResponse> handler) {
    addMapping(matcher, handler.lift());
    return this;
  }
  
  public MappingBuilder<HttpService> when(Matcher matcher) {
    return new MappingBuilder<>(this::add).when(matcher);
  }
  
  public Optional<HttpResponse> execute(HttpRequest request) {
    return findMapping(request).flatMap(mapping -> mapping.handle(request));
  }
  
  public HttpService combine(HttpService other) {
    List<Mapping> merge = new LinkedList<>();
    merge.addAll(this.mappings);
    merge.addAll(other.mappings);
    return new HttpService(this.name + "+" + other.name, merge);
  }
  
  @Override
  public String toString() {
    return "HttpService(" + name + ")";
  }

  public void clear() {
    mappings.clear();
  }
  
  private void addMapping(Matcher matcher, OptionalHandler<HttpRequest, HttpResponse> handler) {
    mappings.add(new Mapping(matcher, handler));
  }

  private Optional<Mapping> findMapping(HttpRequest request) {
    return mappings.stream()
        .filter(mapping -> mapping.match(request))
        .findFirst();
  }

  public static final class MappingBuilder<T> {
    private final BiFunction<Matcher, Handler1<HttpRequest, HttpResponse>, T> finisher;
    private Matcher matcher;
    
    public MappingBuilder(BiFunction<Matcher, Handler1<HttpRequest, HttpResponse>, T> finisher) {
      this.finisher = requireNonNull(finisher);
    }

    public MappingBuilder<T> when(Matcher matcher) {
      this.matcher = matcher;
      return this;
    }

    public T then(Handler1<HttpRequest, HttpResponse> handler) {
      return finisher.apply(matcher, handler);
    }
  }
  
  public static final class Mapping {
    private final Matcher matcher;
    private final OptionalHandler<HttpRequest, HttpResponse> handler;

    private Mapping(Matcher matcher, OptionalHandler<HttpRequest, HttpResponse> handler) {
      this.matcher = requireNonNull(matcher);
      this.handler = requireNonNull(handler);
    }

    public boolean match(HttpRequest request) {
      return matcher.match(request);
    }

    public Optional<HttpResponse> handle(HttpRequest request) {
      return handler.handle(request);
    }
  }
}
