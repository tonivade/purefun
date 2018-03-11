/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Predicates.all;
import static com.github.tonivade.zeromock.core.Predicates.startsWith;
import static java.util.Objects.requireNonNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

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
    addMapping(new Mapping(startsWith(path), dropOneLevel().andThen(service::execute)));
    return this;
  }
  
  public HttpService exec(Function<HttpRequest, HttpResponse> handler) {
    addMapping(new Mapping(all(), handler.andThen(Optional::of)));
    return this;
  }
  
  public HttpService add(Predicate<HttpRequest> matcher, Function<HttpRequest, HttpResponse> handler) {
    addMapping(new Mapping(matcher, handler.andThen(Optional::of)));
    return this;
  }
  
  public MappingBuilder<HttpService> when(Predicate<HttpRequest> matcher) {
    return new MappingBuilder<>(this::add).when(matcher);
  }
  
  public Optional<HttpResponse> execute(HttpRequest request) {
    return findMapping(request).flatMap(mapping -> mapping.execute(request));
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
  
  private void addMapping(Mapping mapping) {
    mappings.add(mapping);
  }

  private Optional<Mapping> findMapping(HttpRequest request) {
    return mappings.stream()
        .filter(mapping -> mapping.test(request))
        .findFirst();
  }

  private Function<HttpRequest, HttpRequest> dropOneLevel() {
    return HttpRequest::dropOneLevel;
  }

  public static final class MappingBuilder<T> {
    private final BiFunction<Predicate<HttpRequest>, Function<HttpRequest, HttpResponse>, T> finisher;
    private Predicate<HttpRequest> matcher;
    
    public MappingBuilder(BiFunction<Predicate<HttpRequest>, Function<HttpRequest, HttpResponse>, T> finisher) {
      this.finisher = requireNonNull(finisher);
    }

    public MappingBuilder<T> when(Predicate<HttpRequest> matcher) {
      this.matcher = matcher;
      return this;
    }

    public T then(Function<HttpRequest, HttpResponse> handler) {
      return finisher.apply(matcher, handler);
    }
  }
  
  public static final class Mapping {
    private final Predicate<HttpRequest> predicate;
    private final Function<HttpRequest, Optional<HttpResponse>> handler;

    private Mapping(Predicate<HttpRequest> predicate, Function<HttpRequest, Optional<HttpResponse>> handler) {
      this.predicate = requireNonNull(predicate);
      this.handler = requireNonNull(handler);
    }

    public boolean test(HttpRequest request) {
      return predicate.test(request);
    }

    public Optional<HttpResponse> execute(HttpRequest request) {
      return handler.apply(request);
    }
  }
}
