/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Matcher;
import com.github.tonivade.purefun.Monad;

public final class SequenceKind<T> implements Sequence<T>, Higher<SequenceKind.µ, T> {

  public static final class µ {}

  private final Sequence<T> delegate;

  public SequenceKind(Sequence<T> delegate) {
    this.delegate = requireNonNull(delegate);
  }

  @Override
  public Iterator<T> iterator() {
    return delegate.iterator();
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean contains(T element) {
    return delegate.contains(element);
  }

  @Override
  public Sequence<T> append(T element) {
    return delegate.append(element);
  }

  @Override
  public Sequence<T> remove(T element) {
    return delegate.remove(element);
  }

  @Override
  public Sequence<T> appendAll(Sequence<T> other) {
    return delegate.appendAll(other);
  }

  @Override
  public Sequence<T> reverse() {
    return delegate.reverse();
  }

  @Override
  public <R> Sequence<R> map(Function1<T, R> mapper) {
    return delegate.map(mapper);
  }

  @Override
  public <R> Sequence<R> flatMap(Function1<T, ? extends Monad<µ, R>> mapper) {
    return delegate.flatMap(mapper);
  }

  @Override
  public Sequence<T> filter(Matcher<T> matcher) {
    return delegate.filter(matcher);
  }

  public static <T> Sequence<T> narrowK(Higher<SequenceKind.µ, T> hkt) {
    return (Sequence<T>) hkt;
  }
}
