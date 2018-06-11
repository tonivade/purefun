/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Equal.equal;
import static com.github.tonivade.zeromock.core.Handler0.unit;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ImmutableMap<K, V> {
  
  Map<K, V> toMap();
  
  ImmutableMap<K, V> put(K key, V value);
  ImmutableMap<K, V> remove(K key);
  Option<V> get(K key);
  
  Sequence<V> values();
  ImmutableSet<K> keys();
  ImmutableSet<Tuple2<K, V>> entries();
  
  int size();
  
  default void forEach(BiConsumer<K, V> consumer) {
    entries().forEach(tuple -> consumer.accept(tuple.get1(), tuple.get2()));
  }
  
  default <T> ImmutableMap<T, V> mapKeys(Handler1<K, T> mapper) {
    return ImmutableMap.from(entries().map(tuple -> tuple.map1(mapper)));
  }
  
  default <T> ImmutableMap<K, T> mapValues(Handler1<V, T> mapper) {
    return ImmutableMap.from(entries().map(tuple -> tuple.map2(mapper)));
  }
  
  default ImmutableMap<K, V> filterKeys(Matcher<K> filter) {
    return ImmutableMap.from(entries().filter(tuple -> filter.match(tuple.get1())));
  }
  
  default ImmutableMap<K, V> filterValues(Matcher<V> filter) {
    return ImmutableMap.from(entries().filter(tuple -> filter.match(tuple.get2())));
  }
  
  default boolean containsKey(K key) {
    return get(key).isPresent();
  }

  default ImmutableMap<K, V> putIfAbsent(K key, V value) {
    if (containsKey(key)) {
      return this;
    }
    return put(key, value);
  }
  
  default ImmutableMap<K, V> merge(K key, V value, Handler2<V, V, V> merger) {
    if (containsKey(key)) {
      return put(key, merger.handle(getOrDefault(key, unit(value)), value));
    }
    return put(key, value);
  }
  
  default V getOrDefault(K key, Handler0<V> supplier) {
    return get(key).orElse(supplier);
  }
  
  default boolean isEmpty() {
    return size() == 0;
  }

  @SafeVarargs
  static <K, V> ImmutableMap<K, V> of(Tuple2<K, V> ... entries) {
    return from(ImmutableSet.of(entries));
  }

  static <K, V> Tuple2<K, V> entry(K key, V value) {
    return Tuple2.of(key, value);
  }

  static <K, V> ImmutableMap<K, V> from(Map<K, V> map) {
    return new JavaBasedImmutableMap<>(map);
  }

  static <K, V> ImmutableMap<K,V> empty() {
    return new JavaBasedImmutableMap<>(emptyMap());
  }

  static <K, V> ImmutableMap<K, V> from(Stream<Tuple2<K, V>> entries) {
    return from(ImmutableSet.from(entries));
  }

  static <K, V> ImmutableMap<K, V> from(ImmutableSet<Tuple2<K, V>> entries) {
    return new JavaBasedImmutableMap<>(entries.stream()
        .collect(Collectors.toMap(Tuple2::get1, Tuple2::get2)));
  }

  final class JavaBasedImmutableMap<K, V> implements ImmutableMap<K, V> {
    private final Map<K, V> backend;
    
    private JavaBasedImmutableMap(Map<K, V> backend) {
      this.backend = requireNonNull(backend);
    }
    
    @Override
    public Map<K, V> toMap() {
      return new HashMap<>(backend);
    }
    
    @Override
    public int size() {
      return backend.size();
    }
    
    @Override
    public ImmutableMap<K, V> put(K key, V value) {
      Map<K, V> newMap = toMap();
      newMap.put(key, value);
      return new JavaBasedImmutableMap<>(newMap);
    }
    
    @Override
    public ImmutableMap<K, V> remove(K key) {
      Map<K, V> newMap = toMap();
      newMap.remove(key);
      return new JavaBasedImmutableMap<>(newMap);
    }
    
    @Override
    public Option<V> get(K key) {
      return Option.of(() -> backend.get(key));
    }
    
    @Override
    public Sequence<V> values() {
      return ImmutableList.from(backend.values());
    }
    
    @Override
    public ImmutableSet<K> keys() {
      return ImmutableSet.from(backend.keySet());
    }
    
    @Override
    public ImmutableSet<Tuple2<K, V>> entries() {
      return ImmutableSet.from(backend.entrySet()).map(Tuple2::from);
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(backend);
    }
    
    @Override
    public boolean equals(Object obj) {
      return equal(this)
          .append((a, b) -> Objects.equals(a.backend, b.backend))
          .applyTo(obj);
    }
    
    @Override
    public String toString() {
      return "ImmutableMap(" + backend + ")";
    }
  }
}
