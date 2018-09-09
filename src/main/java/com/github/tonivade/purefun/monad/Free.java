package com.github.tonivade.purefun.monad;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Monad2;
import com.github.tonivade.purefun.Transformer;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Either;

public interface Free<F extends Witness, T> extends Monad2<FreeKind.µ, F, T> {

  static <F extends Witness, T> Free<F, T> pure(T value) {
    return new Pure<>(value);
  }

  static <F extends Witness, T> Free<F, T> suspend(Higher<F, Free<F, T>> value) {
    return new Suspend<>(value);
  }

  static <F extends Witness, T> Free<F, T> liftF(Functor<F> functor, Higher<F, T> value) {
    return suspend(functor.map(value, Free::pure));
  }

  @Override
  default <R> Free<F, R> map(Function1<T, R> map) {
    return flatMap(map.andThen(Free::pure));
  }

  @Override
  <R> Free<F, R> flatMap(Function1<T, ? extends Higher2<FreeKind.µ, F, R>> map);

  default Either<Higher<F, Free<F, T>>, T> resume(Functor<F> functor) {
    return FreeModule.resume(this, functor);
  }

  default <G extends Witness> Higher<G, T> foldMap(Monad<G> monad,
                                                   Functor<F> functor,
                                                   Transformer<F, G> interpreter) {
    return resume(functor)
        .fold(left -> monad.flatMap(interpreter.apply(left),
                                    free -> free.foldMap(monad, functor, interpreter)),
              right -> monad.pure(right));
  }

  final class Pure<F extends Witness, T> implements Free<F, T> {

    final T value;

    private Pure(T value) {
      this.value = requireNonNull(value);
    }

    @Override
    public <R> Free<F, R> flatMap(Function1<T, ? extends Higher2<FreeKind.µ, F, R>> map) {
      return new FlatMap<>(this, map);
    }
  }

  final class Suspend<F extends Witness, T> implements Free<F, T> {

    final Higher<F, Free<F, T>> value;

    private Suspend(Higher<F, Free<F, T>> value) {
      this.value = requireNonNull(value);
    }

    @Override
    public <R> Free<F, R> flatMap(Function1<T, ? extends Higher2<FreeKind.µ, F, R>> map) {
      return new FlatMap<>(this, map);
    }
  }

  final class FlatMap<F extends Witness, T, R> implements Free<F, R> {

    final Higher2<FreeKind.µ, F, T> value;
    final Function1<T, ? extends Higher2<FreeKind.µ, F, R>> map;

    private FlatMap(Higher2<FreeKind.µ, F, T> value, Function1<T, ? extends Higher2<FreeKind.µ, F, R>> map) {
      this.value = requireNonNull(value);
      this.map = requireNonNull(map);
    }

    @Override
    public <X> Free<F, X> flatMap(Function1<R, ? extends Higher2<FreeKind.µ, F, X>> map) {
      return new FlatMap<>(value, free -> new FlatMap<>(narrowFn().apply(free), map));
    }

    Function1<T, Free<F, R>> narrowFn() {
      return map.andThen(FreeKind::narrowK);
    }

    Free<F, T> narrowK() {
      return FreeKind.narrowK(value);
    }
  }
}

interface FreeModule {

  static <F extends Witness, T> Free.Pure<F, T> asPure(Free<F, T> free) {
    return (Free.Pure<F, T>) free;
  }

  static <F extends Witness, T> Free.Suspend<F, T> asSuspend(Free<F, T> free) {
    return (Free.Suspend<F, T>) free;
  }

  @SuppressWarnings("unchecked")
  static <F extends Witness, T, X> Free.FlatMap<F, X, T> asFlatMap(Free<F, T> free) {
    return (Free.FlatMap<F, X, T>) free;
  }

  static <X1, X2, F extends Witness, T> Either<Higher<F, Free<F, T>>, T> resume(Free<F, T> current, Functor<F> functor) {
    while (true) {
      if (current instanceof Free.Pure) {
        return Either.right(asPure(current).value);
      } else if (current instanceof Free.Suspend) {
        return Either.left(asSuspend(current).value);
      } else if (current instanceof Free.FlatMap) {
        Free.FlatMap<F, X1, T> flatMap1 = asFlatMap(current);
        Free<F, X1> innerFree1 = flatMap1.narrowK();
        if (innerFree1 instanceof Free.Pure) {
          current = flatMap1.narrowFn().apply(asPure(innerFree1).value);
        } else if (innerFree1 instanceof Free.Suspend) {
          return Either.left(functor.map(asSuspend(innerFree1).value,
                                         x1 -> x1.flatMap(flatMap1.map)));
        } else if (innerFree1 instanceof Free.FlatMap) {
          Free.FlatMap<F, X2, X1> flatMap2 = asFlatMap(innerFree1);
          Free<F, X2> innerValue2 = flatMap2.narrowK();
          current = innerValue2.flatMap(x2 -> flatMap2.narrowFn().apply(x2).flatMap(flatMap1.map));
        }
      }
    }
  }
}

interface Functor<F extends Witness> {

  <T, R> Higher<F, R> map(Higher<F, T> value, Function1<T, R> map);
}

interface Monad<F extends Witness> extends Functor<F> {

  <T> Higher<F, T> pure(T value);

  <T, R> Higher<F, R> flatMap(Higher<F, T> value, Function1<T, ? extends Higher<F, R>> map);

  @Override
  default <T, R> Higher<F, R> map(Higher<F, T> value, Function1<T, R> map) {
    return flatMap(value, map.andThen(this::pure));
  }
}