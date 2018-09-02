/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Higher;

public class IOKind<T> implements IO<T>, Higher<IOKind.µ, T>{

  public static final class µ {}

  private final IO<T> deletage;

  public IOKind(IO<T> delegate) {
    this.deletage = requireNonNull(delegate);
  }

  @Override
  public T unsafeRunSync() {
    return deletage.unsafeRunSync();
  }

  public static <T> IO<T> narrowK(Higher<IOKind.µ, T> hkt) {
    return (IO<T>) hkt;
  }
}
