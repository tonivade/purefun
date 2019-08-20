/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.generic;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.generic.HList.append;
import static com.github.tonivade.purefun.generic.HList.combine;
import static com.github.tonivade.purefun.generic.HList.compose;
import static com.github.tonivade.purefun.generic.HList.empty;
import static com.github.tonivade.purefun.generic.HList.foldr;
import static com.github.tonivade.purefun.type.Option.none;
import static com.github.tonivade.purefun.type.Option.some;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.generic.HList.HAppend;
import com.github.tonivade.purefun.generic.HList.HCons;
import com.github.tonivade.purefun.generic.HList.HFoldr;
import com.github.tonivade.purefun.generic.HList.HNil;

public class HListTest {

  @Test
  public void consTest() {
    Tuple2<String, Integer> tuple = Tuple.of("Hola", 42);
    HCons<String, HCons<Integer, HNil>> hlist = HList.of("Hola", 42);

    assertAll(
        () -> assertEquals("Hola", hlist.head()),
        () -> assertEquals(42, hlist.tail().head()),
        () -> assertEquals(empty(), hlist.tail().tail()),
        () -> assertEquals(2, hlist.size()),
        () -> assertFalse(hlist.isEmpty()),
        () -> assertEquals(some("Hola"), hlist.find(String.class)),
        () -> assertEquals(none(), hlist.find(Void.class)),
        () -> assertEquals(HList.from(tuple), hlist),
        () -> assertEquals("HCons(Hola,HCons(42,HNil))", hlist.toString())
      );
  }

  @Test
  public void emptyTest() {
    HNil hlist = empty();

    assertAll(
        () -> assertEquals(0, hlist.size()),
        () -> assertTrue(hlist.isEmpty()),
        () -> assertEquals(empty(), hlist),
        () -> assertEquals(none(), hlist.find(String.class)),
        () -> assertEquals("HNil", hlist.toString())
      );
  }

  @Test
  public void appendLists() {
    HCons<Boolean, HNil> alist = HList.of(true);
    HCons<String, HCons<Integer, HNil>> blist = HList.of("Hola", 42);

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
        () -> assertEquals(empty(), append.tail().tail().tail()),
        () -> assertEquals("HCons(true,HCons(Hola,HCons(42,HNil)))", append.toString())
      );
  }

  @Test
  public void foldrListsWithFunctions() {
    HCons<Function1<String, Integer>, HCons<Function1<Integer, Integer>, HCons<Function1<Integer, Integer>, HNil>>> hlist =
        HList.of(String::length, length -> length + 1, sum -> sum * 2);

    HFoldr<Unit,
           Function1<Integer, Integer>,
           HCons<Function1<String, Integer>, HCons<Function1<Integer, Integer>, HCons<Function1<Integer, Integer>, HNil>>>,
           Function1<String, Integer>> foldr = foldr(compose(), foldr(compose(), foldr(compose(), foldr())));

    Function1<String, Integer> apply = foldr.foldr(unit(), identity(), hlist);

    assertEquals(("abcde".length() + 1) * 2, apply.apply("abcde"));
  }

  @Test
  public void foldrListsWithData() {
    HCons<String, HCons<Integer, HNil>> hlist = HList.of("Hola", 42);

    HFoldr<Unit, String, HCons<String, HCons<Integer, HNil>>, String> foldr =
        foldr(combine((a, b) -> a + b), foldr(combine((a, b) -> a + b), foldr()));

    assertEquals("Hola42", foldr.foldr(unit(), "", hlist));
  }
}
