/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.transformer.EitherTOf.toEitherT;
import static com.github.tonivade.purefun.transformer.KleisliOf.toKleisli;
import static com.github.tonivade.purefun.transformer.StateTOf.toStateT;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.instances.MonadMTL.EffectE;
import com.github.tonivade.purefun.instances.MonadMTL.EffectR;
import com.github.tonivade.purefun.instances.MonadMTL.EffectS;
import com.github.tonivade.purefun.transformer.EitherT;
import com.github.tonivade.purefun.transformer.Kleisli;
import com.github.tonivade.purefun.transformer.StateT;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadReader;
import com.github.tonivade.purefun.typeclasses.MonadState;

public class MonadMTL<F, S, R, E>
    implements Monad<EffectS<?, ?, ?, ?, ?>>,
      MonadError<EffectS<?, ?, ?, ?, ?>, E>,
      MonadState<EffectS<?, ?, ?, ?, ?>, S>,
      MonadReader<EffectS<?, ?, ?, ?, ?>, R> {

  private final Monad<F> monad;
  private final MonadError<EffectE<?, ?, ?>, E> monadErrorE;
  private final Monad<EffectR<?, ?, ?, ?>> monadR;
  private final MonadError<Kind<Kind<StateT<?, ?, ?>, EffectR<?, ?, ?, ?>>, S>, E> monadErrorS;
  private final MonadReader<Kind<Kind<StateT<?, ?, ?>, EffectR<?, ?, ?, ?>>, S>, R> monadReaderS;
  private final MonadState<Kind<Kind<StateT<?, ?, ?>, EffectR<?, ?, ?, ?>>, S>, S> monadStateS;

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
  public <A, B> EffectS<F, S, R, E, B> flatMap(Kind<EffectS<?, ?, ?, ?, ?>, ? extends A> value,
      Function1<? super A, ? extends Kind<EffectS<?, ?, ?, ?, ?>, ? extends B>> map) {
    return new EffectS<>(monadStateS.flatMap(
        value.fix(EffectS::<F, S, R, E, A>narrowK).value(),
        x -> map.apply(x).fix(EffectS::<F, S, R, E, B>narrowK).value()));
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
      Kind<EffectS<?, ?, ?, ?, ?>, A> value, Function1<? super E, ? extends Kind<EffectS<?, ?, ?, ?, ?>, ? extends A>> handler) {
    return new EffectS<>(monadErrorS.handleErrorWith(
        value.fix(EffectS::<F, S, R, E, A>narrowK).value(),
        error -> handler.apply(error).fix(EffectS::<F, S, R, E, A>narrowK).value()));
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

  public static final class EffectE<F, E, A> implements Kind<EffectE<?, ?, ?>, A> {

    private final EitherT<F, E, A> value;

    public EffectE(Kind<Kind<Kind<EitherT<?, ?, ?>, F>, E>, A> value) {
      this.value = value.fix(toEitherT());
    }

    public EitherT<F, E, A> value() {
      return value;
    }

    public Kind<F, Either<E, A>> run() {
      return value.value();
    }

    @SuppressWarnings("unchecked")
    public static <F, E, A> EffectE<F, E, A> narrowK(Kind<EffectE<?, ?, ?>, ? extends A> hkt) {
      return (EffectE<F, E, A>) hkt;
    }
  }

  public static final class EffectR<F, R, E, A> implements Kind<EffectR<?, ?, ?, ?>, A> {

    private final Kleisli<EffectE<?, ?, ?>, R, A> value;

    public EffectR(Kind<Kind<Kind<Kleisli<?, ?, ?>, EffectE<?, ?, ?>>, R>, A> value) {
      this.value = value.fix(toKleisli());
    }

    public Kleisli<EffectE<?, ?, ?>, R, A> value() {
      return value;
    }

    public EffectE<F, E, A> run(R config) {
      return value.run(config).fix(EffectE::narrowK);
    }

    @SuppressWarnings("unchecked")
    public static <F, R, E, A> EffectR<F, R, E, A> narrowK(Kind<EffectR<?, ?, ?, ?>, ? extends A> hkt) {
      return (EffectR<F, R, E, A>) hkt;
    }
  }

  public static final class EffectS<F, S, R, E, A> implements Kind<EffectS<?, ?, ?, ?, ?>, A> {

    private final StateT<EffectR<?, ?, ?, ?>, S, A> value;

    public EffectS(Kind<Kind<Kind<StateT<?, ?, ?>, EffectR<?, ?, ?, ?>>, S>, A> value) {
      this.value = value.fix(toStateT());
    }

    public StateT<EffectR<?, ?, ?, ?>, S, A> value() {
      return value;
    }

    public EffectR<F, R, E, Tuple2<S, A>> run(S state) {
      return value.run(state).fix(EffectR::narrowK);
    }

    @SuppressWarnings("unchecked")
    public static <F, S, R, E, A> EffectS<F, S, R, E, A> narrowK(Kind<EffectS<?, ?, ?, ?, ?>, ? extends A> hkt) {
      return (EffectS<F, S, R, E, A>) hkt;
    }
  }
}

class EffectEMonadError<F, E> implements MonadError<EffectE<?, ?, ?>, E> {

  private final MonadError<Kind<Kind<EitherT<?, ?, ?>, F>, E>, E> monad;

  public EffectEMonadError(Monad<F> monad) {
    this.monad = EitherTInstances.monadError(monad);
  }

  @Override
  public <A> EffectE<F, E, A> pure(A value) {
    return new EffectE<>(monad.pure(value));
  }

  @Override
  public <A, B> EffectE<F, E, B> flatMap(Kind<EffectE<?, ?, ?>, ? extends A> value,
      Function1<? super A, ? extends Kind<EffectE<?, ?, ?>, ? extends B>> map) {
    return new EffectE<>(monad.flatMap(
        value.fix(EffectE::<F, E, A>narrowK).value(),
        x -> map.apply(x).fix(EffectE::<F, E, B>narrowK).value()));
  }

  @Override
  public <A> EffectE<F, E, A> raiseError(E error) {
    return new EffectE<>(monad.raiseError(error));
  }

  @Override
  public <A> EffectE<F, E, A> handleErrorWith(Kind<EffectE<?, ?, ?>, A> value,
      Function1<? super E, ? extends Kind<EffectE<?, ?, ?>, ? extends A>> handler) {
    return new EffectE<>(monad.handleErrorWith(value.fix(EffectE::<F, E, A>narrowK).value(),
            error -> handler.apply(error).fix(EffectE::<F, E, A>narrowK).value()));
  }
}

class EffectRMonad<F, R, E> implements Monad<EffectR<?, ?, ?, ?>> {

  private final Monad<Kind<Kind<Kleisli<?, ?, ?>, EffectE<?, ?, ?>>, R>> monad;

  public EffectRMonad(Monad<F> monad) {
    this.monad = KleisliInstances.monad(new EffectEMonadError<F, E>(monad));
  }


  @Override
  public <A> EffectR<F, R, E, A> pure(A value) {
    return new EffectR<>(monad.pure(value));
  }

  @Override
  public <A, B> EffectR<F, R, E, B> flatMap(Kind<EffectR<?, ?, ?, ?>, ? extends A> value,
      Function1<? super A, ? extends Kind<EffectR<?, ?, ?, ?>, ? extends B>> map) {
    return new EffectR<>(monad.flatMap(value.fix(EffectR::<F, R, E, A>narrowK).value(),
        t -> map.apply(t).fix(EffectR::<F, R, E, B>narrowK).value()));
  }
}

class EffectRMonadReader<F, R, E> extends EffectRMonad<F, E, R> implements MonadReader<EffectR<?, ?, ?, ?>, R> {

  private final MonadReader<Kind<Kind<Kleisli<?, ?, ?>, EffectE<?, ?, ?>>, R>, R> monad;

  public EffectRMonadReader(Monad<F> monad) {
    super(monad);
    this.monad = KleisliInstances.monadReader(new EffectEMonadError<F, E>(monad));
  }

  @Override
  public EffectR<F, R, E, R> ask() {
    return new EffectR<>(monad.ask());
  }
}

class EffectRMonadError<F, R, E> extends EffectRMonad<F, R, E> implements MonadError<EffectR<?, ?, ?, ?>, E> {

  private final MonadError<Kind<Kind<Kleisli<?, ?, ?>, EffectE<?, ?, ?>>, R>, E> monadError;

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
      Kind<EffectR<?, ?, ?, ?>, A> value, Function1<? super E, ? extends Kind<EffectR<?, ?, ?, ?>, ? extends A>> handler) {
    return new EffectR<>(monadError.handleErrorWith(
        value.fix(EffectR::<F, R, E, A>narrowK).value(),
        error -> handler.apply(error).fix(EffectR::<F, R, E, A>narrowK).value()));
  }
}
