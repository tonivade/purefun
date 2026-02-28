/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static java.util.stream.Collectors.collectingAndThen;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.pcollections.PSortedMap;
import org.pcollections.TreePMap;

import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.Operator2;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

/**
 * Similar to a TreeMap
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public interface ImmutableTreeMap<K, V> extends ImmutableMap<K, V> {

  Comparator<K> comparator();

  NavigableMap<K, V> toNavigableMap();

  default SequencedMap<K, V> toSequencedMap() {
    return toNavigableMap();
  }

  default SortedMap<K, V> toSortedMap() {
    return toNavigableMap();
  }

  @Override
  ImmutableTreeMap<K, V> put(K key, V value);

  @Override
  ImmutableTreeMap<K, V> putAll(ImmutableMap<? extends K, ? extends V> other);

  @Override
  ImmutableTreeMap<K, V> remove(K key);

  @Override
  ImmutableTreeMap<K, V> removeAll(Sequence<? extends K> keys);

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
  default <A, B> ImmutableTreeMap<A, B> bimap(Function1<? super K, ? extends A> keyMapper, Function1<? super V, ? extends B> valueMapper) {
    return ImmutableTreeMap.from(entries().map(tuple -> tuple.map(keyMapper, valueMapper)));
  }

  default <A, B> ImmutableTreeMap<A, B> bimap(Comparator<? super A> comparator, Function1<? super K, ? extends A> keyMapper, Function1<? super V, ? extends B> valueMapper) {
    return ImmutableTreeMap.from(comparator, entries().map(tuple -> tuple.map(keyMapper, valueMapper)));
  }

  @Override
  default <A> ImmutableTreeMap<A, V> mapKeys(Function1<? super K, ? extends A> mapper) {
    return ImmutableTreeMap.from(entries().map(tuple -> tuple.map1(mapper)));
  }

  default <A> ImmutableTreeMap<A, V> mapKeys(Comparator<? super A> comparator, Function1<? super K, ? extends A> mapper) {
    return ImmutableTreeMap.from(comparator, entries().map(tuple -> tuple.map1(mapper)));
  }

  @Override
  default <A> ImmutableTreeMap<K, A> mapValues(Function1<? super V, ? extends A> mapper) {
    return ImmutableTreeMap.from(entries().map(tuple -> tuple.map2(mapper)));
  }

  @Override
  default ImmutableTreeMap<K, V> filterKeys(Matcher1<? super K> filter) {
    return ImmutableTreeMap.from(entries().filter(tuple -> filter.match(tuple.get1())));
  }

  @Override
  default ImmutableTreeMap<K, V> filterValues(Matcher1<? super V> filter) {
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
  default V getOrDefault(K key, Producer<? extends V> supplier) {
    return get(key).getOrElse(supplier);
  }

  @Override
  default boolean isEmpty() {
    return size() == 0;
  }

  @SafeVarargs
  static <K, V> ImmutableTreeMap<K, V> of(Tuple2<K, V>... entries) {
    return from(naturalOrder(), ImmutableSet.of(entries));
  }

  static <K, V> Tuple2<K, V> entry(K key, V value) {
    return Tuple2.of(key, value);
  }

  static <K, V> ImmutableTreeMap<K, V> from(Comparator<? super K> comparator, Map<K, V> map) {
    return new PImmutableTreeMap<>(comparator, map);
  }

  static <K, V> ImmutableTreeMap<K, V> from(Map<K, V> map) {
    return from(naturalOrder(), map);
  }

  @SuppressWarnings("unchecked")
  static <K, V> ImmutableTreeMap<K, V> empty() {
    return (ImmutableTreeMap<K, V>) PImmutableTreeMap.EMPTY;
  }

  static <K, V> ImmutableTreeMap<K, V> empty(Comparator<? super K> comparator) {
    return new PImmutableTreeMap<>(TreePMap.empty(comparator));
  }

  static <K, V> ImmutableTreeMap<K, V> from(ImmutableSet<Tuple2<K, V>> entries) {
    return from(naturalOrder(), entries);
  }

  static <K, V> ImmutableTreeMap<K, V> from(Comparator<? super K> comparator, ImmutableSet<Tuple2<K, V>> entries) {
    return Pipeline.<Tuple2<K, V>>identity()
      .finish(Finisher.toImmutableTreeMap(entries, comparator, Tuple2::get1, Tuple2::get2));
  }

  static <K, V> ImmutableTreeMap<K, V> from(Set<Map.Entry<K, V>> entries) {
    return from(naturalOrder(), entries);
  }

  static <K, V> ImmutableTreeMap<K, V> from(Comparator<? super K> comparator, Set<Map.Entry<K, V>> entries) {
    return Pipeline.<Map.Entry<K, V>>identity()
      .finish(Finisher.toImmutableTreeMap(entries, comparator, Map.Entry::getKey, Map.Entry::getValue));
  }

  static <T, K, V> Collector<T, ?, ImmutableTreeMap<K, V>> toImmutableTreeMap(
      Function1<? super T, ? extends K> keyMapper, Function1<? super T, ? extends V> valueMapper) {
    return toImmutableTreeMap(naturalOrder(), keyMapper, valueMapper);
  }

  static <T, K, V> Collector<T, ?, ImmutableTreeMap<K, V>> toImmutableTreeMap(
      Comparator<? super K> comparator, Function1<? super T, ? extends K> keyMapper, Function1<? super T, ? extends V> valueMapper) {
    Collector<T, ?, ? extends TreeMap<K, V>> toTreeMap = toTreeMap(comparator, keyMapper, valueMapper);
    return collectingAndThen(toTreeMap, PImmutableTreeMap::new);
  }

  static <K extends Comparable<?>, V> Builder<K, V> builder() {
    return new Builder<>();
  }

  final class Builder<K extends Comparable<?>, V> {

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

  @SuppressWarnings("unchecked")
  private static <R> Comparator<R> naturalOrder() {
    return (Comparator<R>) Comparator.naturalOrder();
  }

  final class PImmutableTreeMap<K, V> implements ImmutableTreeMap<K, V>, Serializable {

    @Serial
    private static final long serialVersionUID = -3269335569221894587L;

    private static final ImmutableTreeMap<?, ?> EMPTY = new PImmutableTreeMap<>(TreePMap.empty(naturalOrder()));

    private static final Equal<PImmutableTreeMap<?, ?>> EQUAL =
        Equal.<PImmutableTreeMap<?, ?>>of().comparing(a -> a.backend);

    private final PSortedMap<K, V> backend;

    private PImmutableTreeMap(Comparator<? super K> comparator, Map<? extends K, ? extends V> backend) {
      this(TreePMap.from(comparator, backend));
    }

    private PImmutableTreeMap(SortedMap<K, V> backend) {
      this(TreePMap.fromSortedMap(backend));
    }

    private PImmutableTreeMap(PSortedMap<K, V> backend) {
      this.backend = checkNonNull(backend);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Comparator<K> comparator() {
      return (Comparator<K>) backend.comparator();
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
      return new PImmutableTreeMap<>(backend.plus(key, value));
    }

    @Override
    public ImmutableTreeMap<K, V> putAll(ImmutableMap<? extends K, ? extends V> other) {
      return new PImmutableTreeMap<>(backend.plusAll(other.toMap()));
    }

    @Override
    public ImmutableTreeMap<K, V> remove(K key) {
      return new PImmutableTreeMap<>(backend.minus(key));
    }

    @Override
    public ImmutableTreeMap<K, V> removeAll(Sequence<? extends K> keys) {
      return new PImmutableTreeMap<>(backend.minusAll(keys.toCollection()));
    }

    @Override
    public Option<V> get(K key) {
      return Option.of(backend.get(key));
    }

    @Override
    public ImmutableTreeMap<K, V> merge(K key, V value, Operator2<V> merger) {
      var oldValue = backend.get(key);
      var newValue = oldValue == null ? value : merger.apply(oldValue, value);
      if (newValue == null) {
        return new PImmutableTreeMap<>(backend.minus(key));
      }
      return new PImmutableTreeMap<>(backend.plus(key, newValue));
    }

    @Override
    public ImmutableTreeMap<K, V> headMap(K toKey) {
      return new PImmutableTreeMap<>(backend.headMap(toKey, false));
    }

    @Override
    public ImmutableTreeMap<K, V> tailMap(K fromKey) {
      return new PImmutableTreeMap<>(backend.tailMap(fromKey, false));
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
      return EQUAL.applyTo(this, obj);
    }

    @Override
    public String toString() {
      return "ImmutableTreeMap(" + backend + ")";
    }

    @Serial
    private Object readResolve() {
      if (backend.isEmpty()) {
        return EMPTY;
      }
      return this;
    }
  }

  private static <T, K, V> Collector<T, ?, ? extends TreeMap<K, V>> toTreeMap(
      Comparator<? super K> comparator,
      Function1<? super T, ? extends K> keyMapper,
      Function1<? super T, ? extends V> valueMapper) {
    return Collectors.toMap(keyMapper, valueMapper, Finisher::throwingMerge, () -> new TreeMap<>(comparator));
  }
}