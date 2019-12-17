/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

/**
 * This is a utility class to generate more readable {@code equals()} methods. It's based on {@link Eq} instances and it can combine
 * some of them to generate a bigger function that verify the equivalence between two instances of the same type {@code T}.
 *
 * <pre><code>  {@literal @Override}
 * public boolean equals(Object obj) {
 *   return{@literal <Equal.Data>}of()
 *     .comparing(Data::getId)
 *     .comparing(Data::getValue)
 *     .applyTo(this, obj);
 * }</code></pre>
 * @param <T> type to which it applies
 */
public final class Equal<T> {

  private final Eq<T> tester;

  private Equal(Eq<T> tester) {
    this.tester = requireNonNull(tester);
  }

  public Equal<T> append(Eq<T> other) {
    return new Equal<>(tester.and(other));
  }

  public <V> Equal<T> comparing(Function1<T, V> getter) {
    return append(Eq.comparing(getter));
  }

  public <V> Equal<T> comparingArray(Function1<T, V[]> getter) {
    return append(Eq.comparingArray(getter));
  }

  @SuppressWarnings("unchecked")
  public boolean applyTo(T self, Object obj) {
    requireNonNull(self);
    if (isNull(obj)) {
      return false;
    }
    if (sameObjects(self, obj)) {
      return true;
    }
    return sameClasses(self, obj) && areEquals(self, (T) obj);
  }

  private boolean areEquals(T self, T other) {
    return tester.eqv(self, other);
  }

  private boolean sameClasses(T self, Object obj) {
    return self.getClass() == obj.getClass();
  }

  private boolean sameObjects(T self, Object obj) {
    return self == obj;
  }

  public static <T> Equal<T> of() {
    return new Equal<>(Eq.always());
  }
}
