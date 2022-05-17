package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Witness;

public final class Instances {
  
  private Instances() { }

  public static <F extends Witness> Functor<F> functor(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.functor(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness> Functor<F> functor(F...reified) {
    return functor((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness> Bifunctor<F> bifunctor(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.bifunctor(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness> Bifunctor<F> bifunctor(F...reified) {
    return bifunctor((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness> Applicative<F> applicative(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.applicative(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness> Applicative<F> applicative(F...reified) {
    return applicative((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness> Monad<F> monad(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.monad(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness> Monad<F> monad(F...reified) {
    return monad((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness, R> MonadReader<F, R> monadReader(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.monadReader(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness, R> MonadReader<F, R> monadReader(F...reified) {
    return monadReader((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness, S> MonadState<F, S> monadState(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.monadState(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness, S> MonadState<F, S> monadState(F...reified) {
    return monadState((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness, W> MonadWriter<F, W> monadWriter(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.monadWriter(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness, W> MonadWriter<F, W> monadWriter(F...reified) {
    return monadWriter((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness> Comonad<F> comonad(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.comonad(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness> Comonad<F> comonad(F...reified) {
    return comonad((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness> Selective<F> selective(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.selective(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness> Selective<F> selective(F...reified) {
    return selective((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness, E> ApplicativeError<F, E> applicativeError(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.applicativeError(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness, E> ApplicativeError<F, E> applicativeError(F...reified) {
    return applicativeError((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness, E> MonadError<F, E> monadError(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.monadError(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness, E> MonadError<F, E> monadError(F...reified) {
    return monadError((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness> MonadThrow<F> monadThrow(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.monadThrow(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness> MonadThrow<F> monadThrow(F...reified) {
    return monadThrow((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness> MonadDefer<F> monadDefer(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.monadDefer(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness> MonadDefer<F> monadDefer(F...reified) {
    return monadDefer((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness> Async<F> async(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.async(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness> Async<F> async(F...reified) {
    return async((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness> Concurrent<F> concurrent(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.concurrent(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness> Concurrent<F> concurrent(F...reified) {
    return concurrent((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness> Runtime<F> runtime(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.runtime(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness> Runtime<F> runtime(F...reified) {
    return runtime((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness> Console<F> console(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.console(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness> Console<F> console(F...reified) {
    return console((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness> Foldable<F> foldable(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.foldable(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness> Foldable<F> foldable(F...reified) {
    return foldable((Class<F>) reified.getClass().getComponentType());
  }

  public static <F extends Witness> Traverse<F> traverse(Class<F> type, Object...params) {
    return new Instance<F>(type) {}.traverse(params);
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <F extends Witness> Traverse<F> traverse(F...reified) {
    return traverse((Class<F>) reified.getClass().getComponentType());
  }

}
