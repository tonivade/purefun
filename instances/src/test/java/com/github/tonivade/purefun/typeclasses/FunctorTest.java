/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.laws.FunctorLaws.verifyLaws;
import static com.github.tonivade.purefun.typeclasses.Nested.nest;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.ConstInstances;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.SequenceInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.instances.TupleInstances;
import com.github.tonivade.purefun.instances.ValidationInstances;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.Validation;

public class FunctorTest {

  @Test
  public void idFunctor() {
    verifyLaws(IdInstances.functor(), Id.of("hola mundo!"));
  }

  @Test
  public void tupleFunctor() {
    verifyLaws(TupleInstances.functor(), Tuple.of("hola mundo!"));
  }

  @Test
  public void optionFunctor() {
    verifyLaws(OptionInstances.functor(), Option.some("hola mundo!"));
  }

  @Test
  public void tryFunctor() {
    verifyLaws(TryInstances.functor(), Try.success("hola mundo!"));
  }

  @Test
  public void eitherFunctor() {
    verifyLaws(EitherInstances.functor(), Either.right("hola mundo!"));
  }

  @Test
  public void validationFunctor() {
    verifyLaws(ValidationInstances.functor(), Validation.valid("hola mundo!"));
  }

  @Test
  public void traverseFunctor() {
    verifyLaws(SequenceInstances.traverse(), Sequence.listOf("hola mundo!"));
  }

  @Test
  public void composedCovariantFunctor() {
    verifyLaws(Functor.compose(OptionInstances.functor(), IdInstances.functor()), nest(Option.some(Id.of("hola mundo!"))));
  }

  @Test
  public void composedContravariantFunctor() {
    verifyLaws(Functor.compose(ConstInstances.contravariant(), ConstInstances.contravariant()), nest(Const.of(Const.of("hola mundo!"))));
  }
}
