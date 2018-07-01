package com.github.tonivade.zeromock.core;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ImmutableArray<E> extends Sequence<E> {

  List<E> toList();

  @Override
  ImmutableArray<E> append(E element);
  @Override
  ImmutableArray<E> remove(E element);
  @Override
  ImmutableArray<E> appendAll(Sequence<E> other);
  
  ImmutableArray<E> sort(Comparator<E> comparator);

  Option<E> get(int position);
  
  ImmutableArray<E> set(int position, E element);
  ImmutableArray<E> add(int position, E element);
  
  default ImmutableArray<E> drop(int n) {
    return ImmutableArray.from(stream().skip(n));
  }

  @Override
  default <R> ImmutableArray<R> map(Function1<E, R> mapper) {
    return ImmutableArray.from(stream().map(mapper::apply));
  }

  @Override
  default <R> ImmutableArray<R> flatMap(SequenceHandler<E, R> mapper) {
    return ImmutableArray.from(stream().flatMap(mapper.toStreamHandler()::apply));
  }

  @Override
  default ImmutableArray<E> filter(Matcher<E> matcher) {
    return ImmutableArray.from(stream().filter(matcher::match));
  }
  
  static <T> ImmutableArray<T> from(Collection<T> collection) {
    return new JavaBasedImmutableArray<>(new ArrayList<>(collection));
  }
  
  static <T> ImmutableArray<T> from(Stream<T> stream) {
    return new JavaBasedImmutableArray<>(stream.collect(Collectors.toList()));
  }
  
  @SafeVarargs
  static <T> ImmutableArray<T> of(T... elements) {
    return new JavaBasedImmutableArray<>(Arrays.asList(elements));
  }

  static <T> ImmutableArray<T> empty() {
    return new JavaBasedImmutableArray<>(Collections.emptyList());
  }
  
  final class JavaBasedImmutableArray<E> implements ImmutableArray<E> {

    private final List<E> backend;

    public JavaBasedImmutableArray(List<E> backend) {
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
    public Sequence<E> reverse() {
      List<E> list = toList();
      Collections.reverse(list);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public Iterator<E> iterator() {
      return backend.iterator();
    }

    @Override
    public ImmutableArray<E> append(E element) {
      List<E> list = toList();
      list.add(element);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> remove(E element) {
      List<E> list = toList();
      list.remove(element);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> appendAll(Sequence<E> other) {
      List<E> list = toList();
      for (E element : other) {
        list.add(element);
      }
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public ImmutableArray<E> sort(Comparator<E> comparator) {
      List<E> list = toList();
      Collections.sort(list, comparator);
      return new JavaBasedImmutableArray<>(list);
    }

    @Override
    public Option<E> get(int position) {
      return Try.of(() -> backend.get(position)).toOption();
    }

    @Override
    public ImmutableArray<E> set(int position, E element) {
      List<E> list = toList();
      list.set(position, element);
      return new JavaBasedImmutableArray<>(list);
    }
    
    @Override
    public ImmutableArray<E> add(int position, E element) {
      List<E> list = toList();
      list.add(position, element);
      return new JavaBasedImmutableArray<>(list);
    }
    
    @Override
    public List<E> toList() {
      return new ArrayList<>(backend);
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
      return "ImmutableArray(" + backend + ")";
    }
    
  }
}
