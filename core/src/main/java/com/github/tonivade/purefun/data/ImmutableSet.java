/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static java.util.stream.Collectors.collectingAndThen;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.pcollections.HashTreePSet;
import org.pcollections.PSet;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.PartialFunction1;

/**
 * Similar to a HashSet
 * @param <E> the type of elements in this set
 */
public interface ImmutableSet<E> extends Sequence<E> {

  Set<E> toSet();

  @Override
  ImmutableSet<E> append(E element);
  @Override
  ImmutableSet<E> remove(E element);
  @Override
  ImmutableSet<E> appendAll(Sequence<? extends E> other);
  @Override
  ImmutableSet<E> removeAll(Sequence<? extends E> other);

  @Override
  ImmutableSet<E> reverse();

  ImmutableSet<E> union(ImmutableSet<? extends E> other);
  ImmutableSet<E> intersection(ImmutableSet<? extends E> other);
  ImmutableSet<E> difference(ImmutableSet<? extends E> other);

  @Override
  <R> ImmutableSet<R> apply(Pipeline<E, R> pipeline);

  @Override
  default <R> ImmutableSet<R> map(Function1<? super E, ? extends R> mapper) {
    return pipeline().<R>map(mapper).toImmutableSet();
  }

  @Override
  default <R> ImmutableSet<R> flatMap(Function1<? super E, ? extends Kind<Sequence<?>, ? extends R>> mapper) {
    return pipeline().<R>flatMap(mapper.andThen(SequenceOf::toSequence)).toImmutableSet();
  }

  @Override
  default ImmutableSet<E> filter(Matcher1<? super E> matcher) {
    return pipeline().filter(matcher).toImmutableSet();
  }

  @Override
  default ImmutableSet<E> filterNot(Matcher1<? super E> matcher) {
    return filter(matcher.negate());
  }

  @Override
  default <R> ImmutableSet<R> collect(PartialFunction1<? super E, ? extends R> function) {
    return pipeline().<R>mapFilter(function).toImmutableSet();
  }

  @Override
  default <U> Sequence<U> scanLeft(U initial, Function2<? super U, ? super E, ? extends U> combinator) {
    return pipeline().scan(initial, combinator).toImmutableSet();
  }

  static <T> ImmutableSet<T> from(Iterable<? extends T> iterable) {
    return Pipeline.<T>identity().finish(Finisher.toImmutableSet(iterable));
  }

  @SafeVarargs
  static <T> ImmutableSet<T> of(T... elements) {
    return from(Arrays.asList(elements));
  }

  @SuppressWarnings("unchecked")
  static <T> ImmutableSet<T> empty() {
    return (ImmutableSet<T>) PImmutableSet.EMPTY;
  }

  static <E> Collector<E, ?, ImmutableSet<E>> toImmutableSet() {
    return collectingAndThen(Collectors.toCollection(ArrayList::new), PImmutableSet::new);
  }

  final class PImmutableSet<E> implements ImmutableSet<E>, Serializable {

    @Serial
    private static final long serialVersionUID = -8988192488466183450L;

    private static final ImmutableSet<?> EMPTY = new PImmutableSet<>(HashTreePSet.empty());

    private static final Equal<PImmutableSet<?>> EQUAL = Equal.<PImmutableSet<?>>of().comparing(x -> x.backend);

    private final PSet<E> backend;

    private PImmutableSet(Collection<E> backend) {
      this(HashTreePSet.from(backend));
    }

    private PImmutableSet(PSet<E> backend) {
      this.backend = checkNonNull(backend);
    }

    @Override
    public int size() {
      return backend.size();
    }

    @Override
    public <R> ImmutableSet<R> apply(Pipeline<E, R> pipeline) {
      return pipeline.finish(Finisher.toImmutableSet(this));
    }

    @Override
    public boolean contains(Object element) {
      return backend.contains(element);
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
    public ImmutableSet<E> append(E element) {
      return new PImmutableSet<>(backend.plus(element));
    }

    @Override
    public ImmutableSet<E> remove(E element) {
      return new PImmutableSet<>(backend.minus(element));
    }

    @Override
    public ImmutableSet<E> appendAll(Sequence<? extends E> other) {
      return new PImmutableSet<>(backend.plusAll(other.toCollection()));
    }

    @Override
    public ImmutableSet<E> removeAll(Sequence<? extends E> other) {
      return new PImmutableSet<>(backend.minusAll(other.toCollection()));
    }

    @Override
    public ImmutableSet<E> reverse() {
      return this;
    }

    @Override
    public ImmutableSet<E> union(ImmutableSet<? extends E> other) {
      return new PImmutableSet<>(backend.plusAll(other.toCollection()));
    }

    @Override
    public ImmutableSet<E> intersection(ImmutableSet<? extends E> other) {
      // TODO: reimplement when retainingAll will be implemented
      Set<E> copy = new HashSet<>(backend);
      copy.retainAll(other.toCollection());
      return new PImmutableSet<>(copy);
    }

    @Override
    public ImmutableSet<E> difference(ImmutableSet<? extends E> other) {
      return new PImmutableSet<>(backend.minusAll(other.toCollection()));
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
      return "ImmutableSet(" + backend + ")";
    }

    @Serial
    private Object readResolve() {
      if (backend.isEmpty()) {
        return EMPTY;
      }
      return this;
    }
  }
}
