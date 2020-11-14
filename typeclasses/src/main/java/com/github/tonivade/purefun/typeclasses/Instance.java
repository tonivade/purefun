/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
import java.util.concurrent.Executor;

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

  public Bifunctor<F> bifunctor() {
    return load(this, Bifunctor.class);
  }

  public Applicative<F> applicative() {
    return load(this, Applicative.class);
  }

  public <E> Applicative<F> applicative(Semigroup<E> semigroup) {
    return load(this, Applicative.class, semigroup);
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

  public Async<F> async(Executor executor) {
    return load(this, Async.class, executor);
  }

  public Concurrent<F> concurrent() {
    return load(this, Concurrent.class);
  }

  public Runtime<F> runtime() {
    return load(this, Runtime.class);
  }

  public Console<F> console() {
    return load(this, Console.class);
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

  public static <F extends Witness> Bifunctor<F> bifunctor(Class<F> type) {
    return new Instance<F>(type) {}.bifunctor();
  }

  public static <F extends Witness> Applicative<F> applicative(Class<F> type) {
    return new Instance<F>(type) {}.applicative();
  }

  public static <F extends Witness, E> Applicative<F> applicative(Class<F> type, Semigroup<E> semigroup) {
    return new Instance<F>(type) {}.applicative(semigroup);
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

  public static <F extends Witness> Async<F> async(Class<F> type, Executor executor) {
    return new Instance<F>(type) {}.async(executor);
  }

  public static <F extends Witness> Concurrent<F> concurrent(Class<F> type) {
    return new Instance<F>(type) {}.concurrent();
  }

  public static <F extends Witness> Runtime<F> runtime(Class<F> type) {
    return new Instance<F>(type) {}.runtime();
  }

  public static <F extends Witness> Console<F> console(Class<F> type) {
    return new Instance<F>(type) {}.console();
  }

  public static <F extends Witness> Foldable<F> foldable(Class<F> type) {
    return new Instance<F>(type) {}.foldable();
  }

  public static <F extends Witness> Traverse<F> traverse(Class<F> type) {
    return new Instance<F>(type) {}.traverse();
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
