/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.core.Unit.unit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple5;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.instances.ConstInstances;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.IdOf;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Instances;
import com.github.tonivade.purefun.typeclasses.Monoid;
import org.junit.jupiter.api.Test;

public class FreeApTest {

  private final Applicative<FreeAp<DSL<?>, ?>> applicative = FreeAp.applicativeF();

  @Test
  public void map() {
    FreeAp<DSL<?>, Integer> map = applicative.map(DSL.readInt(4), i -> i + 1).fix(FreeApOf::toFreeAp);

    Id<Integer> foldMap = map.foldMap(idTransform(), Instances.applicative()).fix(IdOf::toId);

    assertEquals(Id.of(5), foldMap);
  }

  @Test
  public void ap() {
    FreeAp<DSL<?>, Integer> freeAp = FreeAp.lift(new ReadInt(123));
    FreeAp<DSL<?>, Function1<? super Integer, ? extends String>> apply = FreeAp.pure(Object::toString);

    Id<Integer> foldMap = freeAp.ap(apply)
        .map(String::length)
        .foldMap(idTransform(), Instances.applicative()).fix(IdOf::toId);

    assertEquals(Id.of(3), foldMap);
  }

  @Test
  public void lift() {
    FreeAp<DSL<?>, Tuple5<Integer, Boolean, Double, String, Unit>> tuple =
        applicative.mapN(
            DSL.readInt(2),
            DSL.readBoolean(false),
            DSL.readDouble(2.1),
            DSL.readString("hola mundo"),
            DSL.readUnit(),
            Tuple::of
        ).fix(FreeApOf::toFreeAp);

    Kind<Id<?>, Tuple5<Integer, Boolean, Double, String, Unit>> map =
        tuple.foldMap(idTransform(), Instances.applicative());

    assertEquals(Id.of(Tuple.of(2, false, 2.1, "hola mundo", unit())), map.fix(IdOf::toId));
  }
  @Test
  public void pure() {
    FreeAp<DSL<?>, Tuple5<Integer, String, Double, Boolean, Unit>> tuple =
        applicative.mapN(
            applicative.pure(1),
            applicative.pure("string"),
            applicative.pure(1.1),
            applicative.pure(true),
            applicative.pure(unit()),
            Tuple::of
        ).fix(FreeApOf::toFreeAp);

    Kind<Id<?>, Tuple5<Integer, String, Double, Boolean, Unit>> map =
        tuple.foldMap(idTransform(), Instances.applicative());

    assertEquals(Id.of(Tuple.of(1, "string", 1.1, true, unit())), map.fix(IdOf::toId));
  }

  @Test
  public void compile() {
    FreeAp<DSL<?>, Integer> readInt = FreeAp.pure(5);

    FreeAp<Id<?>, Integer> compile = readInt.compile(idTransform());
    Id<Integer> fold = compile.fold(Instances.applicative()).fix(IdOf::toId);

    assertEquals(5, fold.value());
  }

  @Test
  public void analyze() {
    FreeAp<DSL<?>, Tuple5<Integer, Boolean, Double, String, Unit>> tuple =
        applicative.mapN(
            DSL.readInt(2),
            DSL.readBoolean(false),
            DSL.readDouble(2.1),
            DSL.readString("hola mundo"),
            DSL.readUnit(),
            Tuple::of
        ).fix(FreeApOf::toFreeAp);

    String analize = tuple.analyze(constTransform(), ConstInstances.applicative(Monoid.string()));

    assertEquals("""
        ReadInt(2)
        ReadBoolean(false)
        ReadDouble(2.1)
        ReadString(hola mundo)
        ReadUnit(Unit)
        """, analize);
  }

  private FunctionK<DSL<?>, Id<?>> idTransform() {
    return new FunctionK<>() {
      @Override
      public <T> Kind<Id<?>, T> apply(Kind<DSL<?>, ? extends T> from) {
        return Id.of(from.fix(DSLOf::toDSL).value());
      }
    };
  }

  private FunctionK<DSL<?>, Const<String, ?>> constTransform() {
    return new FunctionK<>() {
      @Override
      public <T> Const<String, T> apply(Kind<DSL<?>, ? extends T> from) {
        Kind<DSL<?>, T> narrowK = Kind.narrowK(from);
        DSL<T> dsl = narrowK.fix(DSLOf::toDSL);
        return Const.of(dsl.getClass().getSimpleName() + "(" + dsl.value() + ")\n");
      }
    };
  }
}

@HigherKind
sealed interface DSL<A> extends DSLOf<A> {

  A value();

  static FreeAp<DSL<?>, Integer> readInt(int value) {
    return FreeAp.lift(new ReadInt(value));
  }

  static FreeAp<DSL<?>, String> readString(String value) {
    return FreeAp.lift(new ReadString(value));
  }

  static FreeAp<DSL<?>, Boolean> readBoolean(boolean value) {
    return FreeAp.lift(new ReadBoolean(value));
  }

  static FreeAp<DSL<?>, Double> readDouble(double value) {
    return FreeAp.lift(new ReadDouble(value));
  }

  static FreeAp<DSL<?>, Unit> readUnit() {
    return FreeAp.lift(new ReadUnit());
  }
}

record ReadInt(Integer value) implements DSL<Integer> { }

record ReadString(String value) implements DSL<String> { }

record ReadBoolean(Boolean value) implements DSL<Boolean> { }

record ReadDouble(Double value) implements DSL<Double> { }

final class ReadUnit implements DSL<Unit> {
  @Override
  public Unit value() {
    return unit();
  }
}
