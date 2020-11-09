/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import java.lang.reflect.Field;

import com.github.tonivade.purefun.Witness;

public final class Instances {

  public static <F extends Witness> Functor<F> functor(Class<F> type) {
    return load(type, "Functor");
  }

  public static <F extends Witness> Applicative<F> applicative(Class<F> type) {
    return load(type, "Applicative");
  }

  public static <F extends Witness> Monad<F> monad(Class<F> type) {
    return load(type, "Monad");
  }

  public static <F extends Witness, E> MonadError<F, E> monadError(Class<F> type) {
    return load(type, "MonadError");
  }

  public static <F extends Witness> MonadThrow<F> monadThrow(Class<F> type) {
    return load(type, "MonadThrow");
  }

  public static <F extends Witness> MonadDefer<F> monadDefer(Class<F> type) {
    return load(type, "MonadDefer");
  }

  public static <F extends Witness> Traverse<F> traverse(Class<F> type) {
    return load(type, "Traverse");
  }

  @SuppressWarnings("unchecked")
  private static <F extends Witness, T> T load(Class<F> type, String typeClass) {
    try {
      Class<?> forName = Class.forName("com.github.tonivade.purefun.instances." + type.getSimpleName().replace("_", "") + typeClass);
      Field declaredField = forName.getDeclaredField("INSTANCE");
      declaredField.setAccessible(true);
      return (T) declaredField.get(null);
    } catch (ClassNotFoundException 
        | IllegalArgumentException 
        | IllegalAccessException 
        | NoSuchFieldException 
        | SecurityException e) {
      throw new InstanceNotFoundException(type, typeClass, e);
    }
  }
}
