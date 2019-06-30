/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Nothing.nothing;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.instances.EitherTInstances;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.OptionTInstances;
import com.github.tonivade.purefun.instances.ZIOInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.transformer.EitherT;
import com.github.tonivade.purefun.transformer.OptionT;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.zio.ZIO;

public class MonadDeferTest {

  private MonadDefer<IO.µ> ioMonadDefer = IOInstances.monadDefer();
  private MonadDefer<Higher1<Higher1<ZIO.µ, Nothing>, Throwable>> zioMonadDefer =
      ZIOInstances.monadDefer();
  private MonadDefer<Future.µ> futureMonadDefer = FutureInstances.monadDefer();
  private MonadDefer<Higher1<Higher1<EitherT.µ, IO.µ>, Throwable>> eitherTMonadDefer =
      EitherTInstances.monadDefer(ioMonadDefer);
  private MonadDefer<Higher1<OptionT.µ, IO.µ>> optionTMonadDefer =
      OptionTInstances.monadDefer(ioMonadDefer);

  private AutoCloseable resource = Mockito.mock(AutoCloseable.class);

  @Test
  public void ioBracket() throws Exception {
    Higher1<IO.µ, String> bracket =
        ioMonadDefer.bracket(IO.pure(resource), r -> IO.pure("done"));

    String result = bracket.fix1(IO::narrowK).unsafeRunSync();

    assertEquals("done", result);
    verify(resource).close();
  }

  @Test
  public void ioBracketAcquireError() throws Exception {
    Higher1<IO.µ, String> bracket =
        ioMonadDefer.bracket(IO.raiseError(new IllegalStateException()), r -> IO.pure("done"));

    assertThrows(IllegalStateException.class, () -> bracket.fix1(IO::narrowK).unsafeRunSync());

    verify(resource, never()).close();
  }

  @Test
  public void ioBracketUseError() throws Exception {
    Higher1<IO.µ, String> bracket =
        ioMonadDefer.bracket(IO.pure(resource), r -> IO.raiseError(new UnsupportedOperationException()));

    assertThrows(UnsupportedOperationException.class, () -> bracket.fix1(IO::narrowK).unsafeRunSync());

    verify(resource).close();
  }

  @Test
  public void eitherTBracket() throws Exception {
    Higher1<Higher1<Higher1<EitherT.µ, IO.µ>, Throwable>, String> bracket =
        eitherTMonadDefer.bracket(EitherT.right(IOInstances.monad(), resource),
                                  r -> EitherT.right(IOInstances.monad(), "done"));

    String result = bracket.fix1(EitherT::narrowK).get().fix1(IO::narrowK).unsafeRunSync();

    assertEquals("done", result);
    verify(resource).close();
  }

  @Test
  public void eitherTBracketAcquireError() throws Exception {
    Higher1<Higher1<Higher1<EitherT.µ, IO.µ>, Throwable>, String> bracket =
        eitherTMonadDefer.bracket(EitherT.left(IOInstances.monad(), new IllegalStateException()),
                                  r -> EitherT.right(IOInstances.monad(), "done"));

    Throwable error = bracket.fix1(EitherT::narrowK).getLeft().fix1(IO::narrowK).unsafeRunSync();

    assertEquals(IllegalStateException.class, error.getClass());
    verify(resource, never()).close();
  }

  @Test
  public void eitherTBracketUseError() throws Exception {
    Higher1<Higher1<Higher1<EitherT.µ, IO.µ>, Throwable>, String> bracket =
        eitherTMonadDefer.bracket(EitherT.right(IOInstances.monad(), resource),
                                  r -> EitherT.left(IOInstances.monad(), new UnsupportedOperationException()));

    Throwable error = bracket.fix1(EitherT::narrowK).getLeft().fix1(IO::narrowK).unsafeRunSync();

    assertEquals(UnsupportedOperationException.class, error.getClass());
    verify(resource).close();
  }

  @Test
  public void optionTBracket() throws Exception {
    Higher1<Higher1<OptionT.µ, IO.µ>, String> bracket =
        optionTMonadDefer.bracket(OptionT.some(IOInstances.monad(), resource),
                                  r -> OptionT.some(IOInstances.monad(), "done"));

    String result = bracket.fix1(OptionT::narrowK).get().fix1(IO::narrowK).unsafeRunSync();

    assertEquals("done", result);
    verify(resource).close();
  }

  @Test
  public void optionTBracketAcquireError() throws Exception {
    Higher1<Higher1<OptionT.µ, IO.µ>, String> bracket =
        optionTMonadDefer.bracket(OptionT.none(IOInstances.monad()),
                                  r -> OptionT.some(IOInstances.monad(), "done"));

    NoSuchElementException error = assertThrows(NoSuchElementException.class,
                 () -> bracket.fix1(OptionT::narrowK).get().fix1(IO::narrowK).unsafeRunSync());

    assertEquals("could not acquire resource", error.getMessage());
    verify(resource, never()).close();
  }

  @Test
  public void optionTBracketUseError() throws Exception {
    Higher1<Higher1<OptionT.µ, IO.µ>, String> bracket =
        optionTMonadDefer.bracket(OptionT.some(IOInstances.monad(), resource),
                                  r -> OptionT.none(IOInstances.monad()));

    NoSuchElementException error = assertThrows(NoSuchElementException.class,
                 () -> bracket.fix1(OptionT::narrowK).get().fix1(IO::narrowK).unsafeRunSync());

    assertEquals("get() in none", error.getMessage());
    verify(resource).close();
  }

  @Test
  public void futureBracket() throws Exception {
    Higher1<Future.µ, String> bracket =
        futureMonadDefer.bracket(Future.success(resource), r -> Future.success("done"));

    String result = bracket.fix1(Future::narrowK).getOrElse("fail");

    assertEquals("done", result);
    verify(resource).close();
  }

  @Test
  public void futureBracketAcquireError() throws Exception {
    Higher1<Future.µ, String> bracket =
        futureMonadDefer.bracket(Future.failure(new IllegalStateException()),
                                 r -> Future.success("done"));

    String result = bracket.fix1(Future::narrowK).getOrElse("fail");

    assertEquals("fail", result);
    verify(resource, never()).close();
  }

  @Test
  public void futureBracketUseError() throws Exception {
    Higher1<Future.µ, String> bracket =
        futureMonadDefer.bracket(Future.success(resource),
                                 r -> Future.failure(new UnsupportedOperationException()));

    String result = bracket.fix1(Future::narrowK).getOrElse("fail");

    assertEquals("fail", result);
    verify(resource).close();
  }

  @Test
  public void zioBracket() throws Exception {
    Higher1<Higher1<Higher1<ZIO.µ, Nothing>, Throwable>, String> bracket =
        zioMonadDefer.bracket(ZIO.pure(resource), r -> ZIO.pure("done"));

    Either<Throwable, String> result = bracket.fix1(ZIO::narrowK).provide(nothing());

    assertEquals(Either.right("done"), result);
    verify(resource).close();
  }

  @Test
  public void zioBracketAcquireError() throws Exception {
    Higher1<Higher1<Higher1<ZIO.µ, Nothing>, Throwable>, String> bracket =
        zioMonadDefer.bracket(ZIO.raiseError(new IllegalStateException()),
                              r -> ZIO.pure("done"));

    Either<Throwable, String> result = bracket.fix1(ZIO::narrowK).provide(nothing());

    assertTrue(result.isLeft());
    verify(resource, never()).close();
  }

  @Test
  public void zioBracketUseError() throws Exception {
    Higher1<Higher1<Higher1<ZIO.µ, Nothing>, Throwable>, String> bracket =
        zioMonadDefer.bracket(ZIO.pure(resource),
                              r -> ZIO.raiseError(new UnsupportedOperationException()));

    Either<Throwable, String> result = bracket.fix1(ZIO::narrowK).provide(nothing());

    assertTrue(result.isLeft());
    verify(resource).close();
  }
}
