/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.pcollections.ConsPStack;
import org.pcollections.PStack;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.PartialFunction1;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.type.Option;

/**
 * Similar to a LinkedList
 * @param <E> the type of elements in this list
 */
public interface ImmutableList<E> extends Sequence<E> {

  List<E> toList();

  @Override
  ImmutableList<E> append(E element);
  ImmutableList<E> prepend(E element);
  @Override
  ImmutableList<E> remove(E element);
  @Override
  ImmutableList<E> appendAll(Sequence<? extends E> other);
  ImmutableList<E> prependAll(Sequence<? extends E> other);
  @Override
  ImmutableList<E> removeAll(Sequence<? extends E> other);

  @Override
  ImmutableList<E> reverse();
  ImmutableList<E> sort(Comparator<? super E> comparator);

  Option<E> head();

  default ImmutableList<E> tail() {
    return drop(1);
  }

  ImmutableList<E> drop(int n);

  @Override
  <R> ImmutableList<R> apply(Pipeline<E, R> pipeline);

  default ImmutableList<Tuple2<Integer, E>> zipWithIndex() {
    return pipeline().zipWithIndex().finish(Finisher::toImmutableList);
  }

  default ImmutableList<E> dropWhile(Matcher1<? super E> matcher) {
    return pipeline().dropWhile(matcher).finish(Finisher::toImmutableList);
  }

  default ImmutableList<E> takeWhile(Matcher1<? super E> matcher) {
    return pipeline().takeWhile(matcher).finish(Finisher::toImmutableList);
  }

  @Override
  default <R> ImmutableList<R> map(Function1<? super E, ? extends R> mapper) {
    return pipeline().<R>map(mapper).finish(Finisher::toImmutableList);
  }

  @Override
  default <R> ImmutableList<R> flatMap(Function1<? super E, ? extends Kind<Sequence<?>, ? extends R>> mapper) {
    return pipeline().<R>flatMap(mapper.andThen(SequenceOf::toSequence)).finish(Finisher::toImmutableList);
  }

  @Override
  default ImmutableList<E> filter(Matcher1<? super E> matcher) {
    return pipeline().filter(matcher).finish(Finisher::toImmutableList);
  }

  @Override
  default ImmutableList<E> filterNot(Matcher1<? super E> matcher) {
    return filter(matcher.negate());
  }

  @Override
  default <R> ImmutableList<R> collect(PartialFunction1<? super E, ? extends R> function) {
    return pipeline().<R>mapFilter(function).finish(Finisher::toImmutableList);
  }

  @Override
  default <R> ImmutableList<R> scanLeft(R initial, Function2<? super R, ? super E, ? extends R> combinator) {
    return pipeline().scan(initial, combinator).finish(Finisher::toImmutableList);
  }

  static <T> ImmutableList<T> from(Iterable<? extends T> iterable) {
    return Pipeline.<T>identity().finish(Finisher.toImmutableList(iterable));
  }

  @SafeVarargs
  static <T> ImmutableList<T> of(T... elements) {
    return from(Arrays.asList(elements));
  }

  @SuppressWarnings("unchecked")
  static <T> ImmutableList<T> empty() {
    return (ImmutableList<T>) PImmutableList.EMPTY;
  }

  static <E> Collector<E, ?, ImmutableList<E>> toImmutableList() {
    return collectingAndThen(Collectors.toCollection(ArrayList::new), PImmutableList::from);
  }

  final class PImmutableList<E> implements ImmutableList<E>, Serializable {

    private static final ImmutableList<?> EMPTY = new PImmutableList<>(ConsPStack.empty());

    private static final Equal<PImmutableList<?>> EQUAL = Equal.<PImmutableList<?>>of().comparing(a -> a.backend);

    @Serial
    private static final long serialVersionUID = 8986736870796940350L;

    private final PStack<E> backend;

    static <E> ImmutableList<E> from(Collection<E> backend) {
      return from(ConsPStack.from(backend));
    }

    static <E> ImmutableList<E> from(PStack<E> backend) {
      if (backend.isEmpty()) {
        return empty();
      }
      return new PImmutableList<>(backend);
    }

    private PImmutableList(PStack<E> backend) {
      this.backend = checkNonNull(backend);
    }

    @Override
    public int size() {
      return backend.size();
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
    public List<E> toList() {
      return new ArrayList<>(backend);
    }

    @Override
    public <R> ImmutableList<R> apply(Pipeline<E, R> pipeline) {
      return pipeline.finish(Finisher.toImmutableList(this));
    }

    @Override
    public ImmutableList<E> append(E element) {
      return from(backend.plus(backend.size(), element));
    }

    @Override
    public ImmutableList<E> prepend(E element) {
      return from(backend.plus(element));
    }

    @Override
    public ImmutableList<E> remove(E element) {
      return from(backend.minus(element));
    }

    @Override
    public ImmutableList<E> appendAll(Sequence<? extends E> other) {
      return from(backend.plusAll(backend.size(), other.toSequencedCollection().reversed()));
    }

    @Override
    public ImmutableList<E> prependAll(Sequence<? extends E> other) {
      return from(backend.plusAll(other.toCollection()));
    }

    @Override
    public ImmutableList<E> removeAll(Sequence<? extends E> other) {
      return from(backend.minusAll(other.toCollection()));
    }

    @Override
    public ImmutableList<E> reverse() {
      return from(backend.reversed());
    }

    @Override
    public Option<E> head() {
      if (isEmpty()) {
        return Option.none();
      }
      return Option.some(backend.getFirst());
    }

    @Override
    public ImmutableList<E> drop(int n) {
      if (n >= backend.size()) {
        return empty();
      }
      return from(backend.subList(n));
    }

    @Override
    public ImmutableList<E> sort(Comparator<? super E> comparator) {
      var copy = new ArrayList<>(backend);
      copy.sort(comparator);
      return PImmutableList.from(copy);
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
      return "ImmutableList(" + backend + ")";
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
