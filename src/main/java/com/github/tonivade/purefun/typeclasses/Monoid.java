/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

public interface Monoid<T> extends Semigroup<T> {

  T zero();

  static Monoid<String> string() {
    return new Monoid<String>() {

      @Override
      public String zero() {
        return "";
      }

      @Override
      public String combine(String t1, String t2) {
        return t1 + t2;
      }
    };
  }

  static Monoid<Integer> integer() {
    return new Monoid<Integer>() {

      @Override
      public Integer zero() {
        return 0;
      }

      @Override
      public Integer combine(Integer t1, Integer t2) {
        return t1 + t2;
      }
    };
  }
}