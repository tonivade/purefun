/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static com.github.tonivade.purefun.core.Producer.cons;
import static com.github.tonivade.purefun.core.Unit.unit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.FutureOf;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.instances.EitherTInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryOf;
import com.github.tonivade.purefun.typeclasses.Instances;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import org.junit.jupiter.api.Test;

public class EitherTTest {

  private final Monad<Id<?>> monad = Instances.monad();

  @Test
  public void map() {
    EitherT<Id<?>, Void, String> right = EitherT.right(monad, "abc");

    EitherT<Id<?>, Void, String> map = right.map(String::toUpperCase);

    assertEquals(Id.of("ABC"), map.get());
  }

  @Test
  public void flatMap() {
    EitherT<Id<?>, Void, String> right = EitherT.right(monad, "abc");

    EitherT<Id<?>, Void, String> map = right.flatMap(value -> EitherT.right(monad, value.toUpperCase()));

    assertEquals(Id.of("ABC"), map.get());
  }

  @Test
  public void filterOrElse() {
    EitherT<Id<?>, Void, String> right = EitherT.right(monad, "abc");

    EitherT<Id<?>, Void, String> filter = right.filterOrElse(String::isEmpty, cons(Either.right("not empty")));
    EitherT<Id<?>, Void, String> orElse = EitherT.right(monad, "not empty");

    assertEquals(orElse.get(), filter.getOrElse("not empty"));
  }

  @Test
  public void left() {
    EitherT<Id<?>, Unit, String> left = EitherT.left(monad, unit());

    assertAll(
        () -> assertEquals(Id.of(true), left.isLeft()),
        () -> assertEquals(Id.of(false), left.isRight()),
        () -> assertEquals(Id.of("empty"), left.getOrElse("empty")));
  }

  @Test
  public void right() {
    EitherT<Id<?>, Void, String> right = EitherT.right(monad, "abc");

    assertAll(
        () -> assertEquals(Id.of(false), right.isLeft()),
        () -> assertEquals(Id.of(true), right.isRight()),
        () -> assertEquals(Id.of("abc"), right.getOrElse("empty")));
  }

  @Test
  public void mapK() {
    EitherT<IO<?>, Void, String> rightIo = EitherT.right(Instances.<IO<?>>monad(), "abc");

    EitherT<Try<?>, Void, String> rightTry = rightIo.mapK(Instances.monad(), new IOToTryFunctionK());

    assertEquals(Try.success("abc"), TryOf.toTry(rightTry.get()));
  }

  @Test
  public void monadErrorFuture() {
    RuntimeException error = new RuntimeException("error");
    MonadError<EitherT<Future<?>, Throwable, ?>, Throwable> monadError =
        EitherTInstances.monadError(Instances.<Future<?>, Throwable>monadError());

    Kind<EitherT<Future<?>, Throwable, ?>, String> pure = monadError.pure("is not ok");
    Kind<EitherT<Future<?>, Throwable, ?>, String> raiseError = monadError.raiseError(error);
    Kind<EitherT<Future<?>, Throwable, ?>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Kind<EitherT<Future<?>, Throwable, ?>, String> ensureOk =
        monadError.ensure(pure, () -> error, value -> "is not ok".equals(value));
    Kind<EitherT<Future<?>, Throwable, ?>, String> ensureError =
        monadError.ensure(pure, () -> error, value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Try.failure(error), raiseError.fix(EitherTOf::toEitherT).value().fix(FutureOf::toFuture).await()),
        () -> assertEquals(Try.success(Either.right("not an error")), handleError.fix(EitherTOf::toEitherT).value().fix(FutureOf::toFuture).await()),
        () -> assertEquals(Try.failure(error), ensureError.fix(EitherTOf::toEitherT).value().fix(FutureOf::toFuture).await()),
        () -> assertEquals(Try.success(Either.right("is not ok")), ensureOk.fix(EitherTOf::toEitherT).value().fix(FutureOf::toFuture).await()));
  }

  @Test
  public void monadErrorIO() {
    RuntimeException error = new RuntimeException("error");
    MonadError<EitherT<Id<?>, Throwable, ?>, Throwable> monadError =
        EitherTInstances.monadError(monad);

    Kind<EitherT<Id<?>, Throwable, ?>, String> pure = monadError.pure("is not ok");
    Kind<EitherT<Id<?>, Throwable, ?>, String> raiseError = monadError.raiseError(error);
    Kind<EitherT<Id<?>, Throwable, ?>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Kind<EitherT<Id<?>, Throwable, ?>, String> ensureOk =
        monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<EitherT<Id<?>, Throwable, ?>, String> ensureError =
        monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Id.of(Either.left(error)), raiseError.fix(EitherTOf::toEitherT).value()),
        () -> assertEquals(Id.of(Either.right("not an error")), handleError.fix(EitherTOf::toEitherT).value()),
        () -> assertEquals(Id.of(Either.left(error)), ensureError.fix(EitherTOf::toEitherT).value()),
        () -> assertEquals(Id.of(Either.right("is not ok")), ensureOk.fix(EitherTOf::toEitherT).value()));
  }
}
