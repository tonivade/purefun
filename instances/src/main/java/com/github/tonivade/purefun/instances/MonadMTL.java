/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import java.util.function.Function;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.instances.MonadMTL.EffectE;
import com.github.tonivade.purefun.instances.MonadMTL.EffectR;
import com.github.tonivade.purefun.instances.MonadMTL.EffectS;
import com.github.tonivade.purefun.transformer.EitherT;
import com.github.tonivade.purefun.transformer.EitherTOf;
import com.github.tonivade.purefun.transformer.Kleisli;
import com.github.tonivade.purefun.transformer.KleisliOf;
import com.github.tonivade.purefun.transformer.StateT;
import com.github.tonivade.purefun.transformer.StateTOf;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadReader;
import com.github.tonivade.purefun.typeclasses.MonadState;

public class MonadMTL<F extends Kind<F, ?>, S, R, E>
    implements Monad<EffectS<F, S, R, E, ?>>,
      MonadError<EffectS<F, S, R, E, ?>, E>,
      MonadState<EffectS<F, S, R, E, ?>, S>,
      MonadReader<EffectS<F, S, R, E, ?>, R> {

  private final Monad<F> monad;
  private final MonadError<EffectE<F, E, ?>, E> monadErrorE;
  private final Monad<EffectR<F, R, E, ?>> monadR;
  private final MonadError<StateT<EffectR<F, R, E, ?>, S, ?>, E> monadErrorS;
  private final MonadReader<StateT<EffectR<F, R, E, ?>, S, ?>, R> monadReaderS;
  private final MonadState<StateT<EffectR<F, R, E, ?>, S, ?>, S> monadStateS;

  public MonadMTL(Monad<F> monad) {
    this.monad = checkNonNull(monad);
    this.monadErrorE = new EffectEMonadError<>(monad);
    this.monadR = new EffectRMonad<>(monad);
    this.monadErrorS = StateTInstances.monadError(new EffectRMonadError<>(monad));
    this.monadReaderS = StateTInstances.monadReader(new EffectRMonadReader<>(monad));
    this.monadStateS = StateTInstances.monadState(monadR);
  }

  @Override
  public <A> EffectS<F, S, R, E, A> pure(A value) {
    return new EffectS<>(monadStateS.pure(value));
  }

  @Override
  public <A, B> EffectS<F, S, R, E, B> flatMap(Kind<EffectS<F, S, R, E, ?>, ? extends A> value,
      Function1<? super A, ? extends Kind<EffectS<F, S, R, E, ?>, ? extends B>> map) {
    return new EffectS<>(monadStateS.flatMap(
        value.fix(EffectS::<F, S, R, E, A>toEffectS).value(),
        x -> map.apply(x).fix(EffectS::<F, S, R, E, B>toEffectS).value()));
  }

  @Override
  public EffectS<F, S, R, E, S> get() {
    return new EffectS<>(monadStateS.get());
  }

  @Override
  public EffectS<F, S, R, E, Unit> set(S state) {
    return new EffectS<>(monadStateS.set(state));
  }

  @Override
  public <A> EffectS<F, S, R, E, A> raiseError(E error) {
    return new EffectS<>(monadErrorS.raiseError(error));
  }

  @Override
  public <A> EffectS<F, S, R, E, A> handleErrorWith(
      Kind<EffectS<F, S, R, E, ?>, A> value, Function1<? super E, ? extends Kind<EffectS<F, S, R, E, ?>, ? extends A>> handler) {
    return new EffectS<>(monadErrorS.handleErrorWith(
        value.fix(EffectS::<F, S, R, E, A>toEffectS).value(),
        error -> handler.apply(error).fix(EffectS::<F, S, R, E, A>toEffectS).value()));
  }

  @Override
  public EffectS<F, S, R, E, R> ask() {
    return new EffectS<>(monadReaderS.ask());
  }

  public <A> EffectE<F, E, A> effectE(Kind<F, Either<E, A>> value) {
    return new EffectE<>(EitherT.of(monad, value));
  }

  public <A> EffectR<F, R, E, A> effectR(EffectE<F, E, A> effect0) {
    return new EffectR<>(Kleisli.of(monadErrorE, config -> effect0));
  }

  public <A> EffectS<F, S, R, E, A> effectS(EffectR<F, R, E, A> effect1) {
    return new EffectS<>(StateT.state(monadR, state -> monadR.map(effect1, x -> Tuple.of(state, x))));
  }

  public <A> EffectS<F, S, R, E, A> effect(Kind<F, Either<E, A>> value) {
    return effectS(effectR(effectE(value)));
  }

  public static final class EffectE<F extends Kind<F, ?>, E, A> implements Kind<EffectE<F, E, ?>, A> {

    private final EitherT<F, E, A> value;

    public EffectE(Kind<EitherT<F, E, ?>, A> value) {
      this.value = value.fix(EitherTOf::toEitherT);
    }

    public EitherT<F, E, A> value() {
      return value;
    }

    public Kind<F, Either<E, A>> run() {
      return value.value();
    }

    public <R extends Kind<F, Either<E, A>>> R run(Function<Kind<F, ? extends Either<E, A>>, R> fixer) {
      return run().fix(fixer);
    }

    @SuppressWarnings("unchecked")
    public static <F extends Kind<F, ?>, E, A> EffectE<F, E, A> toEffectE(Kind<EffectE<F, E, ?>, ? extends A> hkt) {
      return (EffectE<F, E, A>) hkt;
    }
  }

  public static final class EffectR<F extends Kind<F, ?>, R, E, A> implements Kind<EffectR<F, R, E, ?>, A> {

    private final Kleisli<EffectE<F, E, ?>, R, A> value;

    public EffectR(Kind<Kleisli<EffectE<F, E, ?>, R, ?>, A> value) {
      this.value = value.fix(KleisliOf::toKleisli);
    }

    public Kleisli<EffectE<F, E, ?>, R, A> value() {
      return value;
    }

    public EffectE<F, E, A> run(R config) {
      return value.run(config).fix(EffectE::toEffectE);
    }

    @SuppressWarnings("unchecked")
    public static <F extends Kind<F, ?>, R, E, A> EffectR<F, R, E, A> toEffectR(Kind<EffectR<F, R, E, ?>, ? extends A> hkt) {
      return (EffectR<F, R, E, A>) hkt;
    }
  }

  public static final class EffectS<F extends Kind<F, ?>, S, R, E, A> implements Kind<EffectS<F, S, R, E, ?>, A> {

    private final StateT<EffectR<F, R, E, ?>, S, A> value;

    public EffectS(Kind<StateT<EffectR<F, R, E, ?>, S, ?>, A> value) {
      this.value = value.fix(StateTOf::toStateT);
    }

    public StateT<EffectR<F, R, E, ?>, S, A> value() {
      return value;
    }

    public EffectR<F, R, E, Tuple2<S, A>> run(S state) {
      return value.run(state).fix(EffectR::toEffectR);
    }

    public EffectE<F, E, Tuple2<S, A>> run(S state, R env) {
      return run(state).run(env);
    }

    @SuppressWarnings("unchecked")
    public static <F extends Kind<F, ?>, S, R, E, A> EffectS<F, S, R, E, A> toEffectS(Kind<EffectS<F, S, R, E, ?>, ? extends A> hkt) {
      return (EffectS<F, S, R, E, A>) hkt;
    }
  }
}

class EffectEMonadError<F extends Kind<F, ?>, E> implements MonadError<EffectE<F, E, ?>, E> {

  private final MonadError<EitherT<F, E, ?>, E> monad;

  public EffectEMonadError(Monad<F> monad) {
    this.monad = EitherTInstances.monadError(monad);
  }

  @Override
  public <A> EffectE<F, E, A> pure(A value) {
    return new EffectE<>(monad.pure(value));
  }

  @Override
  public <A, B> EffectE<F, E, B> flatMap(Kind<EffectE<F, E, ?>, ? extends A> value,
      Function1<? super A, ? extends Kind<EffectE<F, E, ?>, ? extends B>> map) {
    return new EffectE<>(monad.flatMap(
        value.fix(EffectE::<F, E, A>toEffectE).value(),
        x -> map.apply(x).fix(EffectE::<F, E, B>toEffectE).value()));
  }

  @Override
  public <A> EffectE<F, E, A> raiseError(E error) {
    return new EffectE<>(monad.raiseError(error));
  }

  @Override
  public <A> EffectE<F, E, A> handleErrorWith(Kind<EffectE<F, E, ?>, A> value,
      Function1<? super E, ? extends Kind<EffectE<F, E, ?>, ? extends A>> handler) {
    return new EffectE<>(monad.handleErrorWith(value.fix(EffectE::<F, E, A>toEffectE).value(),
            error -> handler.apply(error).fix(EffectE::<F, E, A>toEffectE).value()));
  }
}

class EffectRMonad<F extends Kind<F, ?>, R, E> implements Monad<EffectR<F, R, E, ?>> {

  private final Monad<Kleisli<EffectE<F, E, ?>, R, ?>> monad;

  public EffectRMonad(Monad<F> monad) {
    this.monad = KleisliInstances.monad(new EffectEMonadError<F, E>(monad));
  }

  @Override
  public <A> EffectR<F, R, E, A> pure(A value) {
    return new EffectR<>(monad.pure(value));
  }

  @Override
  public <A, B> EffectR<F, R, E, B> flatMap(Kind<EffectR<F, R, E, ?>, ? extends A> value,
      Function1<? super A, ? extends Kind<EffectR<F, R, E, ?>, ? extends B>> map) {
    return new EffectR<>(monad.flatMap(value.fix(EffectR::<F, R, E, A>toEffectR).value(),
        t -> map.apply(t).fix(EffectR::<F, R, E, B>toEffectR).value()));
  }
}

class EffectRMonadReader<F extends Kind<F, ?>, R, E> extends EffectRMonad<F, R, E> implements MonadReader<EffectR<F, R, E, ?>, R> {

  private final MonadReader<Kleisli<EffectE<F, E, ?>, R, ?>, R> monad;

  public EffectRMonadReader(Monad<F> monad) {
    super(monad);
    this.monad = KleisliInstances.monadReader(new EffectEMonadError<F, E>(monad));
  }

  @Override
  public EffectR<F, R, E, R> ask() {
    return new EffectR<>(monad.ask());
  }
}

class EffectRMonadError<F extends Kind<F, ?>, R, E> extends EffectRMonad<F, R, E> implements MonadError<EffectR<F, R, E, ?>, E> {

  private final MonadError<Kleisli<EffectE<F, E, ?>, R, ?>, E> monadError;

  public EffectRMonadError(Monad<F> monad) {
    super(monad);
    this.monadError = KleisliInstances.monadError(new EffectEMonadError<>(monad));
  }

  @Override
  public <A> EffectR<F, R, E, A> raiseError(E error) {
    return new EffectR<>(monadError.raiseError(error));
  }

  @Override
  public <A> EffectR<F, R, E, A> handleErrorWith(
      Kind<EffectR<F, R, E, ?>, A> value, Function1<? super E, ? extends Kind<EffectR<F, R, E, ?>, ? extends A>> handler) {
    return new EffectR<>(monadError.handleErrorWith(
        value.fix(EffectR::<F, R, E, A>toEffectR).value(),
        error -> handler.apply(error).fix(EffectR::<F, R, E, A>toEffectR).value()));
  }
}
