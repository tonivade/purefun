/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static com.github.tonivade.purefun.Unit.unit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionTInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.Transformer;

public class OptionTTest {

  final Monad<Id.µ> monad = IdInstances.monad();

  @Test
  public void map() {
    OptionT<Id.µ, String> some = OptionT.some(monad, "abc");

    OptionT<Id.µ, String> map = some.map(String::toUpperCase);

    assertEquals(Id.of("ABC"), map.get());
  }

  @Test
  public void flatMap() {
    OptionT<Id.µ, String> some = OptionT.some(monad, "abc");

    OptionT<Id.µ, String> map = some.flatMap(value -> OptionT.some(monad, value.toUpperCase()));

    assertEquals(Id.of("ABC"), map.get());
  }

  @Test
  public void filter() {
    OptionT<Id.µ, String> some = OptionT.some(monad, "abc");

    OptionT<Id.µ, String> filter = some.filter(String::isEmpty);
    OptionT<Id.µ, String> orElse = OptionT.some(monad, "not empty");

    assertEquals(orElse.get(), filter.getOrElse("not empty"));
  }

  @Test
  public void none() {
    OptionT<Id.µ, String> none = OptionT.none(monad);

    assertAll(
        () -> assertEquals(Id.of(true), none.isEmpty()),
        () -> assertEquals(Id.of("empty"), none.getOrElse("empty")));
  }

  @Test
  public void some() {
    OptionT<Id.µ, String> some = OptionT.some(monad, "abc");

    assertAll(
        () -> assertEquals(Id.of(false), some.isEmpty()),
        () -> assertEquals(Id.of("abc"), some.getOrElse("empty")));
  }

  @Test
  public void mapK() {
    OptionT<IO.µ, String> someIo = OptionT.some(IOInstances.monad(), "abc");

    OptionT<Try.µ, String> someTry = someIo.mapK(TryInstances.monad(), new IOToTryTransformer());

    assertEquals(Try.success("abc"), Try.narrowK(someTry.get()));
  }

  @Test
  public void eq() {
    OptionT<Id.µ, String> some1 = OptionT.some(monad, "abc");
    OptionT<Id.µ, String> some2 = OptionT.some(monad, "abc");
    OptionT<Id.µ, String> none1 = OptionT.none(monad);
    OptionT<Id.µ, String> none2 = OptionT.none(monad);

    Eq<Higher2<OptionT.µ, Id.µ, String>> instance = OptionTInstances.eq(IdInstances.eq(Eq.any()));

    assertAll(
        () -> assertTrue(instance.eqv(some1.kind2(), some2.kind2())),
        () -> assertTrue(instance.eqv(none1.kind2(), none2.kind2())),
        () -> assertFalse(instance.eqv(some1.kind2(), none1.kind2())),
        () -> assertFalse(instance.eqv(none2.kind2(), some2.kind2())));
  }

  @Test
  public void monadErrorFuture() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Higher1<OptionT.µ, Future.µ>, Throwable> monadError =
        OptionTInstances.monadError(FutureInstances.monadError());

    Higher1<Higher1<OptionT.µ, Future.µ>, String> pure = monadError.pure("is not ok");
    Higher1<Higher1<OptionT.µ, Future.µ>, String> raiseError = monadError.raiseError(error);
    Higher1<Higher1<OptionT.µ, Future.µ>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Higher1<Higher1<OptionT.µ, Future.µ>, String> ensureOk =
        monadError.ensure(pure, () -> error, value -> "is not ok".equals(value));
    Higher1<Higher1<OptionT.µ, Future.µ>, String> ensureError =
        monadError.ensure(pure, () -> error, value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Try.failure(error), Future.narrowK(OptionT.narrowK(raiseError).value()).await()),
        () -> assertEquals(Try.success(Option.some("not an error")), Future.narrowK(OptionT.narrowK(handleError).value()).await()),
        () -> assertEquals(Try.failure(error), Future.narrowK(OptionT.narrowK(ensureError).value()).await()),
        () -> assertEquals(Try.success(Option.some("is not ok")), Future.narrowK(OptionT.narrowK(ensureOk).value()).await()));
  }

  @Test
  public void monadErrorIO() {
    MonadError<Higher1<OptionT.µ, Id.µ>, Unit> monadError = OptionTInstances.monadError(monad);

    Higher1<Higher1<OptionT.µ, Id.µ>, String> pure = monadError.pure("is not ok");
    Higher1<Higher1<OptionT.µ, Id.µ>, String> raiseError = monadError.raiseError(unit());
    Higher1<Higher1<OptionT.µ, Id.µ>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Higher1<Higher1<OptionT.µ, Id.µ>, String> ensureOk =
        monadError.ensure(pure, Unit::unit, "is not ok"::equals);
    Higher1<Higher1<OptionT.µ, Id.µ>, String> ensureError =
        monadError.ensure(pure, Unit::unit, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Id.of(Option.none()), OptionT.narrowK(raiseError).value()),
        () -> assertEquals(Id.of(Option.some("not an error")), OptionT.narrowK(handleError).value()),
        () -> assertEquals(Id.of(Option.none()), OptionT.narrowK(ensureError).value()),
        () -> assertEquals(Id.of(Option.some("is not ok")), OptionT.narrowK(ensureOk).value()));
  }
}

class IOToTryTransformer implements Transformer<IO.µ, Try.µ> {

  @Override
  public <T> Higher1<Try.µ, T> apply(Higher1<IO.µ, T> from) {
    return Try.of(IO.narrowK(from)::unsafeRunSync).kind1();
  }
}
