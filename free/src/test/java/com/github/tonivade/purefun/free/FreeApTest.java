/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.free.FreeApOf.toFreeAp;
import static com.github.tonivade.purefun.type.IdOf.toId;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple5;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.annotation.HigherKind;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.instances.ConstInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.Const_;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Id_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Monoid;

public class FreeApTest {

  private final Applicative<Kind<FreeAp_, DSL_>> applicative = FreeAp.applicativeF();

  @Test
  public void map() {
    FreeAp<DSL_, Integer> map = applicative.map(DSL.readInt(4), i -> i + 1).fix(toFreeAp());

    Id<Integer> foldMap = map.foldMap(idTransform(), IdInstances.applicative()).fix(toId());

    assertEquals(Id.of(5), foldMap);
  }

  @Test
  public void ap() {
    FreeAp<DSL_, Integer> freeAp = FreeAp.lift(new ReadInt(123));
    FreeAp<DSL_, Function1<? super Integer, ? extends String>> apply = FreeAp.pure(Object::toString);

    Id<Integer> foldMap = freeAp.ap(apply)
        .map(String::length)
        .foldMap(idTransform(), IdInstances.applicative()).fix(toId());

    assertEquals(Id.of(3), foldMap);
  }

  @Test
  public void lift() {
    FreeAp<DSL_, Tuple5<Integer, Boolean, Double, String, Unit>> tuple =
        applicative.mapN(
            DSL.readInt(2),
            DSL.readBoolean(false),
            DSL.readDouble(2.1),
            DSL.readString("hola mundo"),
            DSL.readUnit(),
            Tuple::of
        ).fix(toFreeAp());

    Kind<Id_, Tuple5<Integer, Boolean, Double, String, Unit>> map =
        tuple.foldMap(idTransform(), IdInstances.applicative());

    assertEquals(Id.of(Tuple.of(2, false, 2.1, "hola mundo", unit())), map.fix(toId()));
  }
  @Test
  public void pure() {
    FreeAp<DSL_, Tuple5<Integer, String, Double, Boolean, Unit>> tuple =
        applicative.mapN(
            applicative.pure(1),
            applicative.pure("string"),
            applicative.pure(1.1),
            applicative.pure(true),
            applicative.pure(unit()),
            Tuple::of
        ).fix(toFreeAp());

    Kind<Id_, Tuple5<Integer, String, Double, Boolean, Unit>> map =
        tuple.foldMap(idTransform(), IdInstances.applicative());

    assertEquals(Id.of(Tuple.of(1, "string", 1.1, true, unit())), map.fix(toId()));
  }

  @Test
  public void compile() {
    FreeAp<DSL_, Integer> readInt = FreeAp.pure(5);

    FreeAp<Id_, Integer> compile = readInt.compile(idTransform());
    Id<Integer> fold = compile.fold(IdInstances.applicative()).fix(toId());

    assertEquals(5, fold.value());
  }

  @Test
  public void analyze() {
    FreeAp<DSL_, Tuple5<Integer, Boolean, Double, String, Unit>> tuple =
        applicative.mapN(
            DSL.readInt(2),
            DSL.readBoolean(false),
            DSL.readDouble(2.1),
            DSL.readString("hola mundo"),
            DSL.readUnit(),
            Tuple::of
        ).fix(toFreeAp());

    String analize = tuple.analyze(constTransform(), ConstInstances.applicative(Monoid.string()));

    assertEquals("""
        ReadInt(2)
        ReadBoolean(false)
        ReadDouble(2.1)
        ReadString(hola mundo)
        ReadUnit(Unit)
        """, analize);
  }

  private FunctionK<DSL_, Id_> idTransform() {
    return new FunctionK<>() {
      @Override
      public <T> Kind<Id_, T> apply(Kind<DSL_, ? extends T> from) {
        return Id.of(from.fix(DSLOf::<T>narrowK).value());
      }
    };
  }

  private FunctionK<DSL_, Kind<Const_, String>> constTransform() {
    return new FunctionK<>() {
      @Override
      public <T> Const<String, T> apply(Kind<DSL_, ? extends T> from) {
        DSL<T> dsl = from.fix(DSLOf::narrowK);
        return Const.of(dsl.getClass().getSimpleName() + "(" + dsl.value() + ")\n");
      }
    };
  }
}

@HigherKind
sealed interface DSL<A> extends DSLOf<A> {

  A value();

  static FreeAp<DSL_, Integer> readInt(int value) {
    return FreeAp.lift(new ReadInt(value));
  }

  static FreeAp<DSL_, String> readString(String value) {
    return FreeAp.lift(new ReadString(value));
  }

  static FreeAp<DSL_, Boolean> readBoolean(boolean value) {
    return FreeAp.lift(new ReadBoolean(value));
  }

  static FreeAp<DSL_, Double> readDouble(double value) {
    return FreeAp.lift(new ReadDouble(value));
  }

  static FreeAp<DSL_, Unit> readUnit() {
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
