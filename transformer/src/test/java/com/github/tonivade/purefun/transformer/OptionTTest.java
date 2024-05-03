/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static com.github.tonivade.purefun.core.Unit.unit;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.FutureOf;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.core.Eq;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionTInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryOf;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;

public class OptionTTest {

  private final Monad<Id<?>> monad = IdInstances.monad();

  @Test
  public void map() {
    OptionT<Id<?>, String> some = OptionT.some(monad, "abc");

    OptionT<Id<?>, String> map = some.map(String::toUpperCase);

    assertEquals(Id.of("ABC"), map.getOrElseThrow());
  }

  @Test
  public void flatMap() {
    OptionT<Id<?>, String> some = OptionT.some(monad, "abc");

    OptionT<Id<?>, String> map = some.flatMap(value -> OptionT.some(monad, value.toUpperCase()));

    assertEquals(Id.of("ABC"), map.getOrElseThrow());
  }

  @Test
  public void filter() {
    OptionT<Id<?>, String> some = OptionT.some(monad, "abc");

    OptionT<Id<?>, String> filter = some.filter(String::isEmpty);
    OptionT<Id<?>, String> orElse = OptionT.some(monad, "not empty");

    assertEquals(orElse.getOrElseThrow(), filter.getOrElse("not empty"));
  }

  @Test
  public void none() {
    OptionT<Id<?>, String> none = OptionT.none(monad);

    assertAll(
        () -> assertEquals(Id.of(true), none.isEmpty()),
        () -> assertEquals(Id.of("empty"), none.getOrElse("empty")));
  }

  @Test
  public void some() {
    OptionT<Id<?>, String> some = OptionT.some(monad, "abc");

    assertAll(
        () -> assertEquals(Id.of(false), some.isEmpty()),
        () -> assertEquals(Id.of("abc"), some.getOrElse("empty")));
  }

  @Test
  public void mapK() {
    OptionT<IO<?>, String> someIo = OptionT.some(IOInstances.monad(), "abc");

    OptionT<Try<?>, String> someTry = someIo.mapK(TryInstances.monad(), new IOToTryFunctionK());

    assertEquals(Try.success("abc"), TryOf.narrowK(someTry.getOrElseThrow()));
  }

  @Test
  public void eq() {
    OptionT<Id<?>, String> some1 = OptionT.some(monad, "abc");
    OptionT<Id<?>, String> some2 = OptionT.some(monad, "abc");
    OptionT<Id<?>, String> none1 = OptionT.none(monad);
    OptionT<Id<?>, String> none2 = OptionT.none(monad);

    Eq<Kind<Kind<OptionT<?, ?>, Id<?>>, String>> instance = OptionTInstances.eq(IdInstances.eq(Eq.any()));

    assertAll(
        () -> assertTrue(instance.eqv(some1, some2)),
        () -> assertTrue(instance.eqv(none1, none2)),
        () -> assertFalse(instance.eqv(some1, none1)),
        () -> assertFalse(instance.eqv(none2, some2)));
  }

  @Test
  public void monadErrorFuture() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Kind<OptionT<?, ?>, Future<?>>, Throwable> monadError =
        OptionTInstances.monadError(FutureInstances.monadError());

    Kind<Kind<OptionT<?, ?>, Future<?>>, String> pure = monadError.pure("is not ok");
    Kind<Kind<OptionT<?, ?>, Future<?>>, String> raiseError = monadError.raiseError(error);
    Kind<Kind<OptionT<?, ?>, Future<?>>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Kind<Kind<OptionT<?, ?>, Future<?>>, String> ensureOk =
        monadError.ensure(pure, () -> error, value -> "is not ok".equals(value));
    Kind<Kind<OptionT<?, ?>, Future<?>>, String> ensureError =
        monadError.ensure(pure, () -> error, value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Try.failure(error), FutureOf.narrowK(OptionTOf.narrowK(raiseError).value()).await()),
        () -> assertEquals(Try.success(Option.some("not an error")), FutureOf.narrowK(OptionTOf.narrowK(handleError).value()).await()),
        () -> assertEquals(Try.failure(error), FutureOf.narrowK(OptionTOf.narrowK(ensureError).value()).await()),
        () -> assertEquals(Try.success(Option.some("is not ok")), FutureOf.narrowK(OptionTOf.narrowK(ensureOk).value()).await()));
  }

  @Test
  public void monadErrorIO() {
    MonadError<Kind<OptionT<?, ?>, Id<?>>, Unit> monadError = OptionTInstances.monadError(monad);

    Kind<Kind<OptionT<?, ?>, Id<?>>, String> pure = monadError.pure("is not ok");
    Kind<Kind<OptionT<?, ?>, Id<?>>, String> raiseError = monadError.raiseError(unit());
    Kind<Kind<OptionT<?, ?>, Id<?>>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Kind<Kind<OptionT<?, ?>, Id<?>>, String> ensureOk =
        monadError.ensure(pure, Unit::unit, "is not ok"::equals);
    Kind<Kind<OptionT<?, ?>, Id<?>>, String> ensureError =
        monadError.ensure(pure, Unit::unit, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Id.of(Option.none()), OptionTOf.narrowK(raiseError).value()),
        () -> assertEquals(Id.of(Option.some("not an error")), OptionTOf.narrowK(handleError).value()),
        () -> assertEquals(Id.of(Option.none()), OptionTOf.narrowK(ensureError).value()),
        () -> assertEquals(Id.of(Option.some("is not ok")), OptionTOf.narrowK(ensureOk).value()));
  }
}

class IOToTryFunctionK implements FunctionK<IO<?>, Try<?>> {

  @Override
  public <T> Kind<Try<?>, T> apply(Kind<IO<?>, ? extends T> from) {
    return Try.of(from.fix(toIO())::unsafeRunSync);
  }
}
