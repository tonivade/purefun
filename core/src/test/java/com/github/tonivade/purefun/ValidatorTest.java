/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.github.tonivade.purefun.type.Validation;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.type.Validation.Result;

import java.util.Objects;

import static com.github.tonivade.purefun.Validator.join;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValidatorTest {

  @Test
  public void range() {
    Validator<String, Integer> validator = Validator.range(10, 20);

    assertAll(
        () -> assertEquals(Validation.invalid("require non null"), validator.validate(null)),
        () -> assertEquals(Validation.invalid("require greater than or equal to: 10"), validator.validate(0)),
        () -> assertEquals(Validation.invalid("require lower than: 20"), validator.validate(100)),
        () -> assertEquals(Validation.valid(10), validator.validate(10)),
        () -> assertEquals(Validation.valid(15), validator.validate(15)),
        () -> assertEquals(Validation.invalid("require lower than: 20"), validator.validate(20))
    );
  }

  @Test
  public void length() {
    Validator<String, String> validator = Validator.length(1, 3);

    assertAll(
      () -> assertEquals(Validation.invalid("require non null"), validator.validate(null)),
      () -> assertEquals(Validation.invalid("require min length: 1"), validator.validate("")),
      () -> assertEquals(Validation.invalid("require max length: 3"), validator.validate("abcde")),
      () -> assertEquals(Validation.valid("a"), validator.validate("a")),
      () -> assertEquals(Validation.valid("ab"), validator.validate("ab")),
      () -> assertEquals(Validation.invalid("require max length: 3"), validator.validate("abc"))
    );
  }

  @Test
  public void positive() {
    Validator<String, Integer> positive = Validator.positive();

    assertAll(
        () -> assertEquals(Validation.valid(1), positive.validate(1)),
        () -> assertEquals(Validation.invalid("require greater than: 0"), positive.validate(0)),
        () -> assertEquals(Validation.invalid("require greater than: 0"), positive.validate(-1))
    );
  }

  @Test
  public void negative() {
    Validator<String, Integer> negative = Validator.negative();

    assertAll(
        () -> assertEquals(Validation.valid(-1), negative.validate(-1)),
        () -> assertEquals(Validation.invalid("require lower than: 0"), negative.validate(0))
    );
  }

  @Test
  public void nonEmpty() {
    Validator<String, String> nonEmpty = Validator.nonEmpty();

    assertAll(
        () -> assertEquals(Validation.valid("1"), nonEmpty.validate("1")),
        () -> assertEquals(Validation.invalid("require non empty string"), nonEmpty.validate(""))
    );
  }

  @Test
  public void uppercase() {
    Validator<String, String> uppercase = Validator.upper();

    assertAll(
        () -> assertEquals(Validation.valid("A"), uppercase.validate("A")),
        () -> assertEquals(Validation.invalid("require uppercase string"), uppercase.validate("a"))
    );
  }

  @Test
  public void lowercase() {
    Validator<String, String> lowercase = Validator.lower();

    assertAll(
        () -> assertEquals(Validation.valid("a"), lowercase.validate("a")),
        () -> assertEquals(Validation.invalid("require lowercase string"), lowercase.validate("A"))
    );
  }

  @Test
  public void combine() {
    Validator<String, String> validator =
        Validator.nonNullAnd(Validator.combine(Validator.nonEmpty(), Validator.match("[a-z]+"), join()));

    assertAll(
        () -> assertEquals(Validation.valid("abc"), validator.validate("abc")),
        () -> assertEquals(Validation.invalid("require non null"), validator.validate(null)),
        () -> assertEquals(Validation.invalid(
            "require non empty string,should match expresion: [a-z]+"), validator.validate("")),
        () -> assertEquals(Validation.invalid("should match expresion: [a-z]+"), validator.validate("123"))
    );
  }

  @Test
  public void combine3() {
    Validator<String, String> validator =
        Validator.nonNullAnd(
            Validator.combine(
                Validator.nonEmpty(),
                Validator.startsWith("a"),
                Validator.endsWith("z"),
                join()));

    assertAll(
        () -> assertEquals(Validation.valid("abz"), validator.validate("abz")),
        () -> assertEquals(Validation.invalid("require non null"), validator.validate(null)),
        () -> assertEquals(Validation.invalid(
            "require non empty string,require start with: a,require end with: z"),
            validator.validate("")),
        () -> assertEquals(Validation.invalid("require start with: a,require end with: z"),
            validator.validate("b")),
        () -> assertEquals(Validation.invalid("require end with: z"), validator.validate("ab"))
    );
  }

  @Test
  public void combine4() {
    Validator<String, String> validator =
        Validator.nonNullAnd(
            Validator.combine(
                Validator.nonEmpty(),
                Validator.startsWith("a"),
                Validator.contains("b"),
                Validator.endsWith("z"),
                join()));

    assertAll(
        () -> assertEquals(Validation.valid("abz"), validator.validate("abz")),
        () -> assertEquals(Validation.invalid("require non null"), validator.validate(null)),
        () -> assertEquals(Validation.invalid(
            "require non empty string,require start with: a,require contain string: b,require end with: z"),
            validator.validate("")),
        () -> assertEquals(Validation.invalid(
            "require start with: a,require contain string: b,require end with: z"),
            validator.validate("c")),
        () -> assertEquals(Validation.invalid(
            "require contain string: b,require end with: z"),
            validator.validate("ac")),
        () -> assertEquals(Validation.invalid("require end with: z"), validator.validate("ab"))
    );
  }

  @Test
  public void combine5() {
    Validator<Result<String>, String> validator =
            Validator.combine(
                Validator.nonEmpty(),
                Validator.startsWith("a"),
                Validator.contains("b"),
                Validator.endsWith("z"),
                Validator.lower());

    assertAll(
        () -> assertEquals(Validation.valid("abz"), validator.validate("abz")),
        () -> assertEquals(Validation.invalidOf(
            "require non empty string",
            "require start with: a",
            "require contain string: b",
            "require end with: z"), validator.validate("")),
        () -> assertEquals(Validation.invalidOf(
            "require start with: a",
            "require contain string: b",
            "require end with: z"), validator.validate("c")),
        () -> assertEquals(Validation.invalidOf(
            "require contain string: b",
            "require end with: z"), validator.validate("ac")),
        () -> assertEquals(Validation.invalidOf("require end with: z"),
            validator.validate("ab")),
        () -> assertEquals(Validation.invalidOf("require lowercase string"),
            validator.validate("abCz"))
    );
  }

  @Test
  public void product2() {
    Validator<String, Integer> ageValidator = Validator.positive(() -> "age must be positive");
    Validator<String, String> nameValidator = Validator.nonEmpty(() -> "name must be non empty");
    Validator<Result<String>, Tuple2<Integer, String>> validator =
        Validator.product(ageValidator, nameValidator);

    assertAll(
        () -> assertEquals(Validation.valid(new Person(10, "some name")),
            validator.validate(Tuple.of(10, "some name")).map(Tuple.applyTo(Person::new))),
        () -> assertEquals(Validation.invalidOf("age must be positive", "name must be non empty"),
            validator.validate(Tuple.of(-1, "")))
    );
  }

  @Test
  public void product3() {
    Validator<String, Integer> v1 = Validator.positive();
    Validator<String, String> v2 = Validator.nonEmpty();
    Validator<String, String> v3 = Validator.match("[a-z]+");
    Validator<Result<String>, Tuple3<Integer, String, String>> validator =
        Validator.product(v1, v2, v3);

    Tuple3<Integer, String, String> valid = Tuple.of(10, "some name", "asdfg");
    assertAll(
        () -> assertEquals(Validation.valid(valid), validator.validate(valid)),
        () -> assertEquals(Validation.invalidOf(
                "require greater than: 0",
                "require non empty string",
                "should match expresion: [a-z]+"),
            validator.validate(Tuple.of(-1, "", "")))
    );
  }

  @Test
  public void product4() {
    Validator<String, Integer> v1 = Validator.positive();
    Validator<String, String> v2 = Validator.nonEmpty();
    Validator<String, String> v3 = Validator.match("[a-z]+");
    Validator<String, Integer> v4 = Validator.negative();
    Validator<Result<String>, Tuple4<Integer, String, String, Integer>> validator =
        Validator.product(v1, v2, v3, v4);

    Tuple4<Integer, String, String, Integer> valid = Tuple.of(10, "some name", "asdfg", -1);
    assertAll(
        () -> assertEquals(Validation.valid(valid), validator.validate(valid)),
        () -> assertEquals(Validation.invalidOf(
                "require greater than: 0",
                "require non empty string",
                "should match expresion: [a-z]+",
                "require lower than: 0"),
            validator.validate(Tuple.of(-1, "", "", 1)))
    );
  }

  @Test
  public void product5() {
    Validator<String, Integer> v1 = Validator.positive();
    Validator<String, String> v2 = Validator.nonEmpty();
    Validator<String, String> v3 = Validator.match("[a-z]+");
    Validator<String, Integer> v4 = Validator.negative();
    Validator<String, String> v5 = Validator.combine(Validator.startsWith("a"), Validator.endsWith("z"), join(" and "));
    Validator<Result<String>, Tuple5<Integer, String, String, Integer, String>> validator =
        Validator.product(v1, v2, v3, v4, v5);

    Tuple5<Integer, String, String, Integer, String> valid = Tuple.of(10, "some name", "asdfg", -1, "a jksdfd z");
    assertAll(
        () -> assertEquals(Validation.valid(valid), validator.validate(valid)),
        () -> assertEquals(Validation.invalidOf(
                "require greater than: 0",
                "require non empty string",
                "should match expresion: [a-z]+",
                "require lower than: 0",
                "require start with: a and require end with: z"),
            validator.validate(Tuple.of(-1, "", "", 1, "x")))
    );
  }
}

final class Person {

  private final Integer age;
  private final String name;

  Person(Integer age, String name) {
    this.age = requireNonNull(age);
    this.name = requireNonNull(name);
  }

  public Integer getAge() {
    return age;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object obj) {
    return Equal.<Person>of()
        .comparing(Person::getAge)
        .comparing(Person::getName)
        .applyTo(this, obj);
  }

  @Override
  public int hashCode() {
    return Objects.hash(age, name);
  }

  @Override
  public String toString() {
    return "Person(" +
        "age=" + age +
        ", name='" + name + '\'' +
        ')';
  }
}