/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple5;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.instances.ConstInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.Const_;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Id_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Monoid;
import org.junit.jupiter.api.Test;

import static com.github.tonivade.purefun.Unit.unit;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FreeApTest {

  private final Applicative<Higher1<FreeAp_, DSL_>> applicative = FreeAp.applicativeF();

  @Test
  public void map() {
    FreeAp<DSL_, Integer> map = applicative.map(DSL.readInt(4), i -> i + 1).fix1(FreeAp_::narrowK);

    Id<Integer> foldMap = map.foldMap(idTransform(), IdInstances.applicative()).fix1(Id_::narrowK);

    assertEquals(Id.of(5), foldMap);
  }

  @Test
  public void ap() {
    FreeAp<DSL_, Integer> freeAp = FreeAp.lift(new ReadInt(123).kind1());
    FreeAp<DSL_, Function1<Integer, String>> apply = FreeAp.pure(Object::toString);

    Id<Integer> foldMap = freeAp.ap(apply)
        .map(String::length)
        .foldMap(idTransform(), IdInstances.applicative()).fix1(Id_::narrowK);

    assertEquals(Id.of(3), foldMap);
  }

  @Test
  public void lift() {
    FreeAp<DSL_, Tuple5<Integer, Boolean, Double, String, Unit>> tuple =
        applicative.map5(
            DSL.readInt(2),
            DSL.readBoolean(false),
            DSL.readDouble(2.1),
            DSL.readString("hola mundo"),
            DSL.readUnit(),
            Tuple::of
        ).fix1(FreeAp_::narrowK);

    Higher1<Id_, Tuple5<Integer, Boolean, Double, String, Unit>> map =
        tuple.foldMap(idTransform(), IdInstances.applicative());

    assertEquals(Id.of(Tuple.of(2, false, 2.1, "hola mundo", unit())), map.fix1(Id_::narrowK));
  }
  @Test
  public void pure() {
    FreeAp<DSL_, Tuple5<Integer, String, Double, Boolean, Unit>> tuple =
        applicative.map5(
            applicative.pure(1),
            applicative.pure("string"),
            applicative.pure(1.1),
            applicative.pure(true),
            applicative.pure(unit()),
            Tuple::of
        ).fix1(FreeAp_::narrowK);

    Higher1<Id_, Tuple5<Integer, String, Double, Boolean, Unit>> map =
        tuple.foldMap(idTransform(), IdInstances.applicative());

    assertEquals(Id.of(Tuple.of(1, "string", 1.1, true, unit())), map.fix1(Id_::narrowK));
  }

  @Test
  public void compile() {
    FreeAp<DSL_, Integer> readInt = FreeAp.pure(5);

    FreeAp<Id_, Integer> compile = readInt.compile(idTransform());
    Id<Integer> fold = compile.fold(IdInstances.applicative()).fix1(Id_::narrowK);

    assertEquals(5, fold.get());
  }

  @Test
  public void analyze() {
    FreeAp<DSL_, Tuple5<Integer, Boolean, Double, String, Unit>> tuple =
        applicative.map5(
            DSL.readInt(2),
            DSL.readBoolean(false),
            DSL.readDouble(2.1),
            DSL.readString("hola mundo"),
            DSL.readUnit(),
            Tuple::of
        ).fix1(FreeAp_::narrowK);

    String analize = tuple.analyze(constTransform(), ConstInstances.applicative(Monoid.string()));

    assertEquals("ReadInt(2)\n" +
        "ReadBoolean(false)\n" +
        "ReadDouble(2.1)\n" +
        "ReadString(hola mundo)\n" +
        "ReadUnit(Unit)\n", analize);
  }

  private FunctionK<DSL_, Id_> idTransform() {
    return new FunctionK<DSL_, Id_>() {
      @Override
      public <T> Higher1<Id_, T> apply(Higher1<DSL_, T> from) {
        DSL<T> dsl = from.fix1(DSL_::narrowK);
        return Id.of(dsl.value()).kind1();
      }
    };
  }

  private FunctionK<DSL_, Higher1<Const_, String>> constTransform() {
    return new FunctionK<DSL_, Higher1<Const_, String>>() {
      @Override
      public <T> Higher2<Const_, String, T> apply(Higher1<DSL_, T> from) {
        DSL<T> dsl = from.fix1(DSL_::narrowK);
        return Const.<String, T>of(dsl.getClass().getSimpleName() + "(" + dsl.value() + ")\n").kind2();
      }
    };
  }
}

@HigherKind
interface DSL<A> extends Higher1<DSL_, A> {

  A value();

  static Higher2<FreeAp_, DSL_, Integer> readInt(int value) {
    return FreeAp.lift(new ReadInt(value).kind1()).kind2();
  }

  static Higher2<FreeAp_, DSL_, String> readString(String value) {
    return FreeAp.lift(new ReadString(value).kind1()).kind2();
  }

  static Higher2<FreeAp_, DSL_, Boolean> readBoolean(boolean value) {
    return FreeAp.lift(new ReadBoolean(value).kind1()).kind2();
  }

  static Higher2<FreeAp_, DSL_, Double> readDouble(double value) {
    return FreeAp.lift(new ReadDouble(value).kind1()).kind2();
  }

  static Higher2<FreeAp_, DSL_, Unit> readUnit() {
    return FreeAp.lift(new ReadUnit().kind1()).kind2();
  }
}

final class ReadInt implements DSL<Integer> {
  private final int value;

  ReadInt(int value) {
    this.value = value;
  }

  @Override
  public Integer value() {
    return value;
  }
}

final class ReadString implements DSL<String> {

  private final String value;

  ReadString(String value) {
    this.value = value;
  }

  @Override
  public String value() {
    return value;
  }
}

final class ReadBoolean implements DSL<Boolean> {

  private final boolean value;

  ReadBoolean(boolean value) {
    this.value = value;
  }

  @Override
  public Boolean value() {
    return value;
  }
}

final class ReadDouble implements DSL<Double> {

  private final double value;

  ReadDouble(double value) {
    this.value = value;
  }

  @Override
  public Double value() {
    return value;
  }
}

final class ReadUnit implements DSL<Unit> {
  @Override
  public Unit value() {
    return unit();
  }
}
