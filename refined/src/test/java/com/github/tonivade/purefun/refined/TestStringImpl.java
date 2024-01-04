/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.refined;

import java.io.Serial;
import java.util.Arrays;

import javax.annotation.processing.Generated;

import com.github.tonivade.purefun.Precondition;

@Generated("xxx")
public final class TestStringImpl implements TestString {
  
  @Serial
  private static final long serialVersionUID = 3297705339043486610L;

  private final byte[] value;

  public TestStringImpl(String value) {
    this.value = Precondition.checkNonEmpty(value).getBytes();
  }

  @Override
  public int length() {
    return toString().length();
  }

  @Override
  public char charAt(int index) {
    return toString().charAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return toString().subSequence(start, end);
  }

  @Override
  public int compareTo(TestString other) {
    return this.toString().compareTo(other.toString());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(value);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TestStringImpl other = (TestStringImpl) obj;
    return Arrays.equals(value, other.value);
  }
  
  @Override
  public String toString() {
    return new String(value);
  }
}
