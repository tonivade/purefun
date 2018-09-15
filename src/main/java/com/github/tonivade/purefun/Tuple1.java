package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.typeclasses.Equal.comparing;
import static java.util.Objects.requireNonNull;

import java.util.Objects;

import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.typeclasses.Equal;

public final class Tuple1<A> implements Tuple {

  private final A value1;

  private Tuple1(A value1) {
    this.value1 = requireNonNull(value1);
  }

  public A get1() {
    return value1;
  }
  
  @Override
  public Sequence<Object> toSequence() {
    return Sequence.listOf(value1);
  }
  
  public <B> Tuple1<B> map(Function1<A, B> mapper) {
    return new Tuple1<>(mapper.apply(value1));
  }

  public static <A> Tuple1<A> of(A value1) {
    return new Tuple1<>(value1);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value1);
  }

  @Override
  public boolean equals(Object obj) {
    return Equal.of(this)
        .append(comparing(Tuple1::get1))
        .applyTo(obj);
  }

  @Override
  public String toString() {
    return "Tuple1(" + value1 + ")";
  }
}
