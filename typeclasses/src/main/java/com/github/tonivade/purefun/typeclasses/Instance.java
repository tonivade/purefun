/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Try;

public abstract class Instance<F extends Witness> {
  
  private final Class<? extends Witness> kindType;
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

  public Class<? extends Witness> getKindType() {
    return kindType;
  }

  public Type getType() {
    return type;
  }

  public Functor<F> functor() {
    return load(this, Functor.class);
  }

  public Applicative<F> applicative() {
    return load(this, Applicative.class);
  }

  public Monad<F> monad() {
    return load(this, Monad.class);
  }

  public <R> MonadReader<F, R> monadReader() {
    return load(this, MonadReader.class);
  }

  public <S> MonadState<F, S> monadState() {
    return load(this, MonadState.class);
  }

  public <W> MonadWriter<F, W> monadWriter() {
    return load(this, MonadWriter.class);
  }

  public Comonad<F> comonad() {
    return load(this, Comonad.class);
  }

  public Selective<F> selective() {
    return load(this, Selective.class);
  }

  public <E> ApplicativeError<F, E> applicativeError() {
    return load(this, ApplicativeError.class);
  }

  public <E> MonadError<F, E> monadError() {
    return load(this, MonadError.class);
  }

  public MonadThrow<F> monadThrow() {
    return load(this, MonadThrow.class);
  }

  public MonadDefer<F> monadDefer() {
    return load(this, MonadDefer.class);
  }

  public Async<F> async() {
    return load(this, Async.class);
  }

  public Concurrent<F> concurrent() {
    return load(this, Concurrent.class);
  }

  public Runtime<F> runtime() {
    return load(this, Runtime.class);
  }

  public Foldable<F> foldable() {
    return load(this, Foldable.class);
  }

  public Traverse<F> traverse() {
    return load(this, Traverse.class);
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

  public static <F extends Witness, R> MonadReader<F, R> monadReader(Class<F> type) {
    return new Instance<F>(type) {}.monadReader();
  }

  public static <F extends Witness, S> MonadState<F, S> monadState(Class<F> type) {
    return new Instance<F>(type) {}.monadState();
  }

  public static <F extends Witness, W> MonadWriter<F, W> monadWriter(Class<F> type) {
    return new Instance<F>(type) {}.monadWriter();
  }

  public static <F extends Witness> Comonad<F> comonad(Class<F> type) {
    return new Instance<F>(type) {}.comonad();
  }

  public static <F extends Witness> Selective<F> selective(Class<F> type) {
    return new Instance<F>(type) {}.selective();
  }

  public static <F extends Witness, E> ApplicativeError<F, E> applicativeError(Class<F> type) {
    return new Instance<F>(type) {}.applicativeError();
  }

  public static <F extends Witness, E> MonadError<F, E> monadError(Class<F> type) {
    return new Instance<F>(type) {}.monadError();
  }

  public static <F extends Witness> MonadThrow<F> monadThrow(Class<F> type) {
    return new Instance<F>(type) {}.monadThrow();
  }

  public static <F extends Witness> MonadDefer<F> monadDefer(Class<F> type) {
    return new Instance<F>(type) {}.monadDefer();
  }

  public static <F extends Witness> Async<F> async(Class<F> type) {
    return new Instance<F>(type) {}.async();
  }

  public static <F extends Witness> Concurrent<F> concurrent(Class<F> type) {
    return new Instance<F>(type) {}.concurrent();
  }

  public static <F extends Witness> Runtime<F> runtime(Class<F> type) {
    return new Instance<F>(type) {}.runtime();
  }

  public static <F extends Witness> Foldable<F> foldable(Class<F> type) {
    return new Instance<F>(type) {}.foldable();
  }

  public static <F extends Witness> Traverse<F> traverse(Class<F> type) {
    return new Instance<F>(type) {}.traverse();
  }

  protected String instanceName(Class<?> typeClass) {
    return "com.github.tonivade.purefun.instances." 
        + kindType.getSimpleName().replace("_", "") + typeClass.getSimpleName();
  }
  
  private static Type genericType(Type type) {
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      return parameterizedType.getActualTypeArguments()[0];
    }
    throw new UnsupportedOperationException("not supported " + type.getTypeName());
  }

  @SuppressWarnings("unchecked")
  private static Class<? extends Witness> kindType(Type type) {
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      if (parameterizedType.getActualTypeArguments()[0] instanceof ParameterizedType) {
        return kindType(parameterizedType.getActualTypeArguments()[0]);
      }
      if (parameterizedType.getActualTypeArguments()[0] instanceof Class) {
        return (Class<? extends Witness>) parameterizedType.getActualTypeArguments()[0];
      }
    }
    throw new UnsupportedOperationException("not supported " + type.getTypeName());
  }

  private static <F extends Witness, T> T load(Instance<F> instance, Class<?> typeClass) {
    return Try.of(() -> findClass(instance, typeClass))
      .map(Instance::findField)
      .map(Instance::<T>getInstance)
      .mapError(error -> new InstanceNotFoundException(instance.getType(), typeClass, error))
      .getOrElseThrow();
  }

  private static <F extends Witness> Class<?> findClass(Instance<F> instance, Class<?> typeClass)
      throws ClassNotFoundException {
    return Class.forName(instance.instanceName(typeClass));
  }

  private static Field findField(Class<?> forName) throws NoSuchFieldException {
    return forName.getDeclaredField("INSTANCE");
  }

  @SuppressWarnings("unchecked")
  private static <T> T getInstance(Field declaredField) throws IllegalAccessException {
    declaredField.setAccessible(true);
    return (T) declaredField.get(null);
  }
}
