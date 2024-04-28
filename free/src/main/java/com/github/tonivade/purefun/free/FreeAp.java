/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.free.FreeApOf.toFreeAp;
import static com.github.tonivade.purefun.free.FreeOf.toFree;
import static com.github.tonivade.purefun.type.ConstOf.toConst;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

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
  default <B> FreeAp<F, B> map(Function1<? super A, ? extends B> mapper) {
    return switch (this) {
      case Pure<F, A>(var value) -> pure(mapper.apply(value));
      case Lift<F, A> lift -> apply(this, pure(mapper));
      case Apply<F, ?, A> apply -> FreeAp.apply(this, pure(mapper));
    };
  }

  @Override
  default <B> FreeAp<F, B> ap(Kind<Kind<FreeAp_, F>, ? extends Function1<? super A, ? extends B>> apply) {
    if (apply instanceof Pure<F, ? extends Function1<? super A, ? extends B>> pure) {
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
    Deque<FreeAp> argsF = new ArrayDeque<>(List.of(this));
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
    return new Pure<>(value);
  }

  static <F extends Witness, T> FreeAp<F, T> lift(Kind<F, T> value) {
    return new Lift<>(value);
  }

  static <F extends Witness, T, R> FreeAp<F, R> apply(Kind<Kind<FreeAp_, F>, ? extends T> value,
      Kind<Kind<FreeAp_, F>, ? extends Function1<? super T, ? extends R>> mapper) {
    return new Apply<>(value.fix(toFreeAp()), mapper.fix(toFreeAp()));
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
    if (argF instanceof Pure<F, A>(var value)) {
      return applicative.pure(value);
    }
    if (argF instanceof Lift<F, A>(var value)) {
      return transformation.apply(value);
    }
    throw new IllegalStateException("unreachable code");
  }

  record Pure<F extends Witness, A>(A value) implements FreeAp<F, A> {

    public Pure {
      checkNonNull(value);
    }

    @Override
    public String toString() {
      return "Pure(" + value + ')';
    }
  }

  record Lift<F extends Witness, A>(Kind<F, A> value) implements FreeAp<F, A> {

    public Lift {
      checkNonNull(value);
    }

    @Override
    public String toString() {
      return "Lift(" + value + ')';
    }
  }

  record Apply<F extends Witness, A, B>(
      FreeAp<F, ? extends A> value,
      FreeAp<F, ? extends Function1<? super A, ? extends B>> apply) implements FreeAp<F, B> {

    public Apply {
      checkNonNull(value);
      checkNonNull(apply);
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

