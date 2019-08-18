/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.generic;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Tuple1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Tuple3;
import com.github.tonivade.purefun.Tuple4;
import com.github.tonivade.purefun.Tuple5;

public interface HList<L extends HList<L>> {

  int size();

  <E> HCons<E, L> prepend(E element);

  HListModule getModule();

  default boolean isEmpty() {
    return size() == 0;
  }

  static HNil empty() {
    return HNil.INSTANCE;
  }

  static <E, L extends HList<L>> HCons<E, L> cons(E element, L list) {
    return new HCons<>(element, list);
  }

  static <A> HCons<A, HNil> of(A element) {
    return empty().prepend(element);
  }

  static <A, B> HCons<A, HCons<B, HNil>> of(A element1, B element2) {
    return empty().prepend(element2).prepend(element1);
  }

  static <A, B, C> HCons<A, HCons<B, HCons<C, HNil>>> of(A element1, B element2, C element3) {
    return empty().prepend(element3).prepend(element2).prepend(element1);
  }

  static <A, B, C, D> HCons<A, HCons<B, HCons<C, HCons<D, HNil>>>> of(A element1,
                                                                      B element2,
                                                                      C element3,
                                                                      D element4) {
    return empty().prepend(element4).prepend(element3).prepend(element2).prepend(element1);
  }

  static <A, B, C, D, E> HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HNil>>>>> of(A element1,
                                                                                   B element2,
                                                                                   C element3,
                                                                                   D element4,
                                                                                   E element5) {
    return empty().prepend(element5).prepend(element4).prepend(element3).prepend(element2).prepend(element1);
  }

  static <A> HCons<A, HNil> from(Tuple1<A> tuple) {
    return tuple.applyTo(HList::of);
  }

  static <A, B> HCons<A, HCons<B, HNil>> from(Tuple2<A, B> tuple) {
    return tuple.applyTo(HList::of);
  }

  static <A, B, C> HCons<A, HCons<B, HCons<C, HNil>>> from(Tuple3<A, B, C> tuple) {
    return tuple.applyTo(HList::of);
  }

  static <A, B, C, D> HCons<A, HCons<B, HCons<C, HCons<D, HNil>>>> from(Tuple4<A, B, C, D> tuple) {
    return tuple.applyTo(HList::of);
  }

  static <A, B, C, D, E> HCons<A, HCons<B, HCons<C, HCons<D, HCons<E, HNil>>>>> from(Tuple5<A, B, C, D, E> tuple) {
    return tuple.applyTo(HList::of);
  }

  static <L extends HList<L>> HAppend<HNil, L, L> append() {
    return new HAppend<>((empty, left) -> left);
  }

  static <E, L extends HList<L>, R extends HList<R>, X extends HList<X>>
      HAppend<HCons<E, L>, R, HCons<E, X>> append(HAppend<L, R, X> append) {
    return new HAppend<>((left, right) -> cons(left.head(), append.append(left.tail(), right)));
  }

  public static final class HNil implements HList<HNil> {

    private static final HNil INSTANCE = new HNil();

    private HNil() {}

    @Override
    public int size() {
      return 0;
    }

    @Override
    public <E> HCons<E, HNil> prepend(E element) {
      return cons(element, this);
    }

    @Override
    public HListModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "HNil";
    }
  }

  public static final class HCons<H, T extends HList<T>> implements HList<HCons<H, T>> {

    private final H head;
    private final T tail;

    private HCons(H head, T tail) {
      this.head = requireNonNull(head);
      this.tail = requireNonNull(tail);
    }

    public H head() {
      return head;
    }

    public T tail() {
      return tail;
    }

    @Override
    public int size() {
      return 1 + tail.size();
    }

    @Override
    public <E> HCons<E, HCons<H, T>> prepend(E element) {
      return cons(element, this);
    }

    @Override
    public HListModule getModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
      return Objects.hash(head, tail);
    }

    @Override
    public boolean equals(Object obj) {
      return Equal.of(this)
          .comparing(HCons::head)
          .comparing(HCons::tail)
          .applyTo(obj);
    }

    @Override
    public String toString() {
      return "HCons(" + head + "," + tail + ")";
    }
  }

  public static final class HAppend<L extends HList<L>, R extends HList<R>, X extends HList<X>> {

    private final Function2<L, R, X> append;

    public HAppend(Function2<L, R, X> append) {
      this.append = requireNonNull(append);
    }

    public X append(L left, R right) {
      return append.apply(left, right);
    }
  }
}

interface HListModule {}
