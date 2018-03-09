/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

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
    addMapping(new ServiceMapping(path, service));
    return this;
  }
  
  public HttpService add(Predicate<HttpRequest> matcher, Function<HttpRequest, HttpResponse> handler) {
    addMapping(new FunctionMapping(matcher, handler));
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
  
  private static interface Mapping {
    boolean test(HttpRequest request);

    Optional<HttpResponse> execute(HttpRequest request);
  }
  
  public static final class FunctionMapping implements Mapping {
    private final Predicate<HttpRequest> predicate;
    private final Function<HttpRequest, HttpResponse> handler;

    private FunctionMapping(Predicate<HttpRequest> predicate, Function<HttpRequest, HttpResponse> handler) {
      this.predicate = requireNonNull(predicate);
      this.handler = requireNonNull(handler);
    }

    public boolean test(HttpRequest request) {
      return predicate.test(request);
    }

    public Optional<HttpResponse> execute(HttpRequest request) {
      return Optional.of(handler.apply(request));
    }
  }
  
  public static final class ServiceMapping implements Mapping {
    private final Predicate<HttpRequest> predicate;
    private final HttpService service;

    private ServiceMapping(String path, HttpService service) {
      this.predicate = startsWith(requireNonNull(path));
      this.service = requireNonNull(service);
    }

    public boolean test(HttpRequest request) {
      return predicate.test(request);
    }

    public Optional<HttpResponse> execute(HttpRequest request) {
      return service.execute(request.dropOneLevel());
    }
  }
}
