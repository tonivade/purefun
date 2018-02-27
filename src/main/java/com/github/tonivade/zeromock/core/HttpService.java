/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Handlers.delegate;
import static com.github.tonivade.zeromock.core.Mappings.mapping;
import static com.github.tonivade.zeromock.core.Predicates.startsWith;
import static java.util.Objects.requireNonNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.github.tonivade.zeromock.core.Mappings.Mapping;

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
    return when(startsWith(path), delegate(service));
  }
  
  public HttpService when(Predicate<HttpRequest> matcher, Function<HttpRequest, HttpResponse> handler) {
    return when(mapping(matcher).then(handler));
  }
  
  public HttpService when(Mapping mapping) {
    addMapping(mapping);
    return this;
  }
  
  public Optional<HttpResponse> execute(HttpRequest request) {
    return findMapping(request).map(mapping -> mapping.execute(request));
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
}
