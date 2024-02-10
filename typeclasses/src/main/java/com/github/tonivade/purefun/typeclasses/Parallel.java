/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Function3;
import com.github.tonivade.purefun.Function4;
import com.github.tonivade.purefun.Function5;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;

public interface Parallel<F extends Witness, G extends Witness> {
  
  Monad<F> monad();
  Applicative<G> applicative();
  
  <A> Kind<F, A> sequential(Kind<G, ? extends A> value);
  <A> Kind<G, A> parallel(Kind<F, ? extends A> value);
  
  default <T extends Witness, A> Kind<F, Kind<T, A>> parSequence(Traverse<T> traverse, 
      Kind<T, ? extends Kind<F, ? extends A>> value) {
    return sequential(traverse.traverse(applicative(), value, this::parallel));
  }
  
  default <T extends Witness, A, B> Kind<F, Kind<T, B>> parTraverse(Traverse<T> traverse,
      Kind<T, ? extends A> value, Function1<? super A, ? extends Kind<F, ? extends B>> mapper) {
    return sequential(traverse.traverse(applicative(), value, a -> parallel(mapper.apply(a))));
  }
  
  default <A, B> Kind<F, B> parAp(Kind<F, ? extends A> fa, 
      Kind<F, ? extends Function1<? super A, ? extends B>> apply) {
    return sequential(applicative().ap(parallel(fa), parallel(apply)));
  }
  
  default <A, B, R> Kind<F, R> parMapN(Kind<F, ? extends A> fa, Kind<F, ? extends B> fb, 
      Function2<? super A, ? super B, ? extends R> mapper) {
    return Kind.narrowK(sequential(applicative().mapN(parallel(fa), parallel(fb), mapper)));
  }
  
  default <A, B, C, R> Kind<F, R> parMapN(
      Kind<F, ? extends A> fa, Kind<F, ? extends B> fb, Kind<F, ? extends C> fc,
      Function3<? super A, ? super B, ? super C, ? extends R> mapper) {
    return Kind.narrowK(sequential(applicative().mapN(parallel(fa), parallel(fb), parallel(fc), mapper)));
  }
  
  default <A, B, C, D, R> Kind<F, R> parMapN(
      Kind<F, ? extends A> fa, Kind<F, ? extends B> fb, Kind<F, ? extends C> fc, Kind<F, ? extends D> fd,
      Function4<? super A, ? super B, ? super C, ? super D, ? extends R> mapper) {
    return Kind.narrowK(sequential(applicative().mapN(parallel(fa), parallel(fb), parallel(fc), parallel(fd), mapper)));
  }
  
  default <A, B, C, D, E, R> Kind<F, R> parMapN(
      Kind<F, ? extends A> fa, Kind<F, ? extends B> fb, Kind<F, ? extends C> fc, Kind<F, ? extends D> fd, Kind<F, ? extends E> fe,
      Function5<? super A, ? super B, ? super C, ? super D, ? super E, ? extends R> mapper) {
    return Kind.narrowK(sequential(applicative().mapN(parallel(fa), parallel(fb), parallel(fc), parallel(fd), parallel(fe), mapper)));
  }
  
  static <F extends Witness, G extends Witness> Parallel<F, G> of(
      Monad<F> monad, Applicative<G> applicative, FunctionK<F, G> to, FunctionK<G, F> from) {
    return new Parallel<>() {
      @Override
      public Applicative<G> applicative() { return applicative; }
      
      @Override
      public Monad<F> monad() { return monad; }
      
      @Override
      public <A> Kind<F, A> sequential(Kind<G, ? extends A> value) { return from.apply(value); }
      
      @Override
      public <A> Kind<G, A> parallel(Kind<F, ? extends A> value) { return to.apply(value); }
    };
  }
  
  static <F extends Witness> Parallel<F, F> identity(Monad<F> monad) {
    return new Parallel<>() {
      @Override
      public Applicative<F> applicative() { return monad; }
      
      @Override
      public Monad<F> monad() { return monad; }
      
      @Override
      public <A> Kind<F, A> parallel(Kind<F, ? extends A> value) { return Kind.narrowK(value); }
      
      @Override
      public <A> Kind<F, A> sequential(Kind<F, ? extends A> value) { return Kind.narrowK(value); }
    };
  }
}
