package com.github.tonivade.purefun.refined;

import java.io.Serial;
import java.util.Objects;

import javax.annotation.processing.Generated;

import com.github.tonivade.purefun.Precondition;

@Generated("xxx")
public final class TestIntImpl extends Number implements TestInt {
  
  @Serial
  private static final long serialVersionUID = 2426275725152618668L;
  
  private final int value;

  public TestIntImpl(int value) {
    this.value = Precondition.checkPositive(value);
  }

  @Override
  public int intValue() {
    return value;
  }

  @Override
  public long longValue() {
    return value;
  }

  @Override
  public float floatValue() {
    return value;
  }

  @Override
  public double doubleValue() {
    return value;
  }

  @Override
  public int compareTo(TestIntImpl other) {
    return this.value - other.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TestIntImpl other = (TestIntImpl) obj;
    return value == other.value;
  }
  
  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
