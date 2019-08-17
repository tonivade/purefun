/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.generic;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Function2;

public class HList<L extends HList<L>> {

  private HList() {}

  public static HNil nil() {
    return HNil.INSTANCE;
  }

  public static <E, L extends HList<L>> HCons<E, L> cons(E element, L list) {
    return new HCons<>(element, list);
  }

  public static <L extends HList<L>> HAppend<HNil, L, L> append() {
    return new HAppend<>((hnil, left) -> left);
  }

  public static <E, L extends HList<L>, R extends HList<R>, X extends HList<X>>
      HAppend<HCons<E, L>, R, HCons<E, X>> append(HAppend<L, R, X> append) {
    return new HAppend<>((left, right) -> cons(left.head(), append.append(left.tail(), right)));
  }

  public static final class HNil extends HList<HNil> {

    private static final HNil INSTANCE = new HNil();

    private HNil() { }

    @Override
    public int hashCode() {
      return 1;
    }

    @Override
    public boolean equals(Object obj) {
      return obj == this;
    }

    @Override
    public String toString() {
      return "HNil";
    }
  }

  public static final class HCons<E, L extends HList<L>> extends HList<HCons<E, L>> {

    private final E head;
    private final L tail;

    private HCons(E head, L tail) {
      this.head = requireNonNull(head);
      this.tail = requireNonNull(tail);
    }

    public E head() {
      return head;
    }

    public L tail() {
      return tail;
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
