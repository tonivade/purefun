/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.data.Sequence.zip;
import static java.lang.Character.toLowerCase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

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

  public Functor<F> functor(Object...params) {
    return load(this, Functor.class, params);
  }

  public Bifunctor<F> bifunctor(Object...params) {
    return load(this, Bifunctor.class, params);
  }

  public Applicative<F> applicative(Object...params) {
    return load(this, Applicative.class, params);
  }

  public Monad<F> monad(Object...params) {
    return load(this, Monad.class, params);
  }

  public <R> MonadReader<F, R> monadReader(Object...params) {
    return load(this, MonadReader.class, params);
  }

  public <S> MonadState<F, S> monadState(Object...params) {
    return load(this, MonadState.class, params);
  }

  public <W> MonadWriter<F, W> monadWriter(Object...params) {
    return load(this, MonadWriter.class, params);
  }

  public Comonad<F> comonad(Object...params) {
    return load(this, Comonad.class, params);
  }

  public Selective<F> selective(Object...params) {
    return load(this, Selective.class, params);
  }

  public <E> ApplicativeError<F, E> applicativeError(Object...params) {
    return load(this, ApplicativeError.class, params);
  }

  public <E> MonadError<F, E> monadError(Object...params) {
    return load(this, MonadError.class, params);
  }

  public MonadThrow<F> monadThrow(Object...params) {
    return load(this, MonadThrow.class, params);
  }

  public MonadDefer<F> monadDefer(Object...params) {
    return load(this, MonadDefer.class, params);
  }

  public Async<F> async(Object...params) {
    return load(this, Async.class, params);
  }

  public Concurrent<F> concurrent(Object...params) {
    return load(this, Concurrent.class, params);
  }

  public Runtime<F> runtime(Object...params) {
    return load(this, Runtime.class, params);
  }

  public Console<F> console(Object...params) {
    return load(this, Console.class, params);
  }

  public Foldable<F> foldable(Object...params) {
    return load(this, Foldable.class, params);
  }

  public Traverse<F> traverse(Object...params) {
    return load(this, Traverse.class, params);
  }

  public static <F extends Witness> Functor<F> functor(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.functor(params);
  }

  public static <F extends Witness> Bifunctor<F> bifunctor(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.bifunctor(params);
  }

  public static <F extends Witness> Applicative<F> applicative(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.applicative(params);
  }

  public static <F extends Witness> Monad<F> monad(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.monad(params);
  }

  public static <F extends Witness, R> MonadReader<F, R> monadReader(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.monadReader(params);
  }

  public static <F extends Witness, S> MonadState<F, S> monadState(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.monadState(params);
  }

  public static <F extends Witness, W> MonadWriter<F, W> monadWriter(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.monadWriter(params);
  }

  public static <F extends Witness> Comonad<F> comonad(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.comonad(params);
  }

  public static <F extends Witness> Selective<F> selective(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.selective(params);
  }

  public static <F extends Witness, E> ApplicativeError<F, E> applicativeError(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.applicativeError(params);
  }

  public static <F extends Witness, E> MonadError<F, E> monadError(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.monadError(params);
  }

  public static <F extends Witness> MonadThrow<F> monadThrow(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.monadThrow(params);
  }

  public static <F extends Witness> MonadDefer<F> monadDefer(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.monadDefer(params);
  }

  public static <F extends Witness> Async<F> async(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.async(params);
  }

  public static <F extends Witness> Concurrent<F> concurrent(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.concurrent(params);
  }

  public static <F extends Witness> Runtime<F> runtime(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.runtime(params);
  }

  public static <F extends Witness> Console<F> console(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.console(params);
  }

  public static <F extends Witness> Foldable<F> foldable(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.foldable(params);
  }

  public static <F extends Witness> Traverse<F> traverse(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.traverse(params);
  }

  protected String instanceName() {
    return "com.github.tonivade.purefun.instances." + kindType.getSimpleName().replace("_", "Instances");
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

  private static <F extends Witness, T> T load(Instance<F> instance, Class<?> typeClass, Object... args) {
    return Try.of(() -> findClass(instance))
      .map(clazz -> findMethod(clazz, typeClass, args))
      .map(method -> Instance.<T>getInstance(method, args))
      .mapError(error -> new InstanceNotFoundException(instance.getType(), typeClass, error))
      .getOrElseThrow();
  }

  private static <F extends Witness> Class<?> findClass(Instance<F> instance)
      throws ClassNotFoundException {
    return Class.forName(instance.instanceName());
  }

  private static Method findMethod(Class<?> instanceClass, Class<?> typeClass, Object... args) 
      throws NoSuchMethodException {
    String simpleName = typeClass.getSimpleName();
    String methodName = toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    return Arrays.stream(instanceClass.getDeclaredMethods())
        .filter(m -> m.getName().equals(methodName))
        .filter(m -> m.getParameterCount() == args.length)
        .filter(m -> zip(listOf(m.getParameterTypes()), listOf(args)).allMatch(tuple -> tuple.applyTo(Class::isInstance)))
        .findFirst()
        .orElseThrow(NoSuchMethodException::new);
  }

  @SuppressWarnings("unchecked")
  private static <T> T getInstance(Method method, Object...args) 
      throws IllegalAccessException, InvocationTargetException {
    return (T) method.invoke(null, args);
  }
}
