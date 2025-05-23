/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.core.Validator.greaterThan;
import static com.github.tonivade.purefun.core.Validator.nonNullAnd;
import static com.github.tonivade.purefun.data.Sequence.listOf;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.type.Option;

public final class NonEmptyList<E> implements ImmutableList<E>, Serializable {

  @Serial
  private static final long serialVersionUID = 3291606155276185601L;

  private static final Equal<NonEmptyList<?>> EQUAL = Equal.<NonEmptyList<?>>of().comparing(v -> v.value);

  private final ImmutableList<E> value;

  private NonEmptyList(ImmutableList<E> value) {
    this.value = value;
  }

  @Override
  public <R> NonEmptyList<R> map(Function1<? super E, ? extends R> mapper) {
    return of(value.map(mapper));
  }

  @Override
  public <R> NonEmptyList<R> flatMap(Function1<? super E, ? extends Kind<Sequence<?>, ? extends R>> mapper) {
    return of(value.flatMap(mapper));
  }

  @Override
  public ImmutableList<E> filter(Matcher1<? super E> matcher) {
    return value.filter(matcher);
  }

  @Override
  public ImmutableList<E> filterNot(Matcher1<? super E> matcher) {
    return filter(matcher.negate());
  }

  @Override
  public List<E> toList() {
    return value.toList();
  }

  @Override
  public int size() {
    return value.size();
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean contains(Object element) {
    return value.contains(element);
  }

  @Override
  public NonEmptyList<E> append(E element) {
    return of(value.append(element));
  }

  @Override
  public NonEmptyList<E> prepend(E element) {
    return of(value.prepend(element));
  }

  @Override
  public ImmutableList<E> remove(E element) {
    return value.remove(element);
  }

  @Override
  public NonEmptyList<E> appendAll(Sequence<? extends E> other) {
    return of(value.appendAll(other));
  }

  @Override
  public NonEmptyList<E> prependAll(Sequence<? extends E> other) {
    return of(value.prependAll(other));
  }

  @Override
  public ImmutableList<E> removeAll(Sequence<? extends E> other) {
    return of(value.removeAll(other));
  }

  @Override
  public NonEmptyList<E> reverse() {
    return of(value.reverse());
  }

  @Override
  public NonEmptyList<E> sort(Comparator<? super E> comparator) {
    return of(value.sort(comparator));
  }

  @Override
  public Iterator<E> iterator() {
    return value.iterator();
  }

  @Override
  public Option<E> head() {
    return value.head();
  }

  @Override
  public ImmutableList<E> drop(int n) {
    return value.drop(n);
  }

  @Override
  public ImmutableList<E> dropWhile(Matcher1<? super E> matcher) {
    return value.dropWhile(matcher);
  }

  @Override
  public ImmutableList<E> takeWhile(Matcher1<? super E> matcher) {
    return value.takeWhile(matcher);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public boolean equals(Object obj) {
    return EQUAL.applyTo(this, obj);
  }

  @Override
  public String toString() {
    return "NonEmptyList(" + value.toList() + ")";
  }

  @SafeVarargs
  public static <E> NonEmptyList<E> of(E value, E... values) {
    return of(listOf(value).appendAll(listOf(values)));
  }

  public static <E> NonEmptyList<E> of(ImmutableList<E> value) {
    return nonNullAnd(greaterThan(0, () -> "non empty list cannot be empty").<ImmutableList<E>>compose(ImmutableList::size))
        .validate(value).map(NonEmptyList::new).getOrElseThrow();
  }
}
