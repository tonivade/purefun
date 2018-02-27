/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.joining;
import static tonivade.equalizer.Equalizer.equalizer;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class HttpParams {
  
  private static final String BEGIN = "?";
  private static final String EMPTY = "";
  private static final String EQUALS = "=";
  private static final String SEPARATOR = "&";
  
  private final Map<String, String> params;
  
  public HttpParams(String queryParams) {
    this(queryToMap(queryParams));
  }
  
  public HttpParams(Map<String, String> params) {
    this.params = unmodifiableMap(params);
  }
  
  public Optional<String> get(String name) {
    return Optional.ofNullable(params.get(name));
  }

  public boolean isEmpty() {
    return params.isEmpty();
  }

  public boolean contains(String name) {
    return params.containsKey(name);
  }

  public HttpParams withParam(String key, String value) {
    Map<String, String> newParams = new HashMap<>(params);
    newParams.put(key, value);
    return new HttpParams(newParams);
  }
  
  public String toQueryString() {
    return params.isEmpty() ? EMPTY : paramsToString();
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(params);
  }
  
  @Override
  public boolean equals(Object obj) {
    return equalizer(this)
        .append((a, b) -> Objects.equals(a.params, b.params))
        .applyTo(obj);
  }
  
  @Override
  public String toString() {
    return "HttpParams(" + params + ")";
  }

  public static HttpParams empty() {
    return new HttpParams(emptyMap());
  }
  
  private static Map<String, String> queryToMap(String query) {
    Map<String, String> result = new HashMap<>();
    if (query != null) {
      for (String param : query.split(SEPARATOR)) {
        String[] pair = param.split(EQUALS);
        if (pair.length > 1) {
          result.put(pair[0], urlDecode(pair[1]));
        } else {
          result.put(pair[0], EMPTY);
        }
      }
    }
    return result;
  }

  private String paramsToString() {
    return BEGIN + params.entrySet().stream()
        .map(entryToString()).collect(joining(SEPARATOR));
  }

  private Function<Entry<String, String>, String> entryToString() {
    return entry -> entry.getKey() + EQUALS + urlEncode(entry.getValue());
  }

  private static String urlEncode(String value) {
    try {
      return URLEncoder.encode(value, UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String urlDecode(String value) {
    try {
      return URLDecoder.decode(value, UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      throw new UncheckedIOException(e);
    }
  }
}
