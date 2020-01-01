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

import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.instances.EIOInstances;
import com.github.tonivade.purefun.instances.EvalInstances;
import com.github.tonivade.purefun.instances.TaskInstances;
import com.github.tonivade.purefun.instances.UIOInstances;
import com.github.tonivade.purefun.instances.ZIOInstances;
import com.github.tonivade.purefun.effect.EIO;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.ZIO;
import com.github.tonivade.purefun.type.Eval;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

public class MonadErrorTest {

  private final MonadError<Try.µ, Throwable> monadError = TryInstances.monadError();

  @Test
  public void recover() {
    Higher1<Try.µ, String> recover =
        monadError.recover(Try.<String>failure("error").kind1(), PartialFunction1.of(always(), Throwable::toString));

    assertEquals(Try.success("java.lang.Exception: error"), recover);
  }

  @Test
  public void attempRight() {
    Higher1<Try.µ, Either<Throwable, String>> attempt = monadError.attempt(Try.success("hola mundo!").kind1());

    assertEquals(Try.success(Either.right("hola mundo!")), attempt);
  }

  @Test
  public void attempLeft() {
    Exception error = new Exception("error");

    Higher1<Try.µ, Either<Throwable, String>> attempt = monadError.attempt(Try.<String>failure(error).kind1());

    assertEquals(Try.success(Either.left(error)), attempt);
  }

  @Test
  public void ensureError() {
    Exception error = new Exception("error");

    Higher1<Try.µ, String> ensure =
        monadError.ensure(Try.success("not ok").kind1(), cons(error), is("ok"));

    assertEquals(Try.failure(error), ensure);
  }

  @Test
  public void ensureOk() {
    Exception error = new Exception("error");

    Higher1<Try.µ, String> ensure =
        monadError.ensure(Try.success("ok").kind1(), cons(error), is("ok"));

    assertEquals(Try.success("ok"), ensure);
  }

  @Test
  public void either() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Higher1<Either.µ, Throwable>, Throwable> monadError = EitherInstances.<Throwable>monadError();

    Higher1<Higher1<Either.µ, Throwable>, String> pure = monadError.pure("is not ok");
    Higher1<Higher1<Either.µ, Throwable>, String> raiseError = monadError.raiseError(error);
    Higher1<Higher1<Either.µ, Throwable>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Higher1<Higher1<Either.µ, Throwable>, String> ensureOk =
        monadError.ensure(pure, () -> error, value -> "is not ok".equals(value));
    Higher1<Higher1<Either.µ, Throwable>, String> ensureError =
        monadError.ensure(pure, () -> error, value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Either.left(error), raiseError),
        () -> assertEquals(Either.right("not an error"), handleError),
        () -> assertEquals(Either.left(error), ensureError),
        () -> assertEquals(Either.right("is not ok"), ensureOk));
  }

  @Test
  public void option() {
    MonadError<Option.µ, Unit> monadError = OptionInstances.monadError();

    Higher1<Option.µ, String> pure = monadError.pure("is not ok");
    Higher1<Option.µ, String> raiseError = monadError.raiseError(unit());
    Higher1<Option.µ, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Option.µ, String> ensureOk = monadError.ensure(pure, Unit::unit, "is not ok"::equals);
    Higher1<Option.µ, String> ensureError = monadError.ensure(pure, Unit::unit, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Option.none(), raiseError),
        () -> assertEquals(Option.some("not an error"), handleError),
        () -> assertEquals(Option.none(), ensureError),
        () -> assertEquals(Option.some("is not ok"), ensureOk));
  }

  @Test
  public void try_() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Try.µ, Throwable> monadError = TryInstances.monadError();

    Higher1<Try.µ, String> pure = monadError.pure("is not ok");
    Higher1<Try.µ, String> raiseError = monadError.raiseError(error);
    Higher1<Try.µ, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Try.µ, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<Try.µ, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Try.failure(error), raiseError),
        () -> assertEquals(Try.success("not an error"), handleError),
        () -> assertEquals(Try.failure(error), ensureError),
        () -> assertEquals(Try.success("is not ok"), ensureOk));
  }

  @Test
  public void future() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Future.µ, Throwable> monadError = FutureInstances.monadError();

    Higher1<Future.µ, String> pure = monadError.pure("is not ok");
    Higher1<Future.µ, String> raiseError = monadError.raiseError(error);
    Higher1<Future.µ, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Future.µ, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<Future.µ, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Try.failure(error), Future.narrowK(raiseError).await()),
        () -> assertEquals(Try.success("not an error"), Future.narrowK(handleError).await()),
        () -> assertEquals(Try.failure(error), Future.narrowK(ensureError).await()),
        () -> assertEquals(Try.success("is not ok"), Future.narrowK(ensureOk).await()));
  }

  @Test
  public void eval() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Eval.µ, Throwable> monadError = EvalInstances.monadError();

    Higher1<Eval.µ, String> pure = monadError.pure("is not ok");
    Higher1<Eval.µ, String> raiseError = monadError.raiseError(error);
    Higher1<Eval.µ, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Eval.µ, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<Eval.µ, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertThrows(RuntimeException.class, () -> Eval.narrowK(raiseError).value()),
        () -> assertEquals("not an error", Eval.narrowK(handleError).value()),
        () -> assertThrows(RuntimeException.class, () -> Eval.narrowK(ensureError).value()),
        () -> assertEquals("is not ok", Eval.narrowK(ensureOk).value()));
  }

  @Test
  public void io() {
    RuntimeException error = new RuntimeException("error");
    MonadError<IO.µ, Throwable> monadError = IOInstances.monadError();

    Higher1<IO.µ, String> pure = monadError.pure("is not ok");
    Higher1<IO.µ, String> raiseError = monadError.raiseError(error);
    Higher1<IO.µ, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<IO.µ, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<IO.µ, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertThrows(RuntimeException.class, () -> IO.narrowK(raiseError).unsafeRunSync()),
        () -> assertEquals("not an error", IO.narrowK(handleError).unsafeRunSync()),
        () -> assertThrows(RuntimeException.class, () -> IO.narrowK(ensureError).unsafeRunSync()),
        () -> assertEquals("is not ok", IO.narrowK(ensureOk).unsafeRunSync()));
  }

  @Test
  public void uio() {
    RuntimeException error = new RuntimeException("error");
    MonadError<UIO.µ, Throwable> monadError = UIOInstances.monadError();

    Higher1<UIO.µ, String> pure = monadError.pure("is not ok");
    Higher1<UIO.µ, String> raiseError = monadError.raiseError(error);
    Higher1<UIO.µ, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<UIO.µ, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<UIO.µ, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertThrows(RuntimeException.class, () -> UIO.narrowK(raiseError).unsafeRunSync()),
        () -> assertEquals("not an error", UIO.narrowK(handleError).unsafeRunSync()),
        () -> assertThrows(RuntimeException.class, () -> UIO.narrowK(ensureError).unsafeRunSync()),
        () -> assertEquals("is not ok", UIO.narrowK(ensureOk).unsafeRunSync()));
  }

  @Test
  public void eio() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Higher1<EIO.µ, Throwable>, Throwable> monadError = EIOInstances.monadThrow();

    Higher1<Higher1<EIO.µ, Throwable>, String> pure = monadError.pure("is not ok");
    Higher1<Higher1<EIO.µ, Throwable>, String> raiseError = monadError.raiseError(error);
    Higher1<Higher1<EIO.µ, Throwable>, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Higher1<EIO.µ, Throwable>, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<Higher1<EIO.µ, Throwable>, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Either.<Throwable, String>left(error), EIO.narrowK(raiseError).safeRunSync()),
        () -> assertEquals(Either.<Throwable, String>right("not an error"), EIO.narrowK(handleError).safeRunSync()),
        () -> assertEquals(Either.<Throwable, String>left(error), EIO.narrowK(ensureError).safeRunSync()),
        () -> assertEquals(Either.<Throwable, String>right("is not ok"), EIO.narrowK(ensureOk).safeRunSync()));
  }

  @Test
  public void task() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Task.µ, Throwable> monadError = TaskInstances.monadThrow();

    Higher1<Task.µ, String> pure = monadError.pure("is not ok");
    Higher1<Task.µ, String> raiseError = monadError.raiseError(error);
    Higher1<Task.µ, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Task.µ, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<Task.µ, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Try.<String>failure(error), Task.narrowK(raiseError).safeRunSync()),
        () -> assertEquals(Try.success("not an error"), Task.narrowK(handleError).safeRunSync()),
        () -> assertEquals(Try.<String>failure(error), Task.narrowK(ensureError).safeRunSync()),
        () -> assertEquals(Try.success("is not ok"), Task.narrowK(ensureOk).safeRunSync()));
  }

  @Test
  public void zio() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Higher1<Higher1<ZIO.µ, Nothing>, Throwable>, Throwable> monadError = ZIOInstances.monadThrow();

    Higher1<Higher1<Higher1<ZIO.µ, Nothing>, Throwable>, String> pure = monadError.pure("is not ok");
    Higher1<Higher1<Higher1<ZIO.µ, Nothing>, Throwable>, String> raiseError = monadError.raiseError(error);
    Higher1<Higher1<Higher1<ZIO.µ, Nothing>, Throwable>, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Higher1<Higher1<ZIO.µ, Nothing>, Throwable>, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<Higher1<Higher1<ZIO.µ, Nothing>, Throwable>, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Either.<Throwable, String>left(error), ZIO.narrowK(raiseError).provide(nothing())),
        () -> assertEquals(Either.<Throwable, String>right("not an error"), ZIO.narrowK(handleError).provide(nothing())),
        () -> assertEquals(Either.<Throwable, String>left(error), ZIO.narrowK(ensureError).provide(nothing())),
        () -> assertEquals(Either.<Throwable, String>right("is not ok"), ZIO.narrowK(ensureOk).provide(nothing())));
  }
}
