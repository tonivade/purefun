/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.generic;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Sealed;
import com.github.tonivade.purefun.Tuple1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Tuple3;
import com.github.tonivade.purefun.Tuple4;
import com.github.tonivade.purefun.Tuple5;
import com.github.tonivade.purefun.type.Option;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Sealed
public interface HList<L extends HList<L>> {

  int size();

  <E> HCons<E, L> prepend(E element);

  <E> Option<E> find(Class<? extends E> clazz);

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

  final class HNil implements HList<HNil> {

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
    public <E> Option<E> find(Class<? extends E> clazz) {
      return Option.none();
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

  final class HCons<H, T extends HList<T>> implements HList<HCons<H, T>> {

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
    public <E> Option<E> find(Class<? extends E> clazz) {
      if (clazz.isInstance(head)) {
        return Option.some(clazz.cast(head));
      }
      return tail.find(clazz);
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
}

interface HListModule {}
