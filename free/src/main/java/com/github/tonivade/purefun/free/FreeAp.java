/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static java.util.Collections.singletonList;
import static com.github.tonivade.purefun.free.FreeApOf.toFreeAp;
import static com.github.tonivade.purefun.free.FreeOf.toFree;
import static com.github.tonivade.purefun.type.ConstOf.toConst;
import java.util.Deque;
import java.util.LinkedList;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Const_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.FunctionK;

@HigherKind
public abstract class FreeAp<F extends Witness, A> implements FreeApOf<F, A> {

  private FreeAp() {}

  public abstract <B> FreeAp<F, B> map(Function1<? super A, ? extends B> mapper);

  public <B> FreeAp<F, B> ap(FreeAp<F, Function1<? super A, ? extends B>> apply) {
    if (apply instanceof Pure) {
      Pure<F, Function1<? super A, ? extends B>> pure = (Pure<F, Function1<? super A, ? extends B>>) apply;
      return map(pure.value);
    }
    return apply(this, apply);
  }

  public Kind<F, A> fold(Applicative<F> applicative) {
    return foldMap(FunctionK.identity(), applicative);
  }

  public <G extends Witness> FreeAp<G, A> compile(FunctionK<F, G> transformer) {
    return foldMap(functionKF(transformer), applicativeF()).fix(toFreeAp());
  }

  public <G extends Witness> FreeAp<G, A> flatCompile(
      FunctionK<F, Kind<FreeAp_, G>> functionK, Applicative<Kind<FreeAp_, G>> applicative) {
    return foldMap(functionK, applicative).fix(toFreeAp());
  }

  public <M> M analyze(FunctionK<F, Kind<Const_, M>> functionK, Applicative<Kind<Const_, M>> applicative) {
    return foldMap(functionK, applicative).fix(toConst()).get();
  }

  public Free<F, A> monad() {
    return foldMap(Free.functionKF(FunctionK.identity()), Free.monadF()).fix(toFree());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public <G extends Witness> Kind<G, A> foldMap(FunctionK<F, G> functionK, Applicative<G> applicative) {
    Deque<FreeAp> argsF = new LinkedList<>(singletonList(this));
    Deque<CurriedFunction> fns = new LinkedList<>();

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
            if (fns.size() > 0) {
              do {
                function = fns.pollFirst();

                res = applicative.ap(res, function.value);

                if (function.remaining > 1) {
                  fns.addFirst(new CurriedFunction(res, function.remaining - 1));
                }
              } while (function.remaining == 1 && fns.size() > 0);
            }

            if (fns.size() == 0) {
              return res;
            }
          }
        } else {
          return argT;
        }
      }
    }
  }

  public static <F extends Witness, T> FreeAp<F, T> pure(T value) {
    return new FreeAp.Pure<>(value);
  }

  public static <F extends Witness, T> FreeAp<F, T> lift(Kind<F, T> value) {
    return new FreeAp.Lift<>(value);
  }

  public static <F extends Witness, T, R> FreeAp<F, R> apply(FreeAp<F, ? extends T> value, 
      FreeAp<F, ? extends Function1<? super T, ? extends R>> mapper) {
    return new FreeAp.Apply<>(value, mapper);
  }

  public static <F extends Witness, G extends Witness> FunctionK<F, Kind<FreeAp_, G>> functionKF(FunctionK<F, G> functionK) {
    return new FunctionK<F, Kind<FreeAp_, G>>() {
      @Override
      public <T> FreeAp<G, T> apply(Kind<F, ? extends T> from) {
        return lift(functionK.apply(from));
      }
    };
  }

  @SuppressWarnings("unchecked")
  public static <F extends Witness> Applicative<Kind<FreeAp_, F>> applicativeF() {
    return FreeApplicative.INSTANCE;
  }

  private static <F extends Witness, G extends Witness, A> Kind<G, A> foldArg(
      FreeAp<F, A> argF, FunctionK<F, G> transformation, Applicative<G> applicative) {
    if (argF instanceof Pure) {
      return applicative.pure(((Pure<F, A>) argF).value);
    }
    if (argF instanceof Lift) {
      return transformation.apply(((Lift<F, A>) argF).value);
    }
    throw new IllegalStateException();
  }

  private static final class Pure<F extends Witness, A> extends FreeAp<F, A> {

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

  private static final class Lift<F extends Witness, A> extends FreeAp<F, A> {

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

  private static final class Apply<F extends Witness, A, B> extends FreeAp<F, B> {

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

  private static final class CurriedFunction<G extends Witness, A, B> {

    private final Kind<G, Function1<A, B>> value;
    private final int remaining;

    CurriedFunction(Kind<G, Function1<A, B>> value, int remaining) {
      this.value = checkNonNull(value);
      this.remaining = remaining;
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
    return FreeAp.apply(value.fix(toFreeAp()), apply.fix(toFreeAp()));
  }
}

