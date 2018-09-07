package com.github.tonivade.purefun.monad;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Monad2;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Witness;

public interface Free<F extends Witness, T> extends Monad2<FreeKind.µ, F, T> {

  static <F extends Witness, T> Free<F, T> done(T value) {
    return new Done<>(value);
  }

  static <F extends Witness, T> Free<F, T> liftF(Higher<F, T> value) {
    return new More<>(value);
  }

  default <R> Free<F, R> map(Function1<T, R> map) {
    return flatMap(map.andThen(Free::done));
  }

  default <R> Free<F, R> flatMap(Function1<T, ? extends Monad2<FreeKind.µ, F, R>> map) {
    return new FlatMap<>(this, map.andThen(FreeKind::narrowK));
  }

  default Nothing foldMap(Object interpreter) {
    // TODO: implement foldMap
    return Nothing.nothing();
  }

  final class Done<F extends Witness, T> implements Free<F, T> {
    final T value;
    
    private Done(T value) {
      this.value = requireNonNull(value);
    }
  }

  final class More<F extends Witness, T> implements Free<F, T> {
    final Higher<F, T> free;

    private More(Higher<F, T> free) {
      this.free = requireNonNull(free);
    }
  }

  final class FlatMap<F extends Witness, T, I> implements Free<F, I> {
    final Free<F, T> free;
    final Function1<T, Free<F, I>> map;

    private FlatMap(Free<F, T> free, Function1<T, Free<F, I>> map) {
      this.free = requireNonNull(free);
      this.map = requireNonNull(map);
    }
  }
}
