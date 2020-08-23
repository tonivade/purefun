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
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.runtimes.ConsoleExecutor;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.FunctionK;

public class FreeAlgTest {

  private static Free<Kind<Kind<EitherK_, ConsoleAlg_>, EmailAlg_>, String> read() {
    return Free.inject(injectEitherKLeft(), new ConsoleAlg.ReadLine());
  }

  private static Free<Kind<Kind<EitherK_, ConsoleAlg_>, EmailAlg_>, Unit> write(String value) {
    return Free.inject(injectEitherKLeft(), new ConsoleAlg.WriteLine(value));
  }

  private static Free<Kind<Kind<EitherK_, ConsoleAlg_>, EmailAlg_>, Unit> send(String to, String content) {
    return Free.inject(injectEitherKRight(injectReflexive()), new EmailAlg.SendEmail(to, content));
  }

  @Test
  public void algebra() {
    Free<Kind<Kind<EitherK_, ConsoleAlg_>, EmailAlg_>, Unit> hello =
        read().flatMap(name -> write("hello " + name))
            .andThen(send("toni@home", "hello"));

    ConsoleExecutor executor = new ConsoleExecutor().read("toni");

    executor.run(hello.foldMap(IOInstances.monad(), interpreter()).fix(IOOf::narrowK));

    assertEquals("hello toni\nemail to toni@home with content hello\n", executor.getOutput());
  }

  @SuppressWarnings("unchecked")
  private static FunctionK<Kind<Kind<EitherK_, ConsoleAlg_>, EmailAlg_>, IO_> interpreter() {
    final Console<IO_> console = IOInstances.console();
    return new FunctionK<Kind<Kind<EitherK_, ConsoleAlg_>, EmailAlg_>, IO_>() {
      @Override
      public <T> Kind<IO_, T> apply(Kind<Kind<Kind<EitherK_, ConsoleAlg_>, EmailAlg_>, T> from) {
        return from.fix(EitherKOf::narrowK).foldK(
          new FunctionK<ConsoleAlg_, IO_>() {
            @Override
            public <X> Kind<IO_, X> apply(Kind<ConsoleAlg_, X> kind) {
              ConsoleAlg<X> consoleAlg = kind.fix(ConsoleAlgOf::narrowK);
              if (consoleAlg instanceof ConsoleAlg.ReadLine) {
                return (Kind<IO_, X>) console.readln();
              }
              if (consoleAlg instanceof ConsoleAlg.WriteLine) {
                ConsoleAlg.WriteLine writeLine = (ConsoleAlg.WriteLine) consoleAlg;
                return (Kind<IO_, X>) console.println(writeLine.getLine());
              }
              throw new IllegalStateException();
            }
          },
            new FunctionK<EmailAlg_, IO_>() {
              @Override
              public <X> Kind<IO_, X> apply(Kind<EmailAlg_, X> kind) {
                EmailAlg<X> emailAlg = kind.fix(EmailAlgOf::narrowK);
                if (emailAlg instanceof EmailAlg.SendEmail) {
                  EmailAlg.SendEmail sendEmail = (EmailAlg.SendEmail) emailAlg;
                  return (Kind<IO_, X>) console.println(
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

