/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Nothing.nothing;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.type.Future;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Eq;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.Transformer;

public class OptionTTest {

  final Monad<IO.µ> monad = IO.monad();

  @Test
  public void map() {
    OptionT<IO.µ, String> some = OptionT.some(monad, "abc");

    OptionT<IO.µ, String> map = some.map(String::toUpperCase);

    assertEquals("ABC", IO.narrowK(map.get()).unsafeRunSync());
  }

  @Test
  public void flatMap() {
    OptionT<IO.µ, String> some = OptionT.some(monad, "abc");

    OptionT<IO.µ, String> map = some.flatMap(value -> OptionT.some(monad, value.toUpperCase()));

    assertEquals("ABC", IO.narrowK(map.get()).unsafeRunSync());
  }

  @Test
  public void filter() {
    OptionT<IO.µ, String> some = OptionT.some(monad, "abc");

    OptionT<IO.µ, String> filter = some.filter(String::isEmpty);
    OptionT<IO.µ, String> orElse = OptionT.some(monad, "not empty");

    assertEquals(IO.narrowK(orElse.get()).unsafeRunSync(), IO.narrowK(filter.orElse("not empty")).unsafeRunSync());
  }

  @Test
  public void none() {
    OptionT<IO.µ, String> none = OptionT.none(monad);

    assertAll(
        () -> assertTrue(IO.narrowK(none.isEmpty()).unsafeRunSync()),
        () -> assertEquals("empty", IO.narrowK(none.orElse("empty")).unsafeRunSync()));
  }

  @Test
  public void some() {
    OptionT<IO.µ, String> some = OptionT.some(monad, "abc");

    assertAll(
        () -> assertFalse(IO.narrowK(some.isEmpty()).unsafeRunSync()),
        () -> assertEquals("abc", IO.narrowK(some.orElse("empty")).unsafeRunSync()));
  }

  @Test
  public void mapK() {
    OptionT<IO.µ, String> someIo = OptionT.some(monad, "abc");

    OptionT<Try.µ, String> someTry = someIo.mapK(Try.monad(), new IOToTryTransformer());

    assertEquals(Try.success("abc"), Try.narrowK(someTry.get()));
  }

  @Test
  public void eq() {
    OptionT<Try.µ, String> some1 = OptionT.some(Try.monad(), "abc");
    OptionT<Try.µ, String> some2 = OptionT.some(Try.monad(), "abc");
    OptionT<Try.µ, String> none1 = OptionT.none(Try.monad());
    OptionT<Try.µ, String> none2 = OptionT.none(Try.monad());

    Eq<Higher2<OptionT.µ, Try.µ, String>> instance = OptionT.eq(Try.eq(Eq.object(), Eq.throwable()));

    assertAll(
        () -> assertTrue(instance.eqv(some1, some2)),
        () -> assertTrue(instance.eqv(none1, none2)),
        () -> assertFalse(instance.eqv(some1, none1)),
        () -> assertFalse(instance.eqv(none2, some2)));
  }

  @Test
  public void monadErrorFuture() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Higher1<OptionT.µ, Future.µ>, Throwable> monadError = OptionT.monadError(Future.monadError());

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
    MonadError<Higher1<OptionT.µ, IO.µ>, Nothing> monadError = OptionT.monadError(IO.monad());

    Higher1<Higher1<OptionT.µ, IO.µ>, String> pure = monadError.pure("is not ok");
    Higher1<Higher1<OptionT.µ, IO.µ>, String> raiseError = monadError.raiseError(nothing());
    Higher1<Higher1<OptionT.µ, IO.µ>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Higher1<Higher1<OptionT.µ, IO.µ>, String> ensureOk =
        monadError.ensure(pure, () -> nothing(), value -> "is not ok".equals(value));
    Higher1<Higher1<OptionT.µ, IO.µ>, String> ensureError =
        monadError.ensure(pure, () -> nothing(), value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Option.none(), IO.narrowK(OptionT.narrowK(raiseError).value()).unsafeRunSync()),
        () -> assertEquals(Option.some("not an error"), IO.narrowK(OptionT.narrowK(handleError).value()).unsafeRunSync()),
        () -> assertEquals(Option.none(), IO.narrowK(OptionT.narrowK(ensureError).value()).unsafeRunSync()),
        () -> assertEquals(Option.some("is not ok"), IO.narrowK(OptionT.narrowK(ensureOk).value()).unsafeRunSync()));
  }
}

class IOToTryTransformer implements Transformer<IO.µ, Try.µ> {

  @Override
  public <T> Higher1<Try.µ, T> apply(Higher1<IO.µ, T> from) {
    return Try.of(IO.narrowK(from)::unsafeRunSync);
  }
}
