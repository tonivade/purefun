/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static tonivade.equalizer.Equalizer.equalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public final class HttpHeaders {
  
  private final Map<String, List<String>> headers;
  
  public HttpHeaders(Map<String, List<String>> headers) {
    this.headers = unmodifiableMap(headers);
  }

  public HttpHeaders withHeader(String key, String value) {
    Map<String, List<String>> newHeaders = new HashMap<>(headers);
    newHeaders.merge(key, singletonList(value), (oldValue, newValue) -> {
      List<String> newList = new ArrayList<>(oldValue);
      newList.addAll(newValue);
      return unmodifiableList(newList);
    });
    return new HttpHeaders(newHeaders);
  }

  public boolean isEmpty() {
    return headers.isEmpty();
  }

  public boolean contains(String key) {
    return headers.containsKey(key);
  }
  
  public List<String> get(String key) {
    return headers.getOrDefault(key, emptyList());
  }
  
  public void forEach(BiConsumer<String, String> consumer) {
    headers.forEach((key, values) -> values.forEach(value -> consumer.accept(key, value)));
  }

  public static HttpHeaders empty() {
    return new HttpHeaders(emptyMap());
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(headers);
  }
  
  @Override
  public boolean equals(Object obj) {
    return equalizer(this)
        .append((a, b) -> Objects.equals(a.headers, b.headers))
        .applyTo(obj);
  }

  @Override
  public String toString() {
    return "HttpHeaders(" + headers + ")";
  }
}
