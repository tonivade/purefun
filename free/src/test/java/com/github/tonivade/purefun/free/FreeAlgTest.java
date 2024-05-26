/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.instances.EitherKInstances.injectEitherKLeft;
import static com.github.tonivade.purefun.instances.EitherKInstances.injectEitherKRight;
import static com.github.tonivade.purefun.typeclasses.InjectK.injectReflexive;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.runtimes.ConsoleExecutor;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Instances;
import org.junit.jupiter.api.Test;

public class FreeAlgTest {

  private static Free<EitherK<ConsoleAlg<?>, EmailAlg<?>, ?>, String> read() {
    return Free.inject(injectEitherKLeft(), new ConsoleAlg.ReadLine());
  }

  private static Free<EitherK<ConsoleAlg<?>, EmailAlg<?>, ?>, Unit> write(String value) {
    return Free.inject(injectEitherKLeft(), new ConsoleAlg.WriteLine(value));
  }

  private static Free<EitherK<ConsoleAlg<?>, EmailAlg<?>, ?>, Unit> send(String to, String content) {
    return Free.inject(injectEitherKRight(injectReflexive()), new EmailAlg.SendEmail(to, content));
  }

  @Test
  public void algebra() {
    var hello = read()
        .flatMap(name -> write("hello " + name))
        .andThen(send("toni@home", "hello"));

    ConsoleExecutor executor = new ConsoleExecutor().read("toni");

    IO<Unit> fix = hello.foldMap(Instances.<IO<?>>monad(), interpreter()).fix(IOOf::toIO);
    executor.run(fix);

    assertEquals("hello toni\nemail to toni@home with content hello\n", executor.getOutput());
  }

  @SuppressWarnings("unchecked")
  private static FunctionK<EitherK<ConsoleAlg<?>, EmailAlg<?>, ?>, IO<?>> interpreter() {
    final Console<IO<?>> console = Instances.console();
    return new FunctionK<>() {
      @Override
      public <T> Kind<IO<?>, T> apply(Kind<EitherK<ConsoleAlg<?>, EmailAlg<?>, ?>, ? extends T> from) {
        return from.fix(EitherKOf::<ConsoleAlg<?>, EmailAlg<?>, T>toEitherK).foldK(
          new FunctionK<>() {
            @Override
            public <X> IO<X> apply(Kind<ConsoleAlg<?>, ? extends X> kind) {
              return (IO<X>) switch(kind.fix(ConsoleAlgOf::toConsoleAlg)) {
                case ConsoleAlg.ReadLine() -> console.readln();
                case ConsoleAlg.WriteLine(var line) -> console.println(line);
              };
            }
          },
            new FunctionK<>() {
              @Override
              public <X> IO<X> apply(Kind<EmailAlg<?>, ? extends X> kind) {
                return (IO<X>) switch (kind.fix(EmailAlgOf::toEmailAlg)) {
                case EmailAlg.SendEmail(var to, var content)
                  -> console.println("email to " + to + " with content " + content);
                };
              }
            }
        );
      }
    };
  }
}

