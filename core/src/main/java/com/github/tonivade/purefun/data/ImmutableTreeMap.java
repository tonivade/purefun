/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static java.util.Collections.emptyNavigableMap;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Operator2;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

public interface ImmutableTreeMap<K, V> extends ImmutableMap<K, V> {

  NavigableMap<K, V> toNavigableMap();

  @Override
  ImmutableTreeMap<K, V> put(K key, V value);

  @Override
  default ImmutableTreeMap<K, V> putAll(ImmutableSet<Tuple2<K, V>> other) {
    return ImmutableTreeMap.from(entries().appendAll(other));
  }

  @Override
  ImmutableTreeMap<K, V> remove(K key);

  @Override
  ImmutableTreeMap<K, V> merge(K key, V value, Operator2<V> merger);

  ImmutableTreeMap<K, V> headMap(K toKey);
  ImmutableTreeMap<K, V> tailMap(K fromKey);
  Option<Tuple2<K, V>> headEntry();
  Option<Tuple2<K, V>> tailEntry();
  Option<Tuple2<K, V>> higherEntry(K key);
  Option<Tuple2<K, V>> lowerEntry(K key);
  Option<Tuple2<K, V>> floorEntry(K key);
  Option<Tuple2<K, V>> ceilingEntry(K key);

  default Option<K> headKey() {
    return headEntry().map(Tuple2::get1);
  }

  default Option<K> tailKey() {
    return tailEntry().map(Tuple2::get1);
  }

  default Option<K> higherKey(K key) {
    return higherEntry(key).map(Tuple2::get1);
  }

  default Option<K> lowerKey(K key) {
    return lowerEntry(key).map(Tuple2::get1);
  }

  default Option<K> floorKey(K key) {
    return floorEntry(key).map(Tuple2::get1);
  }

  default Option<K> ceilingKey(K key) {
    return floorEntry(key).map(Tuple2::get1);
  }

  @Override
  default <A, B> ImmutableTreeMap<A, B> map(Function1<K, A> keyMapper, Function1<V, B> valueMapper) {
    return ImmutableTreeMap.from(entries().map(tuple -> tuple.map(keyMapper, valueMapper)));
  }

  @Override
  default <A> ImmutableTreeMap<A, V> mapKeys(Function1<K, A> mapper) {
    return ImmutableTreeMap.from(entries().map(tuple -> tuple.map1(mapper)));
  }

  @Override
  default <A> ImmutableTreeMap<K, A> mapValues(Function1<V, A> mapper) {
    return ImmutableTreeMap.from(entries().map(tuple -> tuple.map2(mapper)));
  }

  @Override
  default ImmutableTreeMap<K, V> filterKeys(Matcher1<K> filter) {
    return ImmutableTreeMap.from(entries().filter(tuple -> filter.match(tuple.get1())));
  }

  @Override
  default ImmutableTreeMap<K, V> filterValues(Matcher1<V> filter) {
    return ImmutableTreeMap.from(entries().filter(tuple -> filter.match(tuple.get2())));
  }

  @Override
  default ImmutableTreeMap<K, V> putIfAbsent(K key, V value) {
    if (containsKey(key)) {
      return this;
    }
    return put(key, value);
  }

  @Override
  default V getOrDefault(K key, Producer<V> supplier) {
    return get(key).getOrElse(supplier);
  }

  @Override
  default boolean isEmpty() {
    return size() == 0;
  }

  @SafeVarargs
  static <K, V> ImmutableTreeMap<K, V> of(Tuple2<K, V>... entries) {
    return from(ImmutableSet.of(entries));
  }

  static <K, V> Tuple2<K, V> entry(K key, V value) {
    return Tuple2.of(key, value);
  }

  static <K, V> ImmutableTreeMap<K, V> from(NavigableMap<K, V> map) {
    return new JavaBasedImmutableTreeMap<>(map);
  }

  static <K, V> ImmutableTreeMap<K, V> empty() {
    return new JavaBasedImmutableTreeMap<>(emptyNavigableMap());
  }

  static <K, V> ImmutableTreeMap<K, V> from(Stream<Tuple2<K, V>> entries) {
    return from(ImmutableSet.from(entries));
  }

  static <K, V> ImmutableTreeMap<K, V> from(ImmutableSet<Tuple2<K, V>> entries) {
    return new JavaBasedImmutableTreeMap<>(entries.stream()
        .collect(Collectors.toMap(Tuple2::get1, Tuple2::get2, ImmutableTreeModule.throwingMerge(), TreeMap::new)));
  }

  static <K, V> Builder<K, V> builder() {
    return new Builder<>();
  }

  final class Builder<K, V> {

    private final NavigableMap<K, V> map = new TreeMap<>();

    private Builder() { }

    public Builder<K, V> put(K key, V value) {
      map.put(key, value);
      return this;
    }

    public ImmutableTreeMap<K, V> build() {
      return ImmutableTreeMap.from(map);
    }
  }

  final class JavaBasedImmutableTreeMap<K, V> implements ImmutableTreeMap<K, V>, Serializable {

    private static final long serialVersionUID = 8618845296089216532L;

    private final NavigableMap<K, V> backend;

    private JavaBasedImmutableTreeMap(NavigableMap<K, V> backend) {
      this.backend = requireNonNull(backend);
    }

    @Override
    public Map<K, V> toMap() {
      return toNavigableMap();
    }

    @Override
    public NavigableMap<K, V> toNavigableMap() {
      return new TreeMap<>(backend);
    }

    @Override
    public ImmutableTreeMap<K, V> put(K key, V value) {
      NavigableMap<K, V> newMap = toNavigableMap();
      newMap.put(key, value);
      return new JavaBasedImmutableTreeMap<>(newMap);
    }

    @Override
    public ImmutableTreeMap<K, V> remove(K key) {
      NavigableMap<K, V> newMap = toNavigableMap();
      newMap.remove(key);
      return new JavaBasedImmutableTreeMap<>(newMap);
    }

    @Override
    public Option<V> get(K key) {
      return Option.of(() -> backend.get(key));
    }

    @Override
    public ImmutableTreeMap<K, V> merge(K key, V value, Operator2<V> merger) {
      NavigableMap<K, V> newMap = toNavigableMap();
      newMap.merge(key, value, merger::apply);
      return new JavaBasedImmutableTreeMap<>(newMap);
    }

    @Override
    public ImmutableTreeMap<K, V> headMap(K toKey) {
      return new JavaBasedImmutableTreeMap<>(backend.headMap(toKey, false));
    }

    @Override
    public ImmutableTreeMap<K, V> tailMap(K fromKey) {
      return new JavaBasedImmutableTreeMap<>(backend.tailMap(fromKey, false));
    }

    @Override
    public Option<Tuple2<K, V>> headEntry() {
      return Try.of(() -> Tuple.from(backend.firstEntry())).toOption();
    }

    @Override
    public Option<Tuple2<K, V>> tailEntry() {
      return Try.of(() -> Tuple.from(backend.lastEntry())).toOption();
    }

    @Override
    public Option<Tuple2<K, V>> higherEntry(K key) {
      return Try.of(() -> Tuple.from(backend.higherEntry(key))).toOption();
    }

    @Override
    public Option<Tuple2<K, V>> lowerEntry(K key) {
      return Try.of(() -> Tuple.from(backend.lowerEntry(key))).toOption();
    }

    @Override
    public Option<Tuple2<K, V>> floorEntry(K key) {
      return Try.of(() -> Tuple.from(backend.floorEntry(key))).toOption();
    }

    @Override
    public Option<Tuple2<K, V>> ceilingEntry(K key) {
      return Try.of(() -> Tuple.from(backend.ceilingEntry(key))).toOption();
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
      return ImmutableSet.from(backend.entrySet()).map(Tuple::from);
    }

    @Override
    public int size() {
      return backend.size();
    }

    @Override
    public int hashCode() {
      return Objects.hash(backend);
    }

    @Override
    public boolean equals(Object obj) {
      return Equal.of(this)
          .append((a, b) -> Objects.equals(a.backend, b.backend))
          .applyTo(obj);
    }

    @Override
    public String toString() {
      return "ImmutableTreeMap(" + backend + ")";
    }
  }
}

interface ImmutableTreeModule {

  static <V> BinaryOperator<V> throwingMerge() {
    return (a, b) -> { throw new IllegalArgumentException("conflict detected"); };
  }
}