/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.Producer.unit;
import static java.util.Collections.emptyNavigableMap;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Operator2;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Equal;

public interface ImmutableTreeMap<K, V> extends ImmutableMap<K, V> {

  NavigableMap<K, V> toNavigableMap();

  @Override
  ImmutableTreeMap<K, V> put(K key, V value);
  @Override
  ImmutableTreeMap<K, V> remove(K key);

  ImmutableTreeMap<K, V> headTree(K toKey);
  ImmutableTreeMap<K, V> tailTree(K fromKey);
  Option<Tuple2<K, V>> head();
  Option<Tuple2<K, V>> tail();
  Option<Tuple2<K, V>> higher(K key);
  Option<Tuple2<K, V>> lower(K key);
  Option<Tuple2<K, V>> floor(K key);
  Option<Tuple2<K, V>> ceiling(K key);

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
  default ImmutableTreeMap<K, V> merge(K key, V value, Operator2<V> merger) {
    if (containsKey(key)) {
      return put(key, merger.apply(getOrDefault(key, unit(value)), value));
    }
    return put(key, value);
  }

  @Override
  default V getOrDefault(K key, Producer<V> supplier) {
    return get(key).orElse(supplier);
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

  final class JavaBasedImmutableTreeMap<K, V> implements ImmutableTreeMap<K, V> {

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
    public ImmutableTreeMap<K, V> headTree(K toKey) {
      return new JavaBasedImmutableTreeMap<>(backend.headMap(toKey, false));
    }

    @Override
    public ImmutableTreeMap<K, V> tailTree(K fromKey) {
      return new JavaBasedImmutableTreeMap<>(backend.tailMap(fromKey, false));
    }

    @Override
    public Option<Tuple2<K, V>> head() {
      return Option.of(() -> Tuple.from(backend.firstEntry()));
    }

    @Override
    public Option<Tuple2<K, V>> tail() {
      return Option.of(() -> Tuple.from(backend.lastEntry()));
    }

    @Override
    public Option<Tuple2<K, V>> higher(K key) {
      return Option.of(() -> Tuple.from(backend.higherEntry(key)));
    }

    @Override
    public Option<Tuple2<K, V>> lower(K key) {
      return Option.of(() -> Tuple.from(backend.lowerEntry(key)));
    }

    @Override
    public Option<Tuple2<K, V>> floor(K key) {
      return Option.of(() -> Tuple.from(backend.floorEntry(key)));
    }

    @Override
    public Option<Tuple2<K, V>> ceiling(K key) {
      return Option.of(() -> Tuple.from(backend.ceilingEntry(key)));
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