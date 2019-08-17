/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.generic;

import static com.github.tonivade.purefun.generic.HList.append;
import static com.github.tonivade.purefun.generic.HList.cons;
import static com.github.tonivade.purefun.generic.HList.nil;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.generic.HList.HAppend;
import com.github.tonivade.purefun.generic.HList.HCons;
import com.github.tonivade.purefun.generic.HList.HNil;

public class HListTest {

  @Test
  public void hlist() {
    HCons<String, HCons<Integer, HNil>> hlist = cons("Hola", cons(42, nil()));

    assertAll(
        () -> assertEquals("Hola", hlist.head()),
        () -> assertEquals(42, hlist.tail().head()),
        () -> assertEquals(nil(), hlist.tail().tail()),
        () -> assertEquals(cons("Hola", cons(42, nil())), hlist),
        () -> assertEquals("HCons(Hola,HCons(42,HNil))", hlist.toString())
      );
  }

  @Test
  public void appendLists() {
    HCons<Boolean, HNil> alist = cons(true, nil());
    HCons<String, HCons<Integer, HNil>> blist = cons("Hola", cons(42, nil()));

    HAppend<HNil,
            HCons<String, HCons<Integer, HNil>>,
            HCons<String, HCons<Integer, HNil>>> zero = append();
    HAppend<HCons<Boolean, HNil>,
            HCons<String, HCons<Integer, HNil>>,
            HCons<Boolean, HCons<String, HCons<Integer, HNil>>>> one = append(zero);

    HCons<Boolean, HCons<String, HCons<Integer, HNil>>> append = one.append(alist, blist);

    assertAll(
        () -> assertEquals(true, append.head()),
        () -> assertEquals("Hola", append.tail().head()),
        () -> assertEquals(42, append.tail().tail().head()),
        () -> assertEquals(nil(), append.tail().tail().tail()),
        () -> assertEquals("HCons(true,HCons(Hola,HCons(42,HNil)))", append.toString())
      );
  }
}
