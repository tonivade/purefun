/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.data.Reducer.Step.more;
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
import java.util.stream.Stream;

import org.pcollections.ConsPStack;
import org.pcollections.PStack;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Matcher1;
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
  <R> ImmutableList<R> run(Pipeline<? extends Sequence<R>, E, R> pipeline);

  default ImmutableList<Tuple2<Integer, E>> zipWithIndex() {
    return run(Pipeline.zipWithIndex());
  }

  default ImmutableList<E> dropWhile(Matcher1<? super E> matcher) {
    return run(Pipeline.dropWhile(matcher));
  }

  default ImmutableList<E> takeWhile(Matcher1<? super E> matcher) {
    return run(Pipeline.takeWhile(matcher));
  }

  @Override
  default <R> ImmutableList<R> map(Function1<? super E, ? extends R> mapper) {
    return run(Pipeline.map(mapper));
  }

  @Override
  default <R> ImmutableList<R> flatMap(Function1<? super E, ? extends Kind<Sequence<?>, ? extends R>> mapper) {
    return run(Pipeline.flatMap(mapper.andThen(SequenceOf::toSequence)));
  }

  @Override
  default ImmutableList<E> filter(Matcher1<? super E> matcher) {
    return run(Pipeline.filter(matcher));
  }

  @Override
  default ImmutableList<E> filterNot(Matcher1<? super E> matcher) {
    return filter(matcher.negate());
  }

  static <T> ImmutableList<T> from(Iterable<? extends T> iterable) {
    return from(Sequence.asStream(iterable.iterator()));
  }

  static <T> ImmutableList<T> from(Stream<? extends T> stream) {
    ArrayList<T> collect = stream.collect(Collectors.toCollection(ArrayList::new));
    return PImmutableList.from(collect);
  }

  @SafeVarargs
  static <T> ImmutableList<T> of(T... elements) {
    return from(Arrays.stream(elements));
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
    public <R> ImmutableList<R> run(Pipeline<? extends Sequence<R>, E, R> pipeline) {
      var result = Pipeline.run(pipeline.fix(), (acc, e) -> more(acc.plus(acc.size(), e)), ConsPStack.empty(), this);
      return new PImmutableList<>(result);
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
