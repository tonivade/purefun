/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.FutureOf;
import com.github.tonivade.purefun.concurrent.Future_;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionTInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Id_;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryOf;
import com.github.tonivade.purefun.type.Try_;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;

public class OptionTTest {

  private final Monad<Id_> monad = IdInstances.monad();

  @Test
  public void map() {
    OptionT<Id_, String> some = OptionT.some(monad, "abc");

    OptionT<Id_, String> map = some.map(String::toUpperCase);

    assertEquals(Id.of("ABC"), map.get());
  }

  @Test
  public void flatMap() {
    OptionT<Id_, String> some = OptionT.some(monad, "abc");

    OptionT<Id_, String> map = some.flatMap(value -> OptionT.some(monad, value.toUpperCase()));

    assertEquals(Id.of("ABC"), map.get());
  }

  @Test
  public void filter() {
    OptionT<Id_, String> some = OptionT.some(monad, "abc");

    OptionT<Id_, String> filter = some.filter(String::isEmpty);
    OptionT<Id_, String> orElse = OptionT.some(monad, "not empty");

    assertEquals(orElse.get(), filter.getOrElse("not empty"));
  }

  @Test
  public void none() {
    OptionT<Id_, String> none = OptionT.none(monad);

    assertAll(
        () -> assertEquals(Id.of(true), none.isEmpty()),
        () -> assertEquals(Id.of("empty"), none.getOrElse("empty")));
  }

  @Test
  public void some() {
    OptionT<Id_, String> some = OptionT.some(monad, "abc");

    assertAll(
        () -> assertEquals(Id.of(false), some.isEmpty()),
        () -> assertEquals(Id.of("abc"), some.getOrElse("empty")));
  }

  @Test
  public void mapK() {
    OptionT<IO_, String> someIo = OptionT.some(IOInstances.monad(), "abc");

    OptionT<Try_, String> someTry = someIo.mapK(TryInstances.monad(), new IOToTryFunctionK());

    assertEquals(Try.success("abc"), TryOf.narrowK(someTry.get()));
  }

  @Test
  public void eq() {
    OptionT<Id_, String> some1 = OptionT.some(monad, "abc");
    OptionT<Id_, String> some2 = OptionT.some(monad, "abc");
    OptionT<Id_, String> none1 = OptionT.none(monad);
    OptionT<Id_, String> none2 = OptionT.none(monad);

    Eq<Kind<Kind<OptionT_, Id_>, String>> instance = OptionTInstances.eq(IdInstances.eq(Eq.any()));

    assertAll(
        () -> assertTrue(instance.eqv(some1, some2)),
        () -> assertTrue(instance.eqv(none1, none2)),
        () -> assertFalse(instance.eqv(some1, none1)),
        () -> assertFalse(instance.eqv(none2, some2)));
  }

  @Test
  public void monadErrorFuture() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Kind<OptionT_, Future_>, Throwable> monadError =
        OptionTInstances.monadError(FutureInstances.monadError());

    Kind<Kind<OptionT_, Future_>, String> pure = monadError.pure("is not ok");
    Kind<Kind<OptionT_, Future_>, String> raiseError = monadError.raiseError(error);
    Kind<Kind<OptionT_, Future_>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Kind<Kind<OptionT_, Future_>, String> ensureOk =
        monadError.ensure(pure, () -> error, value -> "is not ok".equals(value));
    Kind<Kind<OptionT_, Future_>, String> ensureError =
        monadError.ensure(pure, () -> error, value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Try.failure(error), FutureOf.narrowK(OptionTOf.narrowK(raiseError).value()).await()),
        () -> assertEquals(Try.success(Option.some("not an error")), FutureOf.narrowK(OptionTOf.narrowK(handleError).value()).await()),
        () -> assertEquals(Try.failure(error), FutureOf.narrowK(OptionTOf.narrowK(ensureError).value()).await()),
        () -> assertEquals(Try.success(Option.some("is not ok")), FutureOf.narrowK(OptionTOf.narrowK(ensureOk).value()).await()));
  }

  @Test
  public void monadErrorIO() {
    MonadError<Kind<OptionT_, Id_>, Unit> monadError = OptionTInstances.monadError(monad);

    Kind<Kind<OptionT_, Id_>, String> pure = monadError.pure("is not ok");
    Kind<Kind<OptionT_, Id_>, String> raiseError = monadError.raiseError(unit());
    Kind<Kind<OptionT_, Id_>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Kind<Kind<OptionT_, Id_>, String> ensureOk =
        monadError.ensure(pure, Unit::unit, "is not ok"::equals);
    Kind<Kind<OptionT_, Id_>, String> ensureError =
        monadError.ensure(pure, Unit::unit, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Id.of(Option.none()), OptionTOf.narrowK(raiseError).value()),
        () -> assertEquals(Id.of(Option.some("not an error")), OptionTOf.narrowK(handleError).value()),
        () -> assertEquals(Id.of(Option.none()), OptionTOf.narrowK(ensureError).value()),
        () -> assertEquals(Id.of(Option.some("is not ok")), OptionTOf.narrowK(ensureOk).value()));
  }
}

class IOToTryFunctionK implements FunctionK<IO_, Try_> {

  @Override
  public <T> Kind<Try_, T> apply(Kind<IO_, ? extends T> from) {
    return Try.of(from.fix(toIO())::unsafeRunSync);
  }
}
