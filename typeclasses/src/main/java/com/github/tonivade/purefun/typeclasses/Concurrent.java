/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.concurrent.Promise;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;

public interface Concurrent<F extends Witness> extends Async<F> {
  
  <A> Kind<F, Fiber<F, A>> fork(Kind<F, A> value);
  
  <A, B> Kind<F, Either<Tuple2<A, Fiber<F, B>>, Tuple2<Fiber<F, A>, B>>> racePair(Kind<F, A> fa, Kind<F, B> fb);
  
  default <A> Resource<F, Kind<F, A>> background(Kind<F, A> acquire) {
    Resource<F, ? extends Fiber<F, A>> from = Resource.from(this, fork(acquire), Fiber::cancel);
    return from.map(Fiber::join);
  }

  default <A, B> Kind<F, Either<A, B>> race(Kind<F, A> fa, Kind<F, B> fb) {
    return flatMap(racePair(fa, fb), either -> either.fold(
        ta -> map(ta.get2().cancel(), x -> Either.left(ta.get1())), 
        tb -> map(tb.get1().cancel(), x -> Either.right(tb.get2()))));
  }
  
  default <A> Kind<F, A> cancelable(Function1<Consumer1<? super Try<? extends A>>, Kind<F, Unit>> callback) {
    return asyncF(cb1 -> {
      Promise<Unit> promise = Promise.make();
      
      Kind<F, Unit> async = async(cb2 -> promise.onComplete(x -> cb2.accept(Try.success(Unit.unit()))));
      
      Kind<F, Unit> token = callback.apply(result -> {
        promise.complete(Try.success(Unit.unit()));
        cb1.accept(result);
      });
      
      return bracket(pure(token), ignore -> async, cancel -> cancel);
    });
  }

}
