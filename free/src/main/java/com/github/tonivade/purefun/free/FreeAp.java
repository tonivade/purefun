/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Monoid;

import java.util.Deque;
import java.util.LinkedList;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

@HigherKind
public abstract class FreeAp<F extends Kind, A> {

  private FreeAp() {}

  public abstract <B> FreeAp<F, B> map(Function1<A, B> mapper);

  public <B> FreeAp<F, B> ap(FreeAp<F, Function1<A, B>> apply) {
    if (apply instanceof Pure) {
      Pure<F, Function1<A, B>> pure = (Pure<F, Function1<A, B>>) apply;
      return map(pure.value);
    }
    return apply(this, apply);
  }

  public Higher1<F, A> fold(Applicative<F> applicative) {
    return foldMap(FunctionK.identity(), applicative);
  }

  public <G extends Kind> FreeAp<G, A> compile(FunctionK<F, G> transformer) {
    return foldMap(functionKF(transformer), applicativeF()).fix1(FreeAp::narrowK);
  }

  public <G extends Kind> FreeAp<G, A> flatCompile(
      FunctionK<F, Higher1<FreeAp.µ, G>> functionK, Applicative<Higher1<FreeAp.µ, G>> applicative) {
    return foldMap(functionK, applicative).fix1(FreeAp::narrowK);
  }

  public <M> M analyze(FunctionK<F, Higher1<Const.µ, M>> functionK, Applicative<Higher1<Const.µ, M>> applicative) {
    return foldMap(functionK, applicative).fix1(Const::narrowK).get();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public <G extends Kind> Higher1<G, A> foldMap(FunctionK<F, G> functionK, Applicative<G> applicative) {
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
        Higher1 argT = foldArg(argF, functionK, applicative);

        if (!fns.isEmpty()) {
          CurriedFunction function = fns.pollFirst();

          Higher1 res = applicative.ap(argT, function.value);

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
              return (Higher1<G, A>) res;
            }
          }
        } else {
          return (Higher1<G, A>) argT;
        }
      }
    }
  }

  public static <F extends Kind, T> FreeAp<F, T> pure(T value) {
    return new FreeAp.Pure<>(value);
  }

  public static <F extends Kind, T> FreeAp<F, T> lift(Higher1<F, T> value) {
    return new FreeAp.Lift<>(value);
  }

  public static <F extends Kind, T, R> FreeAp<F, R> apply(FreeAp<F, T> value, FreeAp<F, Function1<T, R>> mapper) {
    return new FreeAp.Apply<>(value, mapper);
  }

  public static <F extends Kind, G extends Kind> FunctionK<F, Higher1<FreeAp.µ, G>> functionKF(FunctionK<F, G> functionK) {
    return new FunctionK<F, Higher1<FreeAp.µ, G>>() {
      @Override
      public <T> Higher2<FreeAp.µ, G, T> apply(Higher1<F, T> from) {
        return lift(functionK.apply(from)).kind2();
      }
    };
  }

  public static <F extends Kind> Applicative<Higher1<FreeAp.µ, F>> applicativeF() {
    return FreeApplicative.instance();
  }

  private static <F extends Kind, G extends Kind, A> Higher1<G, A> foldArg(
      FreeAp<F, A> argF, FunctionK<F, G> transformation, Applicative<G> applicative) {
    if (argF instanceof Pure) {
      return applicative.pure(((Pure<F, A>) argF).value);
    }
    if (argF instanceof Lift) {
      return transformation.apply(((Lift<F, A>) argF).value);
    }
    throw new IllegalStateException();
  }

  private static final class Pure<F extends Kind, A> extends FreeAp<F, A> {

    private final A value;

    private Pure(A value) {
      this.value = requireNonNull(value);
    }

    @Override
    public <B> FreeAp<F, B> map(Function1<A, B> mapper) {
      return pure(mapper.apply(value));
    }

    @Override
    public String toString() {
      return "Pure(" + value + ')';
    }
  }

  private static final class Lift<F extends Kind, A> extends FreeAp<F, A> {

    private final Higher1<F, A> value;

    private Lift(Higher1<F, A> value) {
      this.value = requireNonNull(value);
    }

    @Override
    public <B> FreeAp<F, B> map(Function1<A, B> mapper) {
      return apply(this, pure(mapper));
    }

    @Override
    public String toString() {
      return "Lift(" + value + ')';
    }
  }

  private static final class Apply<F extends Kind, A, B> extends FreeAp<F, B> {

    private final FreeAp<F, A> value;
    private final FreeAp<F, Function1<A, B>> apply;

    private Apply(FreeAp<F, A> value, FreeAp<F, Function1<A, B>> apply) {
      this.value = requireNonNull(value);
      this.apply = requireNonNull(apply);
    }

    @Override
    public <C> FreeAp<F, C> map(Function1<B, C> mapper) {
      return apply(this, pure(mapper));
    }

    @Override
    public String toString() {
      return "Apply(" + value + ", ...)";
    }
  }

  private static final class CurriedFunction<G extends Kind, A, B> {

    private final Higher1<G, Function1<A, B>> value;
    private final int remaining;

    CurriedFunction(Higher1<G, Function1<A, B>> value, int remaining) {
      this.value = requireNonNull(value);
      this.remaining = remaining;
    }
  }
}

@Instance
interface FreeApplicative<F extends Kind> extends Applicative<Higher1<FreeAp.µ, F>> {

  @Override
  default <T> Higher2<FreeAp.µ, F, T> pure(T value) {
    return FreeAp.<F, T>pure(value).kind2();
  }

  @Override
  default <T, R> Higher2<FreeAp.µ, F, R> ap(
      Higher1<Higher1<FreeAp.µ, F>, T> value, Higher1<Higher1<FreeAp.µ, F>, Function1<T, R>> apply) {
    FreeAp<F, T> freeAp = value.fix1(FreeAp::narrowK);
    FreeAp<F, Function1<T, R>> apply1 = apply.fix1(FreeAp::narrowK);
    return FreeAp.apply(freeAp, apply1).kind2();
  }
}

