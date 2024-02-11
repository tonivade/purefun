/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.type.Option;

public interface ImmutableList<E> extends Sequence<E> {

  List<E> toList();

  @Override
  ImmutableList<E> append(E element);
  @Override
  ImmutableList<E> remove(E element);
  @Override
  ImmutableList<E> appendAll(Sequence<? extends E> other);
  @Override
  ImmutableList<E> removeAll(Sequence<? extends E> other);

  @Override
  ImmutableList<E> reverse();
  ImmutableList<E> sort(Comparator<? super E> comparator);

  default Option<E> head() {
    return Option.from(stream().findFirst());
  }

  default ImmutableList<E> tail() {
    return drop(1);
  }

  default ImmutableList<E> drop(int n) {
    return ImmutableList.from(stream().skip(n));
  }

  @Override
  default <R> ImmutableList<R> map(Function1<? super E, ? extends R> mapper) {
    return ImmutableList.from(stream().map(mapper::apply));
  }

  @Override
  default <R> ImmutableList<R> flatMap(Function1<? super E, ? extends Kind<Sequence_, ? extends R>> mapper) {
    return ImmutableList.from(stream().flatMap(mapper.andThen(SequenceOf::<R>narrowK).andThen(Sequence::stream)::apply));
  }

  @Override
  default ImmutableList<E> filter(Matcher1<? super E> matcher) {
    return ImmutableList.from(stream().filter(matcher::match));
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
    return new PImmutableList<>(collect);
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
    return collectingAndThen(Collectors.toCollection(ArrayList::new), PImmutableList::new);
  }
  
  final class PImmutableList<E> implements ImmutableList<E>, Serializable {
    
    private static final ImmutableList<?> EMPTY = new PImmutableList<>(TreePVector.empty());

    private static final Equal<PImmutableList<?>> EQUAL = Equal.<PImmutableList<?>>of().comparing(a -> a.backend);

    @Serial
    private static final long serialVersionUID = 8986736870796940350L;

    private final PVector<E> backend;
    
    private PImmutableList(Collection<E> backend) {
      this(TreePVector.from(backend));
    }
    
    private PImmutableList(PVector<E> backend) {
      this.backend = checkNonNull(backend);
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
    public Iterator<E> iterator() {
      return backend.iterator();
    }

    @Override
    public List<E> toList() {
      return new ArrayList<>(backend);
    }

    @Override
    public ImmutableList<E> append(E element) {
      return new PImmutableList<>(backend.plus(element));
    }

    @Override
    public ImmutableList<E> remove(E element) {
      return new PImmutableList<>(backend.minus(element));
    }

    @Override
    public ImmutableList<E> appendAll(Sequence<? extends E> other) {
      return new PImmutableList<>(backend.plusAll(other.toCollection()));
    }

    @Override
    public ImmutableList<E> removeAll(Sequence<? extends E> other) {
      return new PImmutableList<>(backend.minusAll(other.toCollection()));
    }

    @Override
    public ImmutableList<E> reverse() {
      var copy = new ArrayList<>(backend);
      Collections.reverse(copy);
      return new PImmutableList<>(copy);
    }

    @Override
    public ImmutableList<E> sort(Comparator<? super E> comparator) {
      var copy = new ArrayList<>(backend);
      copy.sort(comparator);
      return new PImmutableList<>(copy);
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
