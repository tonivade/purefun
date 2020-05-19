/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.instances.EitherKInstances.injectEitherKLeft;
import static com.github.tonivade.purefun.instances.EitherKInstances.injectEitherKRight;
import static com.github.tonivade.purefun.typeclasses.InjectK.injectReflexive;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.runtimes.ConsoleExecutor;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.FunctionK;

public class FreeAlgTest {

  private static Free<Higher1<Higher1<EitherK_, ConsoleAlg_>, EmailAlg_>, String> read() {
    return Free.inject(injectEitherKLeft(), new ConsoleAlg.ReadLine());
  }

  private static Free<Higher1<Higher1<EitherK_, ConsoleAlg_>, EmailAlg_>, Unit> write(String value) {
    return Free.inject(injectEitherKLeft(), new ConsoleAlg.WriteLine(value));
  }

  private static Free<Higher1<Higher1<EitherK_, ConsoleAlg_>, EmailAlg_>, Unit> send(String to, String content) {
    return Free.inject(injectEitherKRight(injectReflexive()), new EmailAlg.SendEmail(to, content));
  }

  @Test
  public void algebra() {
    Free<Higher1<Higher1<EitherK_, ConsoleAlg_>, EmailAlg_>, Unit> hello =
        read().flatMap(name -> write("hello " + name))
            .andThen(send("toni@home", "hello"));

    ConsoleExecutor executor = new ConsoleExecutor().read("toni");

    executor.run(hello.foldMap(IOInstances.monad(), interpreter()).fix1(IOOf::narrowK));

    assertEquals("hello toni\nemail to toni@home with content hello\n", executor.getOutput());
  }

  @SuppressWarnings("unchecked")
  private static FunctionK<Higher1<Higher1<EitherK_, ConsoleAlg_>, EmailAlg_>, IO_> interpreter() {
    final Console<IO_> console = IOInstances.console();
    return new FunctionK<Higher1<Higher1<EitherK_, ConsoleAlg_>, EmailAlg_>, IO_>() {
      @Override
      public <T> Higher1<IO_, T> apply(Higher1<Higher1<Higher1<EitherK_, ConsoleAlg_>, EmailAlg_>, T> from) {
        return from.fix1(EitherKOf::narrowK).foldK(
          new FunctionK<ConsoleAlg_, IO_>() {
            @Override
            public <X> Higher1<IO_, X> apply(Higher1<ConsoleAlg_, X> from) {
              ConsoleAlg<X> consoleAlg = from.fix1(ConsoleAlgOf::narrowK);
              if (consoleAlg instanceof ConsoleAlg.ReadLine) {
                return (Higher1<IO_, X>) console.readln();
              }
              if (consoleAlg instanceof ConsoleAlg.WriteLine) {
                ConsoleAlg.WriteLine writeLine = (ConsoleAlg.WriteLine) consoleAlg;
                return (Higher1<IO_, X>) console.println(writeLine.getLine());
              }
              throw new IllegalStateException();
            }
          },
            new FunctionK<EmailAlg_, IO_>() {
              @Override
              public <X> Higher1<IO_, X> apply(Higher1<EmailAlg_, X> from) {
                EmailAlg<X> emailAlg = from.fix1(EmailAlgOf::narrowK);
                if (emailAlg instanceof EmailAlg.SendEmail) {
                  EmailAlg.SendEmail sendEmail = (EmailAlg.SendEmail) emailAlg;
                  return (Higher1<IO_, X>) console.println(
                      "email to " + sendEmail.getTo() + " with content " + sendEmail.getContent());
                }
                throw new IllegalStateException();
              }
            }
        );
      }
    };
  }
}

