/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Equal.equal;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public interface InmutableMap<K, V> {
  
  Map<K, V> toMap();
  
  InmutableMap<K, V> put(K key, V value);
  InmutableMap<K, V> remove(K key);
  Option<V> get(K key);
  
  Sequence<V> values();
  InmutableSet<K> keys();
  
  int size();
  
  default boolean containsKey(K key) {
    return get(key).isPresent();
  }

  default InmutableMap<K, V> putIfAbsent(K key, V value) {
    if (containsKey(key)) {
      return this;
    }
    return put(key, value);
  }
  
  default V getOrDefault(K key, Handler0<V> supplier) {
    return get(key).orElse(supplier);
  }
  
  default boolean isEmpty() {
    return size() == 0;
  }

  static class JavaBasedInmutableMap<K, V> implements InmutableMap<K, V> {
    private final Map<K, V> backend;
    
    private JavaBasedInmutableMap(Map<K, V> backend) {
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
    public InmutableMap<K, V> put(K key, V value) {
      Map<K, V> newMap = toMap();
      newMap.put(key, value);
      return new JavaBasedInmutableMap<>(newMap);
    }
    
    @Override
    public InmutableMap<K, V> remove(K key) {
      Map<K, V> newMap = toMap();
      newMap.remove(key);
      return new JavaBasedInmutableMap<>(newMap);
    }
    
    @Override
    public Option<V> get(K key) {
      return Option.of(() -> backend.get(key));
    }
    
    @Override
    public Sequence<V> values() {
      return InmutableList.from(backend.values());
    }
    
    @Override
    public InmutableSet<K> keys() {
      return InmutableSet.from(backend.keySet());
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
      return "InmutableMap(" + backend + ")";
    }
  }
}
