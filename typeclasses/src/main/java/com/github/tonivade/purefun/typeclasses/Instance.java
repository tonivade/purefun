/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.tools.Diagnostic.Kind;

import com.github.tonivade.purefun.Witness;

public abstract class Instance<T> {
  
  private final Class<?> kindType;
  private final Type type;
  
  protected Instance(Class<T> clazz) {
    this.kindType = clazz;
    this.type = clazz;
  }

  protected Instance() {
    Type genericSuperType = getClass().getGenericSuperclass();
    this.type = genericType(genericSuperType);
    this.kindType = kindType(type);
  }
  
  private static Type genericType(Type type) {
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      return parameterizedType.getActualTypeArguments()[0];
    }
    throw new UnsupportedOperationException();
  }

  private static Class<?> kindType(Type type) {
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      if (parameterizedType.getActualTypeArguments()[0].equals(Kind.class)) {
        return kindType(parameterizedType.getActualTypeArguments()[0]);
      }
      return (Class<?>) parameterizedType.getActualTypeArguments()[0];
    }
    throw new UnsupportedOperationException();
  }

  public Class<?> getKindType() {
    return kindType;
  }

  public Type getType() {
    return type;
  }

  public static <F extends Witness> Functor<F> functor(Class<F> type) {
    return functor(new Instance<F>(type) {});
  }

  public static <F extends Witness> Functor<F> functor(Instance<F> instance) {
    return load(instance, "Functor");
  }

  public static <F extends Witness> Applicative<F> applicative(Class<F> type) {
    return applicative(new Instance<F>(type) {});
  }

  public static <F extends Witness> Applicative<F> applicative(Instance<F> instance) {
    return load(instance, "Applicative");
  }

  public static <F extends Witness> Monad<F> monad(Class<F> type) {
    return monad(new Instance<F>(type) {});
  }

  public static <F extends Witness> Monad<F> monad(Instance<F> instance) {
    return load(instance, "Monad");
  }

  public static <F extends Witness, E> MonadError<F, E> monadError(Class<F> type) {
    return monadError(new Instance<F>(type) {});
  }

  public static <F extends Witness, E> MonadError<F, E> monadError(Instance<F> instance) {
    return load(instance, "MonadError");
  }

  public static <F extends Witness> MonadThrow<F> monadThrow(Class<F> type) {
    return monadThrow(new Instance<F>(type) {});
  }

  public static <F extends Witness> MonadThrow<F> monadThrow(Instance<F> instance) {
    return load(instance, "MonadThrow");
  }

  public static <F extends Witness> MonadThrow<F> monadDefer(Class<F> type) {
    return monadDefer(new Instance<F>(type) {});
  }

  public static <F extends Witness> MonadDefer<F> monadDefer(Instance<F> instance) {
    return load(instance, "MonadDefer");
  }

  public static <F extends Witness> Traverse<F> traverse(Class<F> type) {
    return traverse(new Instance<F>(type) {});
  }

  public static <F extends Witness> Traverse<F> traverse(Instance<F> instance) {
    return load(instance, "Traverse");
  }

  @SuppressWarnings("unchecked")
  private static <F extends Witness, T> T load(Instance<F> type, String typeClass) {
    try {
      Class<?> forName = Class.forName("com.github.tonivade.purefun.instances." + type.getKindType().getSimpleName().replace("_", "") + typeClass);
      Field declaredField = forName.getDeclaredField("INSTANCE");
      declaredField.setAccessible(true);
      return (T) declaredField.get(null);
    } catch (ClassNotFoundException 
        | IllegalArgumentException 
        | IllegalAccessException 
        | NoSuchFieldException 
        | SecurityException e) {
      throw new InstanceNotFoundException(type.getType(), typeClass, e);
    }
  }
}
