/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Applicable;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.ConstOf;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@HigherKind
public sealed interface FreeAp<F, A> extends FreeApOf<F, A>, Applicable<FreeAp<F, ?>, A> {

  @Override
  default <B> FreeAp<F, B> map(Function1<? super A, ? extends B> mapper) {
    return switch (this) {
      case Pure<F, A>(var value) -> pure(mapper.apply(value));
      case Lift<F, A> lift -> apply(this, pure(mapper));
      case Apply<F, ?, A> apply -> FreeAp.apply(this, pure(mapper));
    };
  }

  @Override
  default <B> FreeAp<F, B> ap(Kind<FreeAp<F, ?>, ? extends Function1<? super A, ? extends B>> apply) {
    if (apply instanceof Pure<F, ? extends Function1<? super A, ? extends B>> pure) {
      return map(pure.value);
    }
    return apply(this, apply);
  }

  default Kind<F, A> fold(Applicative<F> applicative) {
    return foldMap(FunctionK.identity(), applicative);
  }

  default <G> FreeAp<G, A> compile(FunctionK<F, G> transformer) {
    return foldMap(functionKF(transformer), applicativeF()).fix(FreeApOf::toFreeAp);
  }

  default <G> FreeAp<G, A> flatCompile(
      FunctionK<F, FreeAp<G, ?>> functionK, Applicative<FreeAp<G, ?>> applicative) {
    return foldMap(functionK, applicative).fix(FreeApOf::toFreeAp);
  }

  default <M> M analyze(FunctionK<F, Const<M, ?>> functionK, Applicative<Const<M, ?>> applicative) {
    return foldMap(functionK, applicative).fix(ConstOf::toConst).value();
  }

  default Free<F, A> monad() {
    return foldMap(Free.functionKF(FunctionK.identity()), Free.monadF()).fix(FreeOf::toFree);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  default <G> Kind<G, A> foldMap(FunctionK<F, G> functionK, Applicative<G> applicative) {
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

  static <F, T> FreeAp<F, T> pure(T value) {
    return new Pure<>(value);
  }

  static <F, T> FreeAp<F, T> lift(Kind<F, T> value) {
    return new Lift<>(value);
  }

  static <F, T, R> FreeAp<F, R> apply(Kind<FreeAp<F, ?>, ? extends T> value,
      Kind<FreeAp<F, ?>, ? extends Function1<? super T, ? extends R>> mapper) {
    return new Apply<>(value.fix(FreeApOf::toFreeAp), mapper.fix(FreeApOf::toFreeAp));
  }

  static <F, G> FunctionK<F, FreeAp<G, ?>> functionKF(FunctionK<F, G> functionK) {
    return new FunctionK<>() {
      @Override
      public <T> FreeAp<G, T> apply(Kind<F, ? extends T> from) {
        return lift(functionK.apply(from));
      }
    };
  }

  @SuppressWarnings("unchecked")
  static <F> Applicative<FreeAp<F, ?>> applicativeF() {
    return FreeApplicative.INSTANCE;
  }

  private static <F, G, A> Kind<G, A> foldArg(
      FreeAp<F, A> argF, FunctionK<F, G> transformation, Applicative<G> applicative) {
    if (argF instanceof Pure<F, A>(var value)) {
      return applicative.pure(value);
    }
    if (argF instanceof Lift<F, A>(var value)) {
      return transformation.apply(value);
    }
    throw new IllegalStateException("unreachable code");
  }

  record Pure<F, A>(A value) implements FreeAp<F, A> {

    public Pure {
      checkNonNull(value);
    }

    @Override
    public String toString() {
      return "Pure(" + value + ')';
    }
  }

  record Lift<F, A>(Kind<F, A> value) implements FreeAp<F, A> {

    public Lift {
      checkNonNull(value);
    }

    @Override
    public String toString() {
      return "Lift(" + value + ')';
    }
  }

  record Apply<F, A, B>(
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

  record CurriedFunction<G, A, B>(Kind<G, Function1<A, B>> value, int remaining) {

    public CurriedFunction {
      checkNonNull(value);
    }
  }
}

interface FreeApplicative<F> extends Applicative<FreeAp<F, ?>> {

  @SuppressWarnings("rawtypes")
  FreeApplicative INSTANCE = new FreeApplicative() {};

  @Override
  default <T> FreeAp<F, T> pure(T value) {
    return FreeAp.pure(value);
  }

  @Override
  default <T, R> FreeAp<F, R> ap(
      Kind<FreeAp<F, ?>, ? extends T> value,
      Kind<FreeAp<F, ?>, ? extends Function1<? super T, ? extends R>> apply) {
    return FreeAp.apply(value, apply);
  }
}

