package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Either;

public interface Concurrent<F extends Witness> extends Async<F> {
  
  <A> Kind<F, Fiber<F, A>> fork(Kind<F, A> value);
  
  <A, B> Kind<F, Either<Tuple2<A, Fiber<F, B>>, Tuple2<Fiber<F, A>, B>>> racePair(Kind<F, A> fa, Kind<F, B> fb);
  
  default <A> Resource<F, Kind<F, A>> background(Kind<F, A> acquire) {
    return Resource.from(this, fork(acquire), Fiber::cancel).map(Fiber::join);
  }

  default <A, B> Kind<F, Either<A, B>> race(Kind<F, A> fa, Kind<F, B> fb) {
    return flatMap(racePair(fa, fb), either -> either.fold(
        ta -> map(ta.get2().cancel(), x -> Either.left(ta.get1())), 
        tb -> map(tb.get1().cancel(), x -> Either.right(tb.get2()))));
  }

}
