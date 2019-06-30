/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.instances.EitherTInstances;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.transformer.EitherT;

public class MonadDeferTest {

  private MonadDefer<IO.µ> ioMonadDefer = IOInstances.monadDefer();
  private MonadDefer<Future.µ> futureMonadDefer = FutureInstances.monadDefer();
  private MonadDefer<Higher1<Higher1<EitherT.µ, IO.µ>, Throwable>> eitherTMonadDefer =
      EitherTInstances.monadDefer(ioMonadDefer);

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
  public void eitherTBracket() throws Exception {
    Higher1<Higher1<Higher1<EitherT.µ, IO.µ>, Throwable>, String> bracket =
        eitherTMonadDefer.bracket(EitherT.right(IOInstances.monad(), resource),
                                  r -> EitherT.right(IOInstances.monad(), "done"));

    String result = bracket.fix1(EitherT::narrowK).get().fix1(IO::narrowK).unsafeRunSync();

    assertEquals("done", result);
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
}
