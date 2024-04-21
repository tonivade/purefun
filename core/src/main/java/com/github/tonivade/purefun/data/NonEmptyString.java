/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Operator1;

import static com.github.tonivade.purefun.type.Validation.requireNonEmpty;

public class NonEmptyString implements Serializable {

  @Serial
  private static final long serialVersionUID = -4000125976618351707L;

  private static final Equal<NonEmptyString> EQUAL = Equal.<NonEmptyString>of().comparing(x -> x.value);

  private final String value;

  private NonEmptyString(String value) {
    this.value = value;
  }

  public NonEmptyString map(Operator1<String> mapper) {
    return NonEmptyString.of(mapper.apply(value));
  }

  public <T> T transform(Function1<String, T> mapper) {
    return mapper.apply(value);
  }

  public String get() {
    return value;
  }

  @Override
  public boolean equals(Object obj) {
    return EQUAL.applyTo(this, obj);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return String.format("NonEmptyString(%s)", value);
  }

  public static NonEmptyString of(String value) {
    return requireNonEmpty(value).map(NonEmptyString::new).getOrElseThrow();
  }
}
