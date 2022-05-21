/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import com.github.tonivade.purefun.Operator2;
import com.github.tonivade.purefun.Operator3;
import com.github.tonivade.purefun.Operator4;
import com.github.tonivade.purefun.Operator5;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.type.Option.none;
import static com.github.tonivade.purefun.type.Option.some;
import static com.github.tonivade.purefun.type.Validation.invalid;
import static com.github.tonivade.purefun.type.Validation.invalidOf;
import static com.github.tonivade.purefun.type.Validation.mapN;
import static com.github.tonivade.purefun.type.Validation.requireNonEmpty;
import static com.github.tonivade.purefun.type.Validation.requireNonNull;
import static com.github.tonivade.purefun.type.Validation.valid;
import static java.lang.String.valueOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidationTest {

  private final Operator2<Integer> sum2 = Integer::sum;
  private final Operator3<Integer> sum3 = (a, b, c) -> a + b + c;
  private final Operator4<Integer> sum4 = (a, b, c, d) -> a + b + c + d;
  private final Operator5<Integer> sum5 = (a, b, c, d, e) -> a + b + c + d + e;

  @Test
  public void validTest() {
    Validation<String, Integer> valid =  valid(1);

    assertAll(
        () -> assertTrue(valid.isValid()),
        () -> assertFalse(valid.isInvalid()),
        () -> assertEquals(Integer.valueOf(1), valid.get()),
        () -> assertThrows(NoSuchElementException.class, valid::getError),
        () -> assertEquals(valid(3), valid.map(i -> i + 2)),
        () -> assertEquals(valid(1), valid.mapError(String::toUpperCase)),
        () -> assertEquals(valid("1"), valid.flatMap(i -> valid(valueOf(i)))),
        () -> assertEquals("1", valid.fold(identity(), String::valueOf)),
        () -> assertEquals(some(valid(1)), valid.filter(i -> i > 0)),
        () -> assertEquals(none(), valid.filter(i -> i > 1)),
        () -> assertEquals(valid(1), valid.filterOrElse(i -> i > 0, () -> valid(10))),
        () -> assertEquals(valid(10), valid.filterOrElse(i -> i > 1, () -> valid(10))),
        () -> assertEquals(Integer.valueOf(1), valid.getOrElse(10)),
        () -> assertEquals(Integer.valueOf(1), valid.getOrElseNull()),
        () -> assertEquals(Either.right(1), valid.toEither()),
        () -> assertEquals("Valid(1)", valid.toString())
    );
  }

  @Test
  public void invalidTest() {
    Validation<String, Integer> invalid =  invalid("error");

    assertAll(
        () -> assertFalse(invalid.isValid()),
        () -> assertTrue(invalid.isInvalid()),
        () -> assertEquals("error", invalid.getError()),
        () -> assertThrows(NoSuchElementException.class, invalid::get),
        () -> assertEquals(invalid("error"), invalid.map(i -> i + 2)),
        () -> assertEquals(invalid("ERROR"), invalid.mapError(String::toUpperCase)),
        () -> assertEquals(invalid("error"), invalid.flatMap(i -> valid(valueOf(i)))),
        () -> assertEquals("error", invalid.fold(identity(), String::valueOf)),
        () -> assertEquals(some(invalid("error")), invalid.filter(i -> i > 0)),
        () -> assertEquals(some(invalid("error")), invalid.filter(i -> i > 1)),
        () -> assertEquals(invalid("error"), invalid.filterOrElse(i -> i > 1, () -> valid(10))),
        () -> assertEquals(Integer.valueOf(10), invalid.getOrElse(10)),
        () -> assertNull(invalid.getOrElseNull()),
        () -> assertEquals(Either.left("error"), invalid.toEither()),
        () -> assertEquals("Invalid(error)", invalid.toString())
    );
  }

  @Test
  public void map2Test() {
    assertAll(
        () -> assertEquals(valid(3), mapN(valid(1), valid(2), sum2)),
        () -> assertEquals(invalidOf("error"), mapN(valid(1), invalid("error"), sum2)),
        () -> assertEquals(invalidOf("error"), mapN(invalid("error"), valid(1), sum2)),
        () -> assertEquals(invalidOf("error1", "error2"), mapN(invalid("error1"), invalid("error2"), sum2))
    );
  }

  @Test
  public void map3Test() {
    assertAll(
        () -> assertEquals(valid(6), mapN(valid(1), valid(2), valid(3), sum3)),
        () -> assertEquals(invalidOf("error1", "error2", "error3"),
            mapN(invalid("error1"), invalid("error2"), invalid("error3"), sum3))
    );
  }

  @Test
  public void map4Test() {
    assertAll(
        () -> assertEquals(valid(10), mapN(valid(1), valid(2), valid(3), valid(4), sum4)),
        () -> assertEquals(invalidOf("error1", "error2", "error3", "error4"),
            mapN(invalid("error1"), invalid("error2"), invalid("error3"), invalid("error4"), sum4))
    );
  }

  @Test
  public void map5Test() {
    assertAll(
        () -> assertEquals(valid(15), mapN(valid(1), valid(2), valid(3), valid(4), valid(5), sum5)),
        () -> assertEquals(invalidOf("error1", "error2", "error3", "error4", "error5"),
            mapN(invalid("error1"), invalid("error2"), invalid("error3"), invalid("error4"), invalid("error5"), sum5))
    );
  }

  @Test
  public void getOrElseThrow() {
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> invalid("error").getOrElseThrow()),
        () -> assertEquals("valid", valid("valid").getOrElseThrow())
    );
  }

  @Test
  public void require() {
    assertAll(
        () -> assertEquals(invalid("require non null"), requireNonNull(null)),
        () -> assertEquals(invalid("require non null"), requireNonEmpty(null)),
        () -> assertEquals(invalid("require non empty string"), requireNonEmpty("")),
        () -> assertEquals(valid("a"), requireNonEmpty("a")),
        () -> assertEquals(invalid("require non null"), Validation.requirePositive(null)),
        () -> assertEquals(invalid("require greater than: 0"), Validation.requirePositive(-1)),
        () -> assertEquals(invalid("require greater than: 0"), Validation.requirePositive(0)), // zero is positive or negative?
        () -> assertEquals(valid(10), Validation.requireGreaterThan(10, 1)),
        () -> assertEquals(invalid("require 1 > 10"), Validation.requireGreaterThan(1, 10)),
        () -> assertEquals(invalid("require 1 > 1"), Validation.requireGreaterThan(1, 1)),
        () -> assertEquals(invalid("require 10 < 1"), Validation.requireLowerThan(10, 1)),
        () -> assertEquals(invalid("require 1 < 1"), Validation.requireLowerThan(1, 1)),
        () -> assertEquals(valid(1), Validation.requireLowerThan(1, 10))
    );
  }
}
