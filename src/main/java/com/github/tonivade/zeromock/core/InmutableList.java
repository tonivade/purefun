/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class InmutableList<E> {
  
  private InmutableList() { }
  
  public abstract Stream<E> stream();
  public abstract int size();
  public abstract List<E> toMutable();
  public abstract boolean contains(E value);

  public Option<E> head() {
    return Option.from(stream().findFirst());
  }
  
  public InmutableList<E> tail() {
    return InmutableList.of(stream().skip(1));
  }

  public void forEach(Consumer<E> consumer) {
    stream().forEach(consumer);
  }
  
  public InmutableList<E> add(E element) {
    return concat(InmutableList.of(element));
  }

  public InmutableList<E> concat(InmutableList<E> other) {
    return InmutableList.of(Stream.concat(stream(), other.stream()));
  }

  public <R> InmutableList<R> map(Handler1<E, R> mapper) {
    return InmutableList.of(stream().map(mapper::handle));
  }

  public <R> InmutableList<R> flatMap(Handler1<E, InmutableList<R>> mapper) {
    return InmutableList.of(stream().flatMap(asStream(mapper)::handle));
  }

  public InmutableList<E> filter(Matcher<E> matcher) {
    return InmutableList.of(stream().filter(matcher::match));
  }
  
  public InmutableList<E> skip(int n) {
    return InmutableList.of(stream().skip(n));
  }

  public boolean isEmpty() {
    return size() == 0;
  }
  
  public static <T> InmutableList<T> of(List<T> list) {
    return new JavaBasedInmutableList<>(list);
  }
  
  public static <T> InmutableList<T> of(Stream<T> stream) {
    return new JavaBasedInmutableList<>(stream.collect(toList()));
  }
  
  @SafeVarargs
  public static <T> InmutableList<T> of(T... elements) {
    return new JavaBasedInmutableList<>(asList(elements));
  }

  public static <T> InmutableList<T> empty() {
    return new JavaBasedInmutableList<>(emptyList());
  }
  
  private static <T, R> StreamHandler<T, R> asStream(Handler1<T, InmutableList<R>> listHandler) {
    return listHandler.andThen(InmutableList::stream)::handle;
  }

  private static final class JavaBasedInmutableList<E> extends InmutableList<E> {
    private final List<E> backend;
    
    private JavaBasedInmutableList(List<E> backend) {
      this.backend = Objects.requireNonNull(backend);
    }
    
    @Override
    public Stream<E> stream() {
      return backend.stream();
    }
    
    @Override
    public int size() {
      return backend.size();
    }
    
    @Override
    public boolean contains(E value) {
      return backend.contains(value);
    }
    
    @Override
    public List<E> toMutable() {
      return new ArrayList<>(backend);
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(backend);
    }
    
    @Override
    public boolean equals(Object obj) {
      return Equal.equal(this)
          .append((a, b) -> Objects.equals(a.backend, b.backend))
          .applyTo(obj);
    }
    
    @Override
    public String toString() {
      return "InmutableList(" + backend + ")";
    }
  }
}
