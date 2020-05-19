/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Nothing.nothing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.FutureOf;
import com.github.tonivade.purefun.concurrent.Future_;
import com.github.tonivade.purefun.effect.ZIO;
import com.github.tonivade.purefun.effect.ZIOOf;
import com.github.tonivade.purefun.effect.ZIO_;
import com.github.tonivade.purefun.instances.EitherTInstances;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.OptionTInstances;
import com.github.tonivade.purefun.instances.ZIOInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.transformer.EitherT;
import com.github.tonivade.purefun.transformer.EitherTOf;
import com.github.tonivade.purefun.transformer.EitherT_;
import com.github.tonivade.purefun.transformer.OptionT;
import com.github.tonivade.purefun.transformer.OptionTOf;
import com.github.tonivade.purefun.transformer.OptionT_;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;

public class MonadDeferTest {

  private MonadDefer<IO_> ioMonadDefer = IOInstances.monadDefer();
  private MonadDefer<Higher1<Higher1<ZIO_, Nothing>, Throwable>> zioMonadDefer =
      ZIOInstances.monadDefer();
  private MonadDefer<Future_> futureMonadDefer = FutureInstances.monadDefer();
  private MonadDefer<Higher1<Higher1<EitherT_, IO_>, Throwable>> eitherTMonadDeferFromMonad =
      EitherTInstances.monadDeferFromMonad(ioMonadDefer);
  private MonadDefer<Higher1<Higher1<EitherT_, IO_>, Throwable>> eitherTMonadDeferFromMonadThrow =
      EitherTInstances.monadDeferFromMonadThrow(ioMonadDefer);
  private MonadDefer<Higher1<OptionT_, IO_>> optionTMonadDefer =
      OptionTInstances.monadDefer(ioMonadDefer);

  private AutoCloseable resource = Mockito.mock(AutoCloseable.class);

  @Test
  public void ioBracket() throws Exception {
    Higher1<IO_, String> bracket =
        ioMonadDefer.bracket(IO.pure(resource), r -> IO.pure("done"));

    String result = bracket.fix1(IOOf::narrowK).unsafeRunSync();

    assertEquals("done", result);
    verify(resource).close();
  }

  @Test
  public void ioBracketAcquireError() throws Exception {
    Higher1<IO_, String> bracket =
        ioMonadDefer.bracket(IO.<AutoCloseable>raiseError(new IllegalStateException()), r -> IO.pure("done"));

    assertThrows(IllegalStateException.class, () -> bracket.fix1(IOOf::narrowK).unsafeRunSync());

    verify(resource, never()).close();
  }

  @Test
  public void ioBracketUseError() throws Exception {
    Higher1<IO_, String> bracket =
        ioMonadDefer.bracket(IO.pure(resource), r -> IO.<String>raiseError(new UnsupportedOperationException()));

    assertThrows(UnsupportedOperationException.class, () -> bracket.fix1(IOOf::narrowK).unsafeRunSync());

    verify(resource).close();
  }

  @Test
  public void eitherTBracket() throws Exception {
    Higher1<Higher1<Higher1<EitherT_, IO_>, Throwable>, String> bracket =
        eitherTMonadDeferFromMonad.bracket(EitherT.<IO_, Throwable, AutoCloseable>right(IOInstances.monad(), resource),
                                           r -> EitherT.<IO_, Throwable, String>right(IOInstances.monad(), "done"));

    String result = bracket.fix1(EitherTOf::narrowK).get().fix1(IOOf::narrowK).unsafeRunSync();

    assertEquals("done", result);
    verify(resource).close();
  }

  @Test
  public void eitherTBracketAcquireError() throws Exception {
    Higher1<Higher1<Higher1<EitherT_, IO_>, Throwable>, String> bracket =
        eitherTMonadDeferFromMonadThrow.bracket(EitherT.<IO_, Throwable, AutoCloseable>left(IOInstances.monad(), new IllegalStateException()),
                                                r -> EitherT.<IO_, Throwable, String>right(IOInstances.monad(), "done"));

    assertThrows(IllegalStateException.class,
                 () -> bracket.fix1(EitherTOf::narrowK).value().fix1(IOOf::narrowK).unsafeRunSync());

    verify(resource, never()).close();
  }

  @Test
  public void eitherTBracketAcquireError2() throws Exception {
    Higher1<Higher1<Higher1<EitherT_, IO_>, Throwable>, String> bracket =
        eitherTMonadDeferFromMonad.bracket(EitherT.<IO_, Throwable, AutoCloseable>left(IOInstances.monad(), new IllegalStateException()),
                                           r -> EitherT.<IO_, Throwable, String>right(IOInstances.monad(), "done"));

    Throwable error = bracket.fix1(EitherTOf::narrowK).getLeft().fix1(IOOf::narrowK).unsafeRunSync();

    assertEquals(IllegalStateException.class, error.getClass());
    verify(resource, never()).close();
  }

  @Test
  public void eitherTBracketUseError() throws Exception {
    Higher1<Higher1<Higher1<EitherT_, IO_>, Throwable>, String> bracket =
        eitherTMonadDeferFromMonad.bracket(EitherT.<IO_, Throwable, AutoCloseable>right(IOInstances.monad(), resource),
                                           r -> EitherT.<IO_, Throwable, String>left(IOInstances.monad(),
                                                             new UnsupportedOperationException()));

    Throwable error = bracket.fix1(EitherTOf::narrowK).getLeft().fix1(IOOf::narrowK).unsafeRunSync();

    assertEquals(UnsupportedOperationException.class, error.getClass());
    verify(resource).close();
  }

  @Test
  public void optionTBracket() throws Exception {
    Higher1<Higher1<OptionT_, IO_>, String> bracket =
        optionTMonadDefer.bracket(OptionT.some(IOInstances.monad(), resource),
                                  r -> OptionT.some(IOInstances.monad(), "done"));

    String result = bracket.fix1(OptionTOf::narrowK).get().fix1(IOOf::narrowK).unsafeRunSync();

    assertEquals("done", result);
    verify(resource).close();
  }

  @Test
  public void optionTBracketAcquireError() throws Exception {
    Higher1<Higher1<OptionT_, IO_>, String> bracket =
        optionTMonadDefer.bracket(OptionT.<IO_, AutoCloseable>none(IOInstances.monad()),
                                  r -> OptionT.some(IOInstances.monad(), "done"));

    NoSuchElementException error = assertThrows(NoSuchElementException.class,
                 () -> bracket.fix1(OptionTOf::narrowK).get().fix1(IOOf::narrowK).unsafeRunSync());

    assertEquals("could not acquire resource", error.getMessage());
    verify(resource, never()).close();
  }

  @Test
  public void optionTBracketUseError() throws Exception {
    Higher1<Higher1<OptionT_, IO_>, String> bracket =
        optionTMonadDefer.bracket(OptionT.some(IOInstances.monad(), resource),
                                  r -> OptionT.<IO_, String>none(IOInstances.monad()));

    NoSuchElementException error = assertThrows(NoSuchElementException.class,
                 () -> bracket.fix1(OptionTOf::narrowK).get().fix1(IOOf::narrowK).unsafeRunSync());

    assertEquals("get() in none", error.getMessage());
    verify(resource).close();
  }

  @Test
  public void futureBracket() throws Exception {
    Higher1<Future_, String> bracket =
        futureMonadDefer.bracket(Future.success(resource), r -> Future.success("done"));

    Future<String> result = bracket.fix1(FutureOf::narrowK).orElse(Future.success("fail"));

    assertEquals(Try.success("done"), result.await());
    verify(resource).close();
  }

  @Test
  public void futureBracketAcquireError() throws Exception {
    Higher1<Future_, String> bracket =
        futureMonadDefer.bracket(Future.<AutoCloseable>failure(new IllegalStateException()),
                                 r -> Future.success("done"));

    Future<String> result = bracket.fix1(FutureOf::narrowK).orElse(Future.success("fail"));

    assertEquals(Try.success("fail"), result.await());
    verify(resource, never()).close();
  }

  @Test
  public void futureBracketUseError() throws Exception {
    Higher1<Future_, String> bracket =
        futureMonadDefer.bracket(Future.success(resource),
                                 r -> Future.<String>failure(new UnsupportedOperationException()));

    Future<String> result = bracket.fix1(FutureOf::narrowK).orElse(Future.success("fail"));

    assertEquals(Try.success("fail"), result.await());
    verify(resource).close();
  }

  @Test
  public void zioBracket() throws Exception {
    Higher1<Higher1<Higher1<ZIO_, Nothing>, Throwable>, String> bracket =
        zioMonadDefer.bracket(ZIO.<Nothing, Throwable, AutoCloseable>pure(resource),
                              r -> ZIO.<Nothing, Throwable, String>pure("done"));

    Either<Throwable, String> result = bracket.fix1(ZIOOf::narrowK).provide(nothing());

    assertEquals(Either.right("done"), result);
    verify(resource).close();
  }

  @Test
  public void zioBracketAcquireError() throws Exception {
    Higher1<Higher1<Higher1<ZIO_, Nothing>, Throwable>, String> bracket =
        zioMonadDefer.bracket(ZIO.<Nothing, Throwable, AutoCloseable>raiseError(new IllegalStateException()),
                              r -> ZIO.<Nothing, Throwable, String>pure("done"));

    Either<Throwable, String> result = bracket.fix1(ZIOOf::narrowK).provide(nothing());

    assertTrue(result.isLeft());
    verify(resource, never()).close();
  }

  @Test
  public void zioBracketUseError() throws Exception {
    Higher1<Higher1<Higher1<ZIO_, Nothing>, Throwable>, String> bracket =
        zioMonadDefer.bracket(ZIO.<Nothing, Throwable, AutoCloseable>pure(resource),
                              r -> ZIO.<Nothing, Throwable, String>raiseError(new UnsupportedOperationException()));

    Either<Throwable, String> result = bracket.fix1(ZIOOf::narrowK).provide(nothing());

    assertTrue(result.isLeft());
    verify(resource).close();
  }
}
