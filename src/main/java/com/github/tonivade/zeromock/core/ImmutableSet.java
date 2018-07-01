/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ImmutableSet<E> extends Sequence<E> {
  
  Set<E> toSet();

  @Override
  ImmutableSet<E> append(E element);
  @Override
  ImmutableSet<E> remove(E element);
  @Override
  ImmutableSet<E> appendAll(Sequence<E> other);

  ImmutableSet<E> union(ImmutableSet<E> other);
  ImmutableSet<E> intersection(ImmutableSet<E> other);
  ImmutableSet<E> difference(ImmutableSet<E> other);

  @Override
  default <R> ImmutableSet<R> map(Function1<E, R> mapper) {
    return ImmutableSet.from(stream().map(mapper::apply));
  }

  @Override
  default <R> ImmutableSet<R> flatMap(SequenceHandler<E, R> mapper) {
    return ImmutableSet.from(stream().flatMap(mapper.toStreamHandler()::apply));
  }

  @Override
  default ImmutableSet<E> filter(Matcher<E> matcher) {
    return ImmutableSet.from(stream().filter(matcher::match));
  }
  
  static <T> ImmutableSet<T> from(Collection<T> collection) {
    return new JavaBasedImmutableSet<>(new HashSet<>(collection));
  }
  
  static <T> ImmutableSet<T> from(Stream<T> stream) {
    return new JavaBasedImmutableSet<>(stream.collect(Collectors.toSet()));
  }
  
  @SafeVarargs
  static <T> ImmutableSet<T> of(T... elements) {
    return new JavaBasedImmutableSet<>(new HashSet<>(asList(elements)));
  }

  static <T> ImmutableSet<T> empty() {
    return new JavaBasedImmutableSet<>(emptySet());
  }

  final class JavaBasedImmutableSet<E> implements ImmutableSet<E> {
    private final Set<E> backend;
    
    private JavaBasedImmutableSet(Set<E> backend) {
      this.backend = requireNonNull(backend);
    }
    
    @Override
    public int size() {
      return backend.size();
    }
    
    @Override
    public boolean contains(E element) {
      return backend.contains(element);
    }
    
    @Override
    public ImmutableSet<E> reverse() {
      return this;
    }
    
    @Override
    public ImmutableSet<E> append(E element) {
      Set<E> newSet = toSet();
      newSet.add(element);
      return new JavaBasedImmutableSet<>(newSet);
    }
    
    @Override
    public ImmutableSet<E> appendAll(Sequence<E> other) {
      Set<E> newSet = toSet();
      for (E element : other) {
        newSet.add(element);
      }
      return new JavaBasedImmutableSet<>(newSet);
    }
    
    @Override
    public ImmutableSet<E> remove(E element) {
      Set<E> newSet = toSet();
      newSet.remove(element);
      return new JavaBasedImmutableSet<>(newSet);
    }
    
    @Override
    public ImmutableSet<E> union(ImmutableSet<E> other) {
      Set<E> newSet = toSet();
      newSet.addAll(other.toSet());
      return new JavaBasedImmutableSet<>(newSet);
    }
    
    @Override
    public ImmutableSet<E> intersection(ImmutableSet<E> other) {
      Set<E> newSet = toSet();
      newSet.retainAll(other.toSet());
      return new JavaBasedImmutableSet<>(newSet);
    }
    
    @Override
    public ImmutableSet<E> difference(ImmutableSet<E> other) {
      Set<E> newSet = toSet();
      newSet.removeAll(other.toSet());
      return new JavaBasedImmutableSet<>(newSet);
    }
    
    @Override
    public Iterator<E> iterator() {
      return backend.iterator();
    }
    
    @Override
    public Set<E> toSet() {
      return new HashSet<>(backend);
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
      return "ImmutableSet(" + backend + ")";
    }
  }
}
