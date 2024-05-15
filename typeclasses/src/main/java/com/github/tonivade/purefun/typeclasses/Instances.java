/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

public final class Instances {

  private Instances() { }

  public static <F> Functor<F> functor(Class<F> type, Object...params) {
    return new Instance<>(type) {}.functor(params);
  }

  @SafeVarargs
  public static <F> Functor<F> functor(F...reified) {
    return functor(getClassOf(reified));
  }

  public static <F> Bifunctor<F> bifunctor(Class<F> type, Object...params) {
    return new Instance<>(type) {}.bifunctor(params);
  }

  @SafeVarargs
  public static <F> Bifunctor<F> bifunctor(F...reified) {
    return bifunctor(getClassOf(reified));
  }

  public static <F> Applicative<F> applicative(Class<F> type, Object...params) {
    return new Instance<>(type) {}.applicative(params);
  }

  @SafeVarargs
  public static <F> Applicative<F> applicative(F...reified) {
    return applicative(getClassOf(reified));
  }

  public static <F> Monad<F> monad(Class<F> type, Object...params) {
    return new Instance<>(type) {}.monad(params);
  }

  @SafeVarargs
  public static <F> Monad<F> monad(F...reified) {
    return monad(getClassOf(reified));
  }

  public static <F, R> MonadReader<F, R> monadReader(Class<F> type, Object...params) {
    return new Instance<>(type) {}.monadReader(params);
  }

  @SafeVarargs
  public static <F, R> MonadReader<F, R> monadReader(F...reified) {
    return monadReader(getClassOf(reified));
  }

  public static <F, S> MonadState<F, S> monadState(Class<F> type, Object...params) {
    return new Instance<>(type) {}.monadState(params);
  }

  @SafeVarargs
  public static <F, S> MonadState<F, S> monadState(F...reified) {
    return monadState(getClassOf(reified));
  }

  public static <F, W> MonadWriter<F, W> monadWriter(Class<F> type, Object...params) {
    return new Instance<>(type) {}.monadWriter(params);
  }

  @SafeVarargs
  public static <F, W> MonadWriter<F, W> monadWriter(F...reified) {
    return monadWriter(getClassOf(reified));
  }

  public static <F> Comonad<F> comonad(Class<F> type, Object...params) {
    return new Instance<>(type) {}.comonad(params);
  }

  @SafeVarargs
  public static <F> Comonad<F> comonad(F...reified) {
    return comonad(getClassOf(reified));
  }

  public static <F> Selective<F> selective(Class<F> type, Object...params) {
    return new Instance<>(type) {}.selective(params);
  }

  @SafeVarargs
  public static <F> Selective<F> selective(F...reified) {
    return selective(getClassOf(reified));
  }

  public static <F, E> ApplicativeError<F, E> applicativeError(Class<F> type, Object...params) {
    return new Instance<>(type) {}.applicativeError(params);
  }

  @SafeVarargs
  public static <F, E> ApplicativeError<F, E> applicativeError(F...reified) {
    return applicativeError(getClassOf(reified));
  }

  public static <F, E> MonadError<F, E> monadError(Class<F> type, Object...params) {
    return new Instance<>(type) {}.monadError(params);
  }

  @SafeVarargs
  public static <F, E> MonadError<F, E> monadError(F...reified) {
    return monadError(getClassOf(reified));
  }

  public static <F> MonadThrow<F> monadThrow(Class<F> type, Object...params) {
    return new Instance<>(type) {}.monadThrow(params);
  }

  @SafeVarargs
  public static <F> MonadThrow<F> monadThrow(F...reified) {
    return monadThrow(getClassOf(reified));
  }

  public static <F> MonadDefer<F> monadDefer(Class<F> type, Object...params) {
    return new Instance<>(type) {}.monadDefer(params);
  }

  @SafeVarargs
  public static <F> MonadDefer<F> monadDefer(F...reified) {
    return monadDefer(getClassOf(reified));
  }

  public static <F> Async<F> async(Class<F> type, Object...params) {
    return new Instance<>(type) {}.async(params);
  }

  @SafeVarargs
  public static <F> Async<F> async(F...reified) {
    return async(getClassOf(reified));
  }

  public static <F> Concurrent<F> concurrent(Class<F> type, Object...params) {
    return new Instance<>(type) {}.concurrent(params);
  }

  @SafeVarargs
  public static <F> Concurrent<F> concurrent(F...reified) {
    return concurrent(getClassOf(reified));
  }

  public static <F> Runtime<F> runtime(Class<F> type, Object...params) {
    return new Instance<>(type) {}.runtime(params);
  }

  @SafeVarargs
  public static <F> Runtime<F> runtime(F...reified) {
    return runtime(getClassOf(reified));
  }

  public static <F> Console<F> console(Class<F> type, Object...params) {
    return new Instance<>(type) {}.console(params);
  }

  @SafeVarargs
  public static <F> Console<F> console(F...reified) {
    return console(getClassOf(reified));
  }

  public static <F> Foldable<F> foldable(Class<F> type, Object...params) {
    return new Instance<>(type) {}.foldable(params);
  }

  @SafeVarargs
  public static <F> Foldable<F> foldable(F...reified) {
    return foldable(getClassOf(reified));
  }

  public static <F> Traverse<F> traverse(Class<F> type, Object...params) {
    return new Instance<>(type) {}.traverse(params);
  }

  @SafeVarargs
  public static <F> Traverse<F> traverse(F...reified) {
    return traverse(getClassOf(reified));
  }

  @SuppressWarnings("unchecked")
  private static <F> Class<F> getClassOf(F... reified) {
    if (reified.length > 0) {
      throw new IllegalArgumentException("do not pass arguments to this function, it's just a trick to get refied types");
    }
    return (Class<F>) reified.getClass().getComponentType();
  }
}
