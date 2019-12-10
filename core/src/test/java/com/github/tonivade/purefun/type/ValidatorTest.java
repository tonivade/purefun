/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.data.Sequence;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.type.Validator.combine;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValidatorTest {

  @Test
  public void range() {
    Validator<String, Integer> validator = Validator.range(10, 20);

    assertAll(
        () -> assertEquals(Validation.invalid("require non null"), validator.validate(null)),
        () -> assertEquals(Validation.invalid("require min value: 10"), validator.validate(0)),
        () -> assertEquals(Validation.invalid("require max value: 20"), validator.validate(100)),
        () -> assertEquals(Validation.valid(10), validator.validate(10)),
        () -> assertEquals(Validation.valid(15), validator.validate(15)),
        () -> assertEquals(Validation.invalid("require max value: 20"), validator.validate(20))
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
        () -> assertEquals(Validation.valid(0), positive.validate(0)),
        () -> assertEquals(Validation.invalid("require min value: 0"), positive.validate(-1))
    );
  }

  @Test
  public void negative() {
    Validator<String, Integer> negative = Validator.negative();

    assertAll(
        () -> assertEquals(Validation.valid(-1), negative.validate(-1)),
        () -> assertEquals(Validation.invalid("require max value: 0"), negative.validate(0))
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
  public void test() {
    Validator<String, String> lowercase =
        Validator.<String>nonNull()
            .andThen(combine(Validator.nonEmpty(), Validator.match("[a-z]+"), seq -> seq.join(",")));

    assertAll(
      () -> assertEquals(Validation.valid("abc"), lowercase.validate("abc")),
      () -> assertEquals(Validation.invalid("require non null"), lowercase.validate(null)),
      () -> assertEquals(Validation.invalid("require non empty string,should match expresion: [a-z]+"), lowercase.validate("")),
      () -> assertEquals(Validation.invalid("should match expresion: [a-z]+"), lowercase.validate("123"))
    );
  }

  @Test
  public void pojo() {
    Validator<String, Integer> ageValidator = Validator.positive(() -> "age must be positive");
    Validator<String, String> nameValidator = Validator.nonEmpty(() -> "name must be non empty");
    Validator<Sequence<String>, Tuple2<Integer, String>> validator =
        Validator.product(ageValidator, nameValidator, identity());

    assertAll(
        () -> assertEquals(Validation.valid(new Person(10, "some name")),
            validator.validate(Tuple.of(10, "some name")).map(Tuple.applyTo(Person::new))),
        () -> assertEquals(Validation.invalid(listOf("age must be positive", "name must be non empty")),
            validator.validate(Tuple.of(-1, "")))
    );
  }
}

final class Person {

  private final Integer age;
  private final String name;

  Person(Integer age, String name) {
    this.age = age;
    this.name = name;
  }

  public Integer getAge() {
    return age;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object obj) {
    return Equal.of(this)
        .comparing(Person::getAge)
        .comparing(Person::getName)
        .applyTo(obj);
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