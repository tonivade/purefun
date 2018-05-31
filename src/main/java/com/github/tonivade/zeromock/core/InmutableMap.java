/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Equal.equal;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface InmutableMap<K, V> {
  
  Map<K, V> toMap();
  
  InmutableMap<K, V> put(K key, V value);
  InmutableMap<K, V> remove(K key);
  Option<V> get(K key);
  
  Sequence<V> values();
  InmutableSet<K> keys();
  InmutableSet<Tupple2<K, V>> entries();
  
  int size();
  
  default void forEach(BiConsumer<K, V> consumer) {
    entries().forEach(tupple -> consumer.accept(tupple.get1(), tupple.get2()));
  }
  
  default <T> InmutableMap<T, V> mapKeys(Handler1<K, T> mapper) {
    return InmutableMap.from(entries().map(tupple -> tupple.map1(mapper)));
  }
  
  default <T> InmutableMap<K, T> mapValues(Handler1<V, T> mapper) {
    return InmutableMap.from(entries().map(tupple -> tupple.map2(mapper)));
  }
  
  default InmutableMap<K, V> filterKeys(Matcher<K> filter) {
    return InmutableMap.from(entries().filter(tupple -> filter.match(tupple.get1())));
  }
  
  default InmutableMap<K, V> filterValues(Matcher<V> filter) {
    return InmutableMap.from(entries().filter(tupple -> filter.match(tupple.get2())));
  }
  
  default boolean containsKey(K key) {
    return get(key).isPresent();
  }

  default InmutableMap<K, V> putIfAbsent(K key, V value) {
    if (containsKey(key)) {
      return this;
    }
    return put(key, value);
  }
  
  default InmutableMap<K, V> merge(K key, V value, Handler2<V, V, V> merger) {
    if (containsKey(key)) {
      return put(key, merger.handle(getOrDefault(key, () -> value), value));
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
  static <K, V> InmutableMap<K, V> of(Tupple2<K, V> ... entries) {
    return from(InmutableSet.of(entries));
  }

  static <K, V> Tupple2<K, V> entry(K key, V value) {
    return Tupple2.of(key, value);
  }

  static <K, V> InmutableMap<K, V> from(Map<K, V> map) {
    return new JavaBasedInmutableMap<>(map);
  }

  static <K, V> InmutableMap<K,V> empty() {
    return new JavaBasedInmutableMap<>(emptyMap());
  }

  static <K, V> InmutableMap<K, V> from(Stream<Tupple2<K, V>> entries) {
    return from(InmutableSet.from(entries));
  }

  static <K, V> InmutableMap<K, V> from(InmutableSet<Tupple2<K, V>> entries) {
    return new JavaBasedInmutableMap<>(entries.stream().collect(Collectors.toMap(Tupple2::get1, Tupple2::get2)));
  }

  final class JavaBasedInmutableMap<K, V> implements InmutableMap<K, V> {
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
    public InmutableSet<Tupple2<K, V>> entries() {
      return InmutableSet.from(backend.entrySet()).map(Tupple2::from);
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
