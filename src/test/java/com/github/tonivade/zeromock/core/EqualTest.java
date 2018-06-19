/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Equal.comparing;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.Objects;

import org.junit.Test;

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
    assertThat(data1, equalTo(data2));
  }

  @Test
  public void notEquals() {
    assertThat(data1, not(equalTo(data3)));
  }

  @Test
  public void sameObjects() {
    assertThat(data1, equalTo(data1));
  }

  @Test
  public void differentClasses() {
    assertThat(data1, not(equalTo(new Object())));
  }

  @Test
  public void notEqualsToNull() {
    assertThat(data1, not(equalTo(null)));
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
        .append(comparing(Data::getId))
        .append(comparing(Data::getValue))
        .applyTo(obj);
  }

  @Override
  public String toString() {
    return "Data [id=" + id + ", value=" + value + "]";
  }
}