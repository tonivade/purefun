package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Equal.comparing;
import static com.github.tonivade.zeromock.core.Equal.equal;
import static java.util.Objects.requireNonNull;

import java.util.Objects;

public final class Tuple1<A> {

  private final A value1;

  private Tuple1(A value1) {
    this.value1 = requireNonNull(value1);
  }

  public A get1() {
    return value1;
  }
  
  public <B> Tuple1<B> map(Handler1<A, B> mapper) {
    return new Tuple1<B>(mapper.handle(value1));
  }

  public static <A> Tuple1<A> of(A value1) {
    return new Tuple1<A>(value1);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value1);
  }

  @Override
  public boolean equals(Object obj) {
    return equal(this)
        .append(comparing(Tuple1::get1))
        .applyTo(obj);
  }

  @Override
  public String toString() {
    return "Tuple1(" + value1 + ")";
  }
}
