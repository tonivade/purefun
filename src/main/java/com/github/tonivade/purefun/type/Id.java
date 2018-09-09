package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Function1.identity;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Holder;
import com.github.tonivade.purefun.Monad;
import com.github.tonivade.purefun.Witness;

public final class Id<T> implements Monad<Id.µ, T>, Holder<T> {

  public static final class µ implements Witness {}

  private final T value;

  private Id(T value) {
    this.value = value;
  }

  public static <T> Id<T> of(T value) {
    return new Id<>(value);
  }

  @Override
  public T get() {
    return value;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> Id<V> flatten() {
    try {
      return ((Id<Id<V>>) this).flatMap(identity());
    } catch (ClassCastException e) {
      throw new UnsupportedOperationException("cannot be flattened");
    }
  }

  @Override
  public <R> Id<R> map(Function1<T, R> map) {
    return Id.of(map.apply(value));
  }

  @Override
  public <R> Id<R> flatMap(Function1<T, ? extends Higher<Id.µ, R>> map) {
    return map.andThen(Id::narrowK).apply(value);
  }

  public static <T> Id<T> narrowK(Higher<Id.µ, T> hkt) {
    return (Id<T>) hkt;
  }
}
