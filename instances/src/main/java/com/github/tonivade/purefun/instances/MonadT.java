/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.instances.MonadT.Effect0;
import com.github.tonivade.purefun.instances.MonadT.Effect0_;
import com.github.tonivade.purefun.instances.MonadT.Effect1;
import com.github.tonivade.purefun.instances.MonadT.Effect1_;
import com.github.tonivade.purefun.instances.MonadT.EffectN_;
import com.github.tonivade.purefun.transformer.EitherT;
import com.github.tonivade.purefun.transformer.EitherTOf;
import com.github.tonivade.purefun.transformer.EitherT_;
import com.github.tonivade.purefun.transformer.Kleisli;
import com.github.tonivade.purefun.transformer.KleisliOf;
import com.github.tonivade.purefun.transformer.Kleisli_;
import com.github.tonivade.purefun.transformer.StateT;
import com.github.tonivade.purefun.transformer.StateTOf;
import com.github.tonivade.purefun.transformer.StateT_;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadReader;
import com.github.tonivade.purefun.typeclasses.MonadState;

public class MonadT<F extends Witness, S, R, E> 
    implements Monad<EffectN_>, MonadError<EffectN_, E>, MonadState<EffectN_, S>, MonadReader<EffectN_, R> {

  private final Monad<F> monad;
  private final MonadError<Effect0_, E> monadError0;
  private final Monad<Effect1_> monad1;
  private final MonadError<Kind<Kind<StateT_, Effect1_>, S>, E> monadErrorN;
  private final MonadReader<Kind<Kind<StateT_, Effect1_>, S>, R> monadReaderN;
  private final MonadState<Kind<Kind<StateT_, Effect1_>, S>, S> monadStateN;
      
  public MonadT(Monad<F> monad) {
    this.monad = checkNonNull(monad);
    this.monadError0 = new Effect0MonadError<F, E>(monad);
    this.monad1 = new Effect1Monad<F, R, E>(monad);
    this.monadErrorN = StateTInstances.monadError(new Effect1MonadError<F, R, E>(monad));
    this.monadReaderN = StateTInstances.monadReader(new Effect1MonadReader<F, R, E>(monad));
    this.monadStateN = StateTInstances.monadState(monad1);
  }

  @Override
  public <A> EffectN<F, S, R, E, A> pure(A value) {
    return new EffectN<>(monadStateN.pure(value).fix(StateTOf.toStateT()));
  }

  @Override
  public <A, B> EffectN<F, S, R, E, B> flatMap(Kind<EffectN_, ? extends A> value, 
      Function1<? super A, ? extends Kind<EffectN_, ? extends B>> map) {
    Kind<Kind<Kind<StateT_, Effect1_>, S>, B> flatMap = monadStateN.flatMap(
        value.fix(EffectN::<F, S, R, E, A>narrowK).value(), 
        x -> map.apply(x).fix(EffectN::<F, S, R, E, B>narrowK).value());
    return new EffectN<>(flatMap.fix(StateTOf::narrowK));
  }
  
  @Override
  public EffectN<F, S, R, E, S> get() {
    return new EffectN<>(monadStateN.get().fix(StateTOf.toStateT()));
  }
  
  @Override
  public EffectN<F, S, R, E, Unit> set(S state) {
    return new EffectN<>(monadStateN.set(state).fix(StateTOf.toStateT()));
  }
  
  @Override
  public <A> EffectN<F, S, R, E, A> raiseError(E error) {
    return new EffectN<>(monadErrorN.<A>raiseError(error).fix(StateTOf.toStateT()));
  }
  
  @Override
  public <A> EffectN<F, S, R, E, A> handleErrorWith(
      Kind<EffectN_, A> value, Function1<? super E, ? extends Kind<EffectN_, ? extends A>> handler) {
    Kind<Kind<Kind<StateT_, Effect1_>, S>, A> handleErrorWith = monadErrorN.handleErrorWith(
        value.fix(EffectN::<F, S, R, E, A>narrowK).value(), 
        error -> handler.apply(error).fix(EffectN::<F, S, R, E, A>narrowK).value());
    return new EffectN<>(handleErrorWith.fix(StateTOf.toStateT()));
  }
  
  @Override
  public EffectN<F, S, R, E, R> ask() {
    return new EffectN<>(monadReaderN.ask().fix(StateTOf.toStateT()));
  }

  public <A> Effect0<F, E, A> effect0(Kind<F, Either<E, A>> value) {
    return new Effect0<>(EitherT.of(monad, value));
  }

  public <A> Effect1<F, R, E, A> effect1(Effect0<F, E, A> effect0) {
    return new Effect1<>(Kleisli.<Effect0_, R, A>of(monadError0, config -> effect0));
  }

  public <A> EffectN<F, S, R, E, A> effectN(Effect1<F, R, E, A> effect1) {
    return new EffectN<>(StateT.<Effect1_, S, A>state(monad1, state -> monad1.map(effect1, x -> Tuple.of(state, x))));
  }

  public static final class Effect0_ implements Witness { }
  public static final class Effect1_ implements Witness { }
  public static final class EffectN_ implements Witness { }

  public static final class Effect0<F extends Witness, E, A> implements Kind<Effect0_, A> {
    
    private final EitherT<F, E, A> value;
    
    public Effect0(EitherT<F, E, A> value) {
      this.value = value;
    }
    
    public EitherT<F, E, A> value() {
      return value;
    }

    public Kind<F, Either<E, A>> run() {
      return value.value();
    }
    
    @SuppressWarnings("unchecked")
    public static <F extends Witness, E, A> Effect0<F, E, A> narrowK(Kind<Effect0_, ? extends A> hkt) {
      return (Effect0<F, E, A>) hkt;
    }
  }

  public static final class Effect1<F extends Witness, R, E, A> implements Kind<Effect1_, A> {
    
    private final Kleisli<Effect0_, R, A> value;
    
    public Effect1(Kleisli<Effect0_, R, A> value) {
      this.value = value;
    }
    
    public Kleisli<Effect0_, R, A> value() {
      return value;
    }

    public Effect0<F, E, A> run(R config) {
      return value.run(config).fix(Effect0::narrowK);
    }
    
    @SuppressWarnings("unchecked")
    public static <F extends Witness, R, E, A> Effect1<F, R, E, A> narrowK(Kind<Effect1_, ? extends A> hkt) {
      return (Effect1<F, R, E, A>) hkt;
    }
  }

  public static final class EffectN<F extends Witness, S, R, E, A> implements Kind<EffectN_, A> {
    
    private final StateT<Effect1_, S, A> value;
    
    public EffectN(StateT<Effect1_, S, A> value) {
      this.value = value;
    }
    
    public StateT<Effect1_, S, A> value() {
      return value;
    }

    public Effect1<F, R, E, Tuple2<S, A>> run(S state) {
      return value.run(state).fix(Effect1::narrowK);
    }
    
    @SuppressWarnings("unchecked")
    public static <F extends Witness, S, R, E, A> EffectN<F, S, R, E, A> narrowK(Kind<EffectN_, ? extends A> hkt) {
      return (EffectN<F, S, R, E, A>) hkt;
    }
  }
}

class Effect0MonadError<F extends Witness, E> implements MonadError<Effect0_, E> {

  private final MonadError<Kind<Kind<EitherT_, F>, E>, E> monad;

  public Effect0MonadError(Monad<F> monad) {
    this.monad = EitherTInstances.monadError(monad);
  }

  @Override
  public <A> Effect0<F, E, A> pure(A value) {
    return new Effect0<>(monad.pure(value).fix(EitherTOf.toEitherT()));
  }

  @Override
  public <A, B> Effect0<F, E, B> flatMap(Kind<Effect0_, ? extends A> value, 
      Function1<? super A, ? extends Kind<Effect0_, ? extends B>> map) {
    Kind<Kind<Kind<EitherT_, F>, E>, ? extends B> flatMap = 
        monad.flatMap(
            value.fix(Effect0::<F, E, A>narrowK).value(), 
            x -> map.apply(x).fix(Effect0::<F, E, B>narrowK).value());
    return new Effect0<>(flatMap.fix(EitherTOf::narrowK));
  }
  
  @Override
  public <A> Effect0<F, E, A> raiseError(E error) {
    return new Effect0<>(monad.<A>raiseError(error).fix(EitherTOf.toEitherT()));
  }
  
  @Override
  public <A> Effect0<F, E, A> handleErrorWith(Kind<Effect0_, A> value,
      Function1<? super E, ? extends Kind<Effect0_, ? extends A>> handler) {
    Kind<Kind<Kind<EitherT_, F>, E>, A> handleErrorWith = monad.handleErrorWith(value.fix(Effect0::<F, E, A>narrowK).value(), 
            error -> handler.apply(error).fix(Effect0::<F, E, A>narrowK).value());
    return new Effect0<>(handleErrorWith.fix(EitherTOf.toEitherT()));
  }
}

class Effect1Monad<F extends Witness, R, E> implements Monad<Effect1_> {

  private final Monad<Kind<Kind<Kleisli_, Effect0_>, R>> monad;

  public Effect1Monad(Monad<F> monad) {
    this.monad = KleisliInstances.monad(new Effect0MonadError<F, E>(monad));
  }
  
      
  @Override
  public <A> Effect1<F, R, E, A> pure(A value) {
    return new Effect1<>(monad.pure(value).fix(KleisliOf.toKleisli()));
  }

  @Override
  public <A, B> Effect1<F, R, E, B> flatMap(Kind<Effect1_, ? extends A> value, 
      Function1<? super A, ? extends Kind<Effect1_, ? extends B>> map) {
    Kind<Kind<Kind<Kleisli_, Effect0_>, R>, B> flatMap = monad.flatMap(value.fix(Effect1::<F, R, E, A>narrowK).value(), 
        t -> map.apply(t).fix(Effect1::<F, R, E, B>narrowK).value());
    return new Effect1<>(flatMap.fix(KleisliOf::narrowK));
  }
}

class Effect1MonadReader<F extends Witness, R, E> extends Effect1Monad<F, E, R> implements MonadReader<Effect1_, R> {

  private final MonadReader<Kind<Kind<Kleisli_, Effect0_>, R>, R> monad;
      
  public Effect1MonadReader(Monad<F> monad) {
    super(monad);
    this.monad = KleisliInstances.monadReader(new Effect0MonadError<F, E>(monad));
  }

  @Override
  public Effect1<F, R, E, R> ask() {
    return new Effect1<>(monad.ask().fix(KleisliOf.toKleisli()));
  }
}

class Effect1MonadError<F extends Witness, R, E> extends Effect1Monad<F, R, E> implements MonadError<Effect1_, E> {

  private final MonadError<Kind<Kind<Kleisli_, Effect0_>, R>, E> monadError;
      
  public Effect1MonadError(Monad<F> monad) {
    super(monad);
    this.monadError = KleisliInstances.monadError(new Effect0MonadError<F, E>(monad));
  }
  
  @Override
  public <A> Effect1<F, R, E, A> raiseError(E error) {
    return new Effect1<>(monadError.<A>raiseError(error).fix(KleisliOf.toKleisli()));
  }
  
  @Override
  public <A> Effect1<F, R, E, A> handleErrorWith(
      Kind<Effect1_, A> value, Function1<? super E, ? extends Kind<Effect1_, ? extends A>> handler) {
    Kind<Kind<Kind<Kleisli_, Effect0_>, R>, A> handleErrorWith = monadError.handleErrorWith(
        value.fix(Effect1::<F, R, E, A>narrowK).value(),  
        error -> handler.apply(error).fix(Effect1::<F, R, E, A>narrowK).value());
    return new Effect1<>(handleErrorWith.fix(KleisliOf.toKleisli()));
  }
}
