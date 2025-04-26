/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import com.github.tonivade.purefun.core.Consumer2;
import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.Operator2;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.type.Option;

/**
 * Similar to a HashMap
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public interface ImmutableMap<K, V> extends Iterable<Tuple2<K, V>> {

  Map<K, V> toMap();

  ImmutableMap<K, V> put(K key, V value);

  ImmutableMap<K, V> putAll(ImmutableMap<? extends K, ? extends V> other);

  ImmutableMap<K, V> remove(K key);

  ImmutableMap<K, V> removeAll(Sequence<? extends K> keys);

  Option<V> get(K key);

  Sequence<V> values();

  ImmutableSet<K> keys();

  ImmutableSet<Tuple2<K, V>> entries();

  ImmutableMap<K, V> merge(K key, V value, Operator2<V> merger);

  int size();

  @Override
  default Iterator<Tuple2<K, V>> iterator() {
    return entries().iterator();
  }

  default void forEach(Consumer2<? super K, ? super V> consumer) {
    entries().forEach(tuple -> consumer.accept(tuple.get1(), tuple.get2()));
  }

  default <A, B> ImmutableMap<A, B> bimap(
      Function1<? super K, ? extends A> keyMapper,
      Function1<? super V, ? extends B> valueMapper) {
    return ImmutableMap.from(entries().map(tuple -> tuple.map(keyMapper, valueMapper)));
  }

  default <A> ImmutableMap<A, V> mapKeys(Function1<? super K, ? extends A> mapper) {
    return ImmutableMap.from(entries().map(tuple -> tuple.map1(mapper)));
  }

  default <A> ImmutableMap<K, A> mapValues(Function1<? super V, ? extends A> mapper) {
    return ImmutableMap.from(entries().map(tuple -> tuple.map2(mapper)));
  }

  default ImmutableMap<K, V> filterKeys(Matcher1<? super K> filter) {
    return ImmutableMap.from(entries().filter(tuple -> filter.match(tuple.get1())));
  }

  default ImmutableMap<K, V> filterValues(Matcher1<? super V> filter) {
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

  default V getOrDefault(K key, Producer<? extends V> supplier) {
    return get(key).getOrElse(supplier);
  }

  default boolean isEmpty() {
    return size() == 0;
  }

  @SafeVarargs
  static <K, V> ImmutableMap<K, V> of(Tuple2<K, V>... entries) {
    return from(ImmutableSet.of(entries));
  }

  static <K, V> Tuple2<K, V> entry(K key, V value) {
    return Tuple2.of(key, value);
  }

  static <K, V> ImmutableMap<K, V> from(Map<K, V> map) {
    return from(map.entrySet());
  }

  @SuppressWarnings("unchecked")
  static <K, V> ImmutableMap<K, V> empty() {
    return (ImmutableMap<K, V>) PImmutableMap.EMPTY;
  }

  static <K, V> ImmutableMap<K, V> from(Iterable<Tuple2<K, V>> entries) {
    return from(ImmutableSet.from(entries));
  }

  static <K, V> ImmutableMap<K, V> from(Stream<Tuple2<K, V>> entries) {
    return from(ImmutableSet.from(entries));
  }

  static <K, V> ImmutableMap<K, V> from(ImmutableSet<Tuple2<K, V>> entries) {
    LinkedHashMap<K, V> collect = entries.stream().collect(toLinkedHashMap(Tuple2::get1, Tuple2::get2));
    return new PImmutableMap<>(collect);
  }

  static <K, V> ImmutableMap<K, V> from(Set<? extends Map.Entry<K, V>> entries) {
    LinkedHashMap<K, V> collect = entries.stream().collect(toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));
    return new PImmutableMap<>(collect);
  }

  static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
      Function1<? super T, ? extends K> keyMapper, Function1<? super T, ? extends V> valueMapper) {
    Collector<T, ?, ? extends LinkedHashMap<K, V>> toLinkedHashMap = toLinkedHashMap(keyMapper, valueMapper);
    return collectingAndThen(toLinkedHashMap, PImmutableMap::new);
  }

  static <K, V> Builder<K, V> builder() {
    return new Builder<>();
  }

  final class Builder<K, V> {

    private final Map<K, V> map = new HashMap<>();

    private Builder() {
    }

    public Builder<K, V> put(K key, V value) {
      map.put(key, value);
      return this;
    }

    public ImmutableMap<K, V> build() {
      return ImmutableMap.from(map);
    }
  }

  final class PImmutableMap<K, V> implements ImmutableMap<K, V>, Serializable {

    @Serial
    private static final long serialVersionUID = -7846127227891259826L;

    private static final ImmutableMap<?, ?> EMPTY = new PImmutableMap<>(HashTreePMap.empty());

    private static final Equal<PImmutableMap<?, ?>> EQUAL =
        Equal.<PImmutableMap<?, ?>>of().comparing(a -> a.backend);

    private final PMap<K, V> backend;

    private PImmutableMap(Map<K, V> backend) {
      this(HashTreePMap.from(backend));
    }

    private PImmutableMap(PMap<K, V> backend) {
      this.backend = checkNonNull(backend);
    }

    @Override
    public Map<K, V> toMap() {
      return new HashMap<>(backend);
    }

    @Override
    public ImmutableMap<K, V> put(K key, V value) {
      return new PImmutableMap<>(backend.plus(key, value));
    }

    @Override
    public ImmutableMap<K, V> putAll(ImmutableMap<? extends K, ? extends V> other) {
      return new PImmutableMap<>(backend.plusAll(other.toMap()));
    }

    @Override
    public ImmutableMap<K, V> remove(K key) {
      return new PImmutableMap<>(backend.minus(key));
    }

    @Override
    public ImmutableMap<K, V> removeAll(Sequence<? extends K> keys) {
      return new PImmutableMap<>(backend.minusAll(keys.toCollection()));
    }

    @Override
    public Option<V> get(K key) {
      return Option.of(backend.get(key));
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
    public ImmutableMap<K, V> merge(K key, V value, Operator2<V> merger) {
      var oldValue = backend.get(key);
      var newValue = oldValue == null ? value : merger.apply(oldValue, value);
      if (newValue == null) {
        return new PImmutableMap<>(backend.minus(key));
      }
      return new PImmutableMap<>(backend.plus(key, newValue));
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
      return EQUAL.applyTo(this, obj);
    }

    @Override
    public String toString() {
      return "ImmutableMap(" + backend + ")";
    }

    @Serial
    private Object readResolve() {
      if (backend.isEmpty()) {
        return EMPTY;
      }
      return this;
    }
  }

  private static <T, K, V> Collector<T, ?, ? extends LinkedHashMap<K, V>> toLinkedHashMap(
      Function1<? super T, ? extends K> keyMapper,
      Function1<? super T, ? extends V> valueMapper) {
    return Collectors.toMap(keyMapper::apply, valueMapper::apply, ImmutableMap::throwingMerge, LinkedHashMap::new);
  }

  private static <V> V throwingMerge(V a, V b) {
    throw new IllegalArgumentException("conflict detected");
  }
}