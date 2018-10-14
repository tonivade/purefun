/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.Producer.unit;
import static java.util.Collections.emptyNavigableMap;
import static java.util.Objects.requireNonNull;

import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tonivade.purefun.Consumer2;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Operator2;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Equal;

public interface ImmutableTree<K extends Comparable<K>, V> {

  NavigableMap<K, V> toNavigableMap();

  ImmutableTree<K, V> put(K key, V value);
  ImmutableTree<K, V> remove(K key);
  Option<V> get(K key);

  Sequence<V> values();
  ImmutableSet<K> keys();
  ImmutableSet<Tuple2<K, V>> entries();

  int size();

  default void forEach(Consumer2<K, V> consumer) {
    entries().forEach(tuple -> consumer.accept(tuple.get1(), tuple.get2()));
  }

  default <A extends Comparable<A>, B> ImmutableTree<A, B> map(Function1<K, A> keyMapper, Function1<V, B> valueMapper) {
    return ImmutableTree.from(entries().map(tuple -> tuple.map(keyMapper, valueMapper)));
  }

  default <A extends Comparable<A>> ImmutableTree<A, V> mapKeys(Function1<K, A> mapper) {
    return ImmutableTree.from(entries().map(tuple -> tuple.map1(mapper)));
  }

  default <A> ImmutableTree<K, A> mapValues(Function1<V, A> mapper) {
    return ImmutableTree.from(entries().map(tuple -> tuple.map2(mapper)));
  }

  default ImmutableTree<K, V> filterKeys(Matcher1<K> filter) {
    return ImmutableTree.from(entries().filter(tuple -> filter.match(tuple.get1())));
  }

  default ImmutableTree<K, V> filterValues(Matcher1<V> filter) {
    return ImmutableTree.from(entries().filter(tuple -> filter.match(tuple.get2())));
  }

  default boolean containsKey(K key) {
    return get(key).isPresent();
  }

  default ImmutableTree<K, V> putIfAbsent(K key, V value) {
    if (containsKey(key)) {
      return this;
    }
    return put(key, value);
  }

  default ImmutableTree<K, V> merge(K key, V value, Operator2<V> merger) {
    if (containsKey(key)) {
      return put(key, merger.apply(getOrDefault(key, unit(value)), value));
    }
    return put(key, value);
  }

  default V getOrDefault(K key, Producer<V> supplier) {
    return get(key).orElse(supplier);
  }

  default boolean isEmpty() {
    return size() == 0;
  }

  @SafeVarargs
  static <K extends Comparable<K>, V> ImmutableTree<K, V> of(Tuple2<K, V>... entries) {
    return from(ImmutableSet.of(entries));
  }

  static <K extends Comparable<K>, V> Tuple2<K, V> entry(K key, V value) {
    return Tuple2.of(key, value);
  }

  static <K extends Comparable<K>, V> ImmutableTree<K, V> from(NavigableMap<K, V> map) {
    return new JavaBasedImmutableTree<>(map);
  }

  static <K extends Comparable<K>, V> ImmutableTree<K, V> empty() {
    return new JavaBasedImmutableTree<>(emptyNavigableMap());
  }

  static <K extends Comparable<K>, V> ImmutableTree<K, V> from(Stream<Tuple2<K, V>> entries) {
    return from(ImmutableSet.from(entries));
  }

  static <K extends Comparable<K>, V> ImmutableTree<K, V> from(ImmutableSet<Tuple2<K, V>> entries) {
    return new JavaBasedImmutableTree<K, V>(entries.stream()
        .collect(Collectors.toMap(Tuple2::get1, Tuple2::get2, ImmutableTreeModule.throwingMerge(), TreeMap::new)));
  }

  final class JavaBasedImmutableTree<K extends Comparable<K>, V> implements ImmutableTree<K, V> {

    private final NavigableMap<K, V> backend;

    private JavaBasedImmutableTree(NavigableMap<K, V> backend) {
      this.backend = requireNonNull(backend);
    }

    @Override
    public NavigableMap<K, V> toNavigableMap() {
      return new TreeMap<>(backend);
    }

    @Override
    public ImmutableTree<K, V> put(K key, V value) {
      NavigableMap<K, V> newMap = toNavigableMap();
      newMap.put(key, value);
      return new JavaBasedImmutableTree<>(newMap);
    }

    @Override
    public ImmutableTree<K, V> remove(K key) {
      NavigableMap<K, V> newMap = toNavigableMap();
      newMap.remove(key);
      return new JavaBasedImmutableTree<>(newMap);
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
      return "ImmutableTree(" + backend + ")";
    }
  }
}

interface ImmutableTreeModule {

  static <V> BinaryOperator<V> throwingMerge() {
    return (a, b) -> { throw new IllegalArgumentException(); };
  }

}