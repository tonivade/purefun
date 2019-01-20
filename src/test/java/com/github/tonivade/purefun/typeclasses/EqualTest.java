/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.typeclasses.Eq.comparing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Equal;

import nl.jqno.equalsverifier.EqualsVerifier;

public class EqualTest {

  private static final String VALUE = "value";

  private Data data1 = new Data(1, VALUE);
  private Data data2 = new Data(1, VALUE);
  private Data data3 = new Data(2, VALUE);

  @Test
  public void equalsContract() {
    EqualsVerifier.forClass(Data.class).verify();
  }

  @Test
  public void areEquals() {
    assertEquals(data1, data2);
  }

  @Test
  public void notEquals() {
    assertNotEquals(data1, data3);
  }

  @Test
  public void sameObjects() {
    assertEquals(data1, data1);
  }

  @Test
  public void differentClasses() {
    assertNotEquals(data1, new Object());
  }

  @Test
  public void notEqualsToNull() {
    assertNotEquals(data1, null);
  }
}

final class Data {

  private final int id;
  private final String value;

  public Data(int id, String value) {
    this.id = id;
    this.value = value;
  }

  public int getId() {
    return id;
  }

  public String getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, value);
  }

  @Override
  public boolean equals(Object obj) {
    return Equal.of(this)
        .append(comparing(Data::getId).and(comparing(Data::getValue)))
        .applyTo(obj);
  }

  @Override
  public String toString() {
    return "Data [id=" + id + ", value=" + value + "]";
  }
}