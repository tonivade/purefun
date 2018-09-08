package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.monad.FreeKind.narrowK;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Monad2;
import com.github.tonivade.purefun.Witness;

public interface Free<F extends Witness, T> extends Monad2<FreeKind.µ, F, T> {

  static <F extends Witness, T> Free<F, T> pure(T value) {
    return new Pure<>(value);
  }

  static <F extends Witness, T> Free<F, T> liftF(Functor<F> functor, Higher<F, T> value) {
    return new Suspend<>(functor, functor.map(value, Free::pure));
  }

  @Override
  default <R> Free<F, R> map(Function1<T, R> map) {
    return flatMap(map.andThen(Free::pure));
  }

  @Override
  <R> Free<F, R> flatMap(Function1<T, ? extends Higher2<FreeKind.µ, F, R>> map);

  default <M extends Witness> Higher<M, T> foldMap(Monad<M> monad, Transformer<F, M> interpreter) {
    if (this instanceof Pure) {
      Pure<F, T> pure = (Pure<F, T>) this;
      return monad.pure(pure.value);
    }
    if (this instanceof Suspend) {
      Suspend<F, T> suspend = ((Suspend<F, T>) this);
      return monad.map(interpreter.apply(suspend.value), x -> null);
    }
    return null;
  }

  final class Pure<F extends Witness, T> implements Free<F, T> {
    final T value;

    private Pure(T value) {
      this.value = requireNonNull(value);
    }

    @Override
    public <R> Free<F, R> flatMap(Function1<T, ? extends Higher2<FreeKind.µ, F, R>> map) {
      return narrowK(map.apply(value));
    }
  }

  final class Suspend<F extends Witness, T> implements Free<F, T> {
    final Functor<F> functor;
    final Higher<F, Free<F, T>> value;

    private Suspend(Functor<F> functor, Higher<F, Free<F, T>> value) {
      this.functor = requireNonNull(functor);
      this.value = requireNonNull(value);
    }

    @Override
    public <R> Free<F, R> flatMap(Function1<T, ? extends Higher2<FreeKind.µ, F, R>> map) {
      return new Suspend<>(functor, functor.map(value, x -> x.flatMap(map)));
    }
  }
}

interface Functor<F extends Witness> {

  <T, R> Higher<F, R> map(Higher<F, T> value, Function1<T, R> map);
}

interface Monad<F extends Witness> extends Functor<F> {
  <T> Higher<F, T> pure(T value);

  <T, R> Higher<F, R> flatMap(Higher<F, T> value, Function1<T, ? extends Higher<F, R>> map);
}