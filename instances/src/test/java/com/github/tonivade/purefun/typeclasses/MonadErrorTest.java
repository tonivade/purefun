/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Matcher1.always;
import static com.github.tonivade.purefun.Matcher1.is;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Unit.unit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.FutureOf;
import com.github.tonivade.purefun.concurrent.Future_;
import com.github.tonivade.purefun.effect.EIOOf;
import com.github.tonivade.purefun.effect.EIO_;
import com.github.tonivade.purefun.effect.TaskOf;
import com.github.tonivade.purefun.effect.Task_;
import com.github.tonivade.purefun.effect.UIOOf;
import com.github.tonivade.purefun.effect.UIO_;
import com.github.tonivade.purefun.effect.ZIOOf;
import com.github.tonivade.purefun.effect.ZIO_;
import com.github.tonivade.purefun.instances.EIOInstances;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.EvalInstances;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.TaskInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.instances.UIOInstances;
import com.github.tonivade.purefun.instances.ZIOInstances;
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Either_;
import com.github.tonivade.purefun.type.EvalOf;
import com.github.tonivade.purefun.type.Eval_;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Option_;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.Try_;

public class MonadErrorTest {

  private final MonadError<Try_, Throwable> monadError = TryInstances.monadError();

  @Test
  public void recover() {
    Kind<Try_, String> recover =
        monadError.recover(Try.<String>failure("error"), PartialFunction1.of(always(), Throwable::toString));

    assertEquals(Try.success("java.lang.Exception: error"), recover);
  }

  @Test
  public void attempRight() {
    Kind<Try_, Either<Throwable, String>> attempt = monadError.attempt(Try.success("hola mundo!"));

    assertEquals(Try.success(Either.right("hola mundo!")), attempt);
  }

  @Test
  public void attempLeft() {
    Exception error = new Exception("error");

    Kind<Try_, Either<Throwable, String>> attempt = monadError.attempt(Try.<String>failure(error));

    assertEquals(Try.success(Either.left(error)), attempt);
  }

  @Test
  public void ensureError() {
    Exception error = new Exception("error");

    Kind<Try_, String> ensure =
        monadError.ensure(Try.success("not ok"), cons(error), is("ok"));

    assertEquals(Try.failure(error), ensure);
  }

  @Test
  public void ensureOk() {
    Exception error = new Exception("error");

    Kind<Try_, String> ensure =
        monadError.ensure(Try.success("ok"), cons(error), is("ok"));

    assertEquals(Try.success("ok"), ensure);
  }

  @Test
  public void either() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Kind<Either_, Throwable>, Throwable> monadError = EitherInstances.<Throwable>monadError();

    Kind<Kind<Either_, Throwable>, String> pure = monadError.pure("is not ok");
    Kind<Kind<Either_, Throwable>, String> raiseError = monadError.raiseError(error);
    Kind<Kind<Either_, Throwable>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Kind<Kind<Either_, Throwable>, String> ensureOk =
        monadError.ensure(pure, () -> error, value -> "is not ok".equals(value));
    Kind<Kind<Either_, Throwable>, String> ensureError =
        monadError.ensure(pure, () -> error, value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Either.left(error), raiseError),
        () -> assertEquals(Either.right("not an error"), handleError),
        () -> assertEquals(Either.left(error), ensureError),
        () -> assertEquals(Either.right("is not ok"), ensureOk));
  }

  @Test
  public void option() {
    MonadError<Option_, Unit> monadError = OptionInstances.monadError();

    Kind<Option_, String> pure = monadError.pure("is not ok");
    Kind<Option_, String> raiseError = monadError.raiseError(unit());
    Kind<Option_, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<Option_, String> ensureOk = monadError.ensure(pure, Unit::unit, "is not ok"::equals);
    Kind<Option_, String> ensureError = monadError.ensure(pure, Unit::unit, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Option.none(), raiseError),
        () -> assertEquals(Option.some("not an error"), handleError),
        () -> assertEquals(Option.none(), ensureError),
        () -> assertEquals(Option.some("is not ok"), ensureOk));
  }

  @Test
  public void try_() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Try_, Throwable> monadError = TryInstances.monadError();

    Kind<Try_, String> pure = monadError.pure("is not ok");
    Kind<Try_, String> raiseError = monadError.raiseError(error);
    Kind<Try_, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<Try_, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<Try_, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Try.failure(error), raiseError),
        () -> assertEquals(Try.success("not an error"), handleError),
        () -> assertEquals(Try.failure(error), ensureError),
        () -> assertEquals(Try.success("is not ok"), ensureOk));
  }

  @Test
  public void future() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Future_, Throwable> monadError = FutureInstances.monadError();

    Kind<Future_, String> pure = monadError.pure("is not ok");
    Kind<Future_, String> raiseError = monadError.raiseError(error);
    Kind<Future_, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<Future_, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<Future_, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Try.failure(error), FutureOf.narrowK(raiseError).await()),
        () -> assertEquals(Try.success("not an error"), FutureOf.narrowK(handleError).await()),
        () -> assertEquals(Try.failure(error), FutureOf.narrowK(ensureError).await()),
        () -> assertEquals(Try.success("is not ok"), FutureOf.narrowK(ensureOk).await()));
  }

  @Test
  public void eval() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Eval_, Throwable> monadError = EvalInstances.monadError();

    Kind<Eval_, String> pure = monadError.pure("is not ok");
    Kind<Eval_, String> raiseError = monadError.raiseError(error);
    Kind<Eval_, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<Eval_, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<Eval_, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertThrows(RuntimeException.class, () -> EvalOf.narrowK(raiseError).value()),
        () -> assertEquals("not an error", EvalOf.narrowK(handleError).value()),
        () -> assertThrows(RuntimeException.class, () -> EvalOf.narrowK(ensureError).value()),
        () -> assertEquals("is not ok", EvalOf.narrowK(ensureOk).value()));
  }

  @Test
  public void io() {
    RuntimeException error = new RuntimeException("error");
    MonadError<IO_, Throwable> monadError = IOInstances.monadError();

    Kind<IO_, String> pure = monadError.pure("is not ok");
    Kind<IO_, String> raiseError = monadError.raiseError(error);
    Kind<IO_, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<IO_, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<IO_, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertThrows(RuntimeException.class, () -> IOOf.narrowK(raiseError).unsafeRunSync()),
        () -> assertEquals("not an error", IOOf.narrowK(handleError).unsafeRunSync()),
        () -> assertThrows(RuntimeException.class, () -> IOOf.narrowK(ensureError).unsafeRunSync()),
        () -> assertEquals("is not ok", IOOf.narrowK(ensureOk).unsafeRunSync()));
  }

  @Test
  public void uio() {
    RuntimeException error = new RuntimeException("error");
    MonadError<UIO_, Throwable> monadError = UIOInstances.monadError();

    Kind<UIO_, String> pure = monadError.pure("is not ok");
    Kind<UIO_, String> raiseError = monadError.raiseError(error);
    Kind<UIO_, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<UIO_, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<UIO_, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertThrows(RuntimeException.class, () -> UIOOf.narrowK(raiseError).unsafeRunSync()),
        () -> assertEquals("not an error", UIOOf.narrowK(handleError).unsafeRunSync()),
        () -> assertThrows(RuntimeException.class, () -> UIOOf.narrowK(ensureError).unsafeRunSync()),
        () -> assertEquals("is not ok", UIOOf.narrowK(ensureOk).unsafeRunSync()));
  }

  @Test
  public void eio() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Kind<EIO_, Throwable>, Throwable> monadError = EIOInstances.monadThrow();

    Kind<Kind<EIO_, Throwable>, String> pure = monadError.pure("is not ok");
    Kind<Kind<EIO_, Throwable>, String> raiseError = monadError.raiseError(error);
    Kind<Kind<EIO_, Throwable>, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<Kind<EIO_, Throwable>, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<Kind<EIO_, Throwable>, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Either.<Throwable, String>left(error), EIOOf.narrowK(raiseError).safeRunSync()),
        () -> assertEquals(Either.<Throwable, String>right("not an error"), EIOOf.narrowK(handleError).safeRunSync()),
        () -> assertEquals(Either.<Throwable, String>left(error), EIOOf.narrowK(ensureError).safeRunSync()),
        () -> assertEquals(Either.<Throwable, String>right("is not ok"), EIOOf.narrowK(ensureOk).safeRunSync()));
  }

  @Test
  public void task() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Task_, Throwable> monadError = TaskInstances.monadThrow();

    Kind<Task_, String> pure = monadError.pure("is not ok");
    Kind<Task_, String> raiseError = monadError.raiseError(error);
    Kind<Task_, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<Task_, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<Task_, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Try.<String>failure(error), TaskOf.narrowK(raiseError).safeRunSync()),
        () -> assertEquals(Try.success("not an error"), TaskOf.narrowK(handleError).safeRunSync()),
        () -> assertEquals(Try.<String>failure(error), TaskOf.narrowK(ensureError).safeRunSync()),
        () -> assertEquals(Try.success("is not ok"), TaskOf.narrowK(ensureOk).safeRunSync()));
  }

  @Test
  public void zio() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Kind<Kind<ZIO_, Nothing>, Throwable>, Throwable> monadError = ZIOInstances.monadThrow();

    Kind<Kind<Kind<ZIO_, Nothing>, Throwable>, String> pure = monadError.pure("is not ok");
    Kind<Kind<Kind<ZIO_, Nothing>, Throwable>, String> raiseError = monadError.raiseError(error);
    Kind<Kind<Kind<ZIO_, Nothing>, Throwable>, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<Kind<Kind<ZIO_, Nothing>, Throwable>, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<Kind<Kind<ZIO_, Nothing>, Throwable>, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Either.<Throwable, String>left(error), ZIOOf.narrowK(raiseError).provide(nothing())),
        () -> assertEquals(Either.<Throwable, String>right("not an error"), ZIOOf.narrowK(handleError).provide(nothing())),
        () -> assertEquals(Either.<Throwable, String>left(error), ZIOOf.narrowK(ensureError).provide(nothing())),
        () -> assertEquals(Either.<Throwable, String>right("is not ok"), ZIOOf.narrowK(ensureOk).provide(nothing())));
  }
}
