package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Function1.identity;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Holder;
import com.github.tonivade.purefun.Monad;

public final class Id<T> implements Monad<IdKind.µ, T>, Holder<T> {

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
  public <R> Id<R> flatMap(Function1<T, ? extends Higher<IdKind.µ, R>> map) {
    return map.andThen(IdKind::narrowK).apply(value);
  }
}
