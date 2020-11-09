/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.github.tonivade.purefun.Witness;

public abstract class Instance<F extends Witness> {
  
  private final Class<?> kindType;
  private final Type type;
  
  protected Instance(Class<F> clazz) {
    this.kindType = clazz;
    this.type = clazz;
  }

  protected Instance() {
    Type genericSuperType = getClass().getGenericSuperclass();
    this.type = genericType(genericSuperType);
    this.kindType = kindType(type);
  }

  public Functor<F> functor() {
    return load(this, "Functor");
  }

  public Applicative<F> applicative() {
    return load(this, "Applicative");
  }

  public Monad<F> monad() {
    return load(this, "Monad");
  }

  public <E> MonadError<F, E> monadError() {
    return load(this, "MonadError");
  }

  public MonadThrow<F> monadThrow() {
    return load(this, "MonadThrow");
  }

  public MonadDefer<F> monadDefer() {
    return load(this, "MonadDefer");
  }

  public Traverse<F> traverse() {
    return load(this, "Traverse");
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
      if (parameterizedType.getActualTypeArguments()[0] instanceof ParameterizedType) {
        return kindType(parameterizedType.getActualTypeArguments()[0]);
      }
      if (parameterizedType.getActualTypeArguments()[0] instanceof Class) {
        return (Class<?>) parameterizedType.getActualTypeArguments()[0];
      }
    }
    throw new UnsupportedOperationException(type.getTypeName());
  }

  public Class<?> getKindType() {
    return kindType;
  }

  public Type getType() {
    return type;
  }

  public static <F extends Witness> Functor<F> functor(Class<F> type) {
    return new Instance<F>(type) {}.functor();
  }

  public static <F extends Witness> Applicative<F> applicative(Class<F> type) {
    return new Instance<F>(type) {}.applicative();
  }

  public static <F extends Witness> Monad<F> monad(Class<F> type) {
    return new Instance<F>(type) {}.monad();
  }

  public static <F extends Witness, E> MonadError<F, E> monadError(Class<F> type) {
    return new Instance<F>(type) {}.monadError();
  }

  public static <F extends Witness> MonadThrow<F> monadThrow(Class<F> type) {
    return new Instance<F>(type) {}.monadThrow();
  }

  public static <F extends Witness> MonadThrow<F> monadDefer(Class<F> type) {
    return new Instance<F>(type) {}.monadDefer();
  }

  public static <F extends Witness> Traverse<F> traverse(Class<F> type) {
    return new Instance<F>(type) {}.traverse();
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
