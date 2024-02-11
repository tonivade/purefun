/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.free.FreeApOf.toFreeAp;
import static com.github.tonivade.purefun.free.FreeOf.toFree;
import static com.github.tonivade.purefun.type.ConstOf.toConst;
import static java.util.Collections.singletonList;
import java.util.ArrayDeque;
import java.util.Deque;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Applicable;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.type.Const_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.FunctionK;

@HigherKind
public sealed interface FreeAp<F extends Witness, A> extends FreeApOf<F, A>, Applicable<Kind<FreeAp_, F>, A> {

  @Override
  <B> FreeAp<F, B> map(Function1<? super A, ? extends B> mapper);

  @Override
  default <B> FreeAp<F, B> ap(Kind<Kind<FreeAp_, F>, Function1<? super A, ? extends B>> apply) {
    if (apply instanceof Pure<F, Function1<? super A, ? extends B>> pure) {
      return map(pure.value);
    }
    return apply(this, apply);
  }

  default Kind<F, A> fold(Applicative<F> applicative) {
    return foldMap(FunctionK.identity(), applicative);
  }

  default <G extends Witness> FreeAp<G, A> compile(FunctionK<F, G> transformer) {
    return foldMap(functionKF(transformer), applicativeF()).fix(toFreeAp());
  }

  default <G extends Witness> FreeAp<G, A> flatCompile(
      FunctionK<F, Kind<FreeAp_, G>> functionK, Applicative<Kind<FreeAp_, G>> applicative) {
    return foldMap(functionK, applicative).fix(toFreeAp());
  }

  default <M> M analyze(FunctionK<F, Kind<Const_, M>> functionK, Applicative<Kind<Const_, M>> applicative) {
    return foldMap(functionK, applicative).fix(toConst()).value();
  }

  default Free<F, A> monad() {
    return foldMap(Free.functionKF(FunctionK.identity()), Free.monadF()).fix(toFree());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  default <G extends Witness> Kind<G, A> foldMap(FunctionK<F, G> functionK, Applicative<G> applicative) {
    Deque<FreeAp> argsF = new ArrayDeque<>(singletonList(this));
    Deque<CurriedFunction> fns = new ArrayDeque<>();

    while (true) {
      FreeAp argF = argsF.pollFirst();

      if (argF instanceof Apply) {
        int lengthInitial = argsF.size();

        do {
          Apply ap = (Apply) argF;
          argsF.addFirst(ap.value);
          argF = ap.apply;
        } while (argF instanceof Apply);

        int argc = argsF.size() - lengthInitial;
        fns.addFirst(new CurriedFunction(foldArg(argF, functionK, applicative), argc));
      } else {
        Kind argT = foldArg(argF, functionK, applicative);

        if (!fns.isEmpty()) {
          CurriedFunction function = fns.pollFirst();

          Kind res = applicative.ap(argT, function.value);

          if (function.remaining > 1) {
            fns.addFirst(new CurriedFunction(res, function.remaining - 1));
          } else {
            if (!fns.isEmpty()) {
              do {
                function = fns.pollFirst();

                res = applicative.ap(res, function.value);

                if (function.remaining > 1) {
                  fns.addFirst(new CurriedFunction(res, function.remaining - 1));
                }
              } while (function.remaining == 1 && !fns.isEmpty());
            }

            if (fns.isEmpty()) {
              return res;
            }
          }
        } else {
          return argT;
        }
      }
    }
  }

  static <F extends Witness, T> FreeAp<F, T> pure(T value) {
    return new FreeAp.Pure<>(value);
  }

  static <F extends Witness, T> FreeAp<F, T> lift(Kind<F, T> value) {
    return new FreeAp.Lift<>(value);
  }

  static <F extends Witness, T, R> FreeAp<F, R> apply(Kind<Kind<FreeAp_, F>, ? extends T> value, 
      Kind<Kind<FreeAp_, F>, ? extends Function1<? super T, ? extends R>> mapper) {
    return new FreeAp.Apply<>(value.fix(toFreeAp()), mapper.fix(toFreeAp()));
  }

  static <F extends Witness, G extends Witness> FunctionK<F, Kind<FreeAp_, G>> functionKF(FunctionK<F, G> functionK) {
    return new FunctionK<>() {
      @Override
      public <T> FreeAp<G, T> apply(Kind<F, ? extends T> from) {
        return lift(functionK.apply(from));
      }
    };
  }

  @SuppressWarnings("unchecked")
  static <F extends Witness> Applicative<Kind<FreeAp_, F>> applicativeF() {
    return FreeApplicative.INSTANCE;
  }

  private static <F extends Witness, G extends Witness, A> Kind<G, A> foldArg(
      FreeAp<F, A> argF, FunctionK<F, G> transformation, Applicative<G> applicative) {
    if (argF instanceof Pure<F, A> pure) {
      return applicative.pure(pure.value);
    }
    if (argF instanceof Lift<F, A> lift) {
      return transformation.apply(lift.value);
    }
    throw new IllegalStateException();
  }

  final class Pure<F extends Witness, A> implements FreeAp<F, A> {

    private final A value;

    private Pure(A value) {
      this.value = checkNonNull(value);
    }

    @Override
    public <B> FreeAp<F, B> map(Function1<? super A, ? extends B> mapper) {
      return pure(mapper.apply(value));
    }

    @Override
    public String toString() {
      return "Pure(" + value + ')';
    }
  }

  final class Lift<F extends Witness, A> implements FreeAp<F, A> {

    private final Kind<F, A> value;

    private Lift(Kind<F, A> value) {
      this.value = checkNonNull(value);
    }

    @Override
    public <B> FreeAp<F, B> map(Function1<? super A, ? extends B> mapper) {
      return apply(this, pure(mapper));
    }

    @Override
    public String toString() {
      return "Lift(" + value + ')';
    }
  }

  final class Apply<F extends Witness, A, B> implements FreeAp<F, B> {

    private final FreeAp<F, ? extends A> value;
    private final FreeAp<F, ? extends Function1<? super A, ? extends B>> apply;

    private Apply(
        FreeAp<F, ? extends A> value, 
        FreeAp<F, ? extends Function1<? super A, ? extends B>> apply) {
      this.value = checkNonNull(value);
      this.apply = checkNonNull(apply);
    }

    @Override
    public <C> FreeAp<F, C> map(Function1<? super B, ? extends C> mapper) {
      return apply(this, pure(mapper));
    }

    @Override
    public String toString() {
      return "Apply(" + value + ", ...)";
    }
  }

  record CurriedFunction<G extends Witness, A, B>(Kind<G, Function1<A, B>> value, int remaining) {

    public CurriedFunction {
      checkNonNull(value);
    }
  }
}

interface FreeApplicative<F extends Witness> extends Applicative<Kind<FreeAp_, F>> {

  @SuppressWarnings("rawtypes")
  FreeApplicative INSTANCE = new FreeApplicative() {};

  @Override
  default <T> FreeAp<F, T> pure(T value) {
    return FreeAp.pure(value);
  }

  @Override
  default <T, R> FreeAp<F, R> ap(
      Kind<Kind<FreeAp_, F>, ? extends T> value, 
      Kind<Kind<FreeAp_, F>, ? extends Function1<? super T, ? extends R>> apply) {
    return FreeAp.apply(value, apply);
  }
}

