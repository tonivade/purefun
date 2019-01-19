/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Matcher1.always;
import static com.github.tonivade.purefun.Matcher1.is;
import static com.github.tonivade.purefun.Producer.cons;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;

public class MonadErrorTest {

  private final MonadError<Try.µ, Throwable> monadError = Try.monadError();
  
  @Test
  public void recover() {
    Higher1<Try.µ, String> recover = 
        monadError.recover(Try.failure("error"), PartialFunction1.of(Throwable::toString, always()));
    
    assertEquals(Try.success("java.lang.Exception: error"), recover);
  }
  
  @Test
  public void attempRight() {
    Higher1<Try.µ, Either<Throwable, String>> attemp = monadError.attemp(Try.success("hola mundo!"));

    assertEquals(Try.success(Either.right("hola mundo!")), attemp);
  }
  
  @Test
  public void attempLeft() {
    Exception error = new Exception("error");

    Higher1<Try.µ, Either<Throwable, String>> attemp = monadError.attemp(Try.failure(error));

    assertEquals(Try.success(Either.left(error)), attemp);
  }
  
  @Test
  public void ensureError() {
    Exception error = new Exception("error");

    Higher1<Try.µ, String> ensure = 
        monadError.ensure(Try.success("not ok"), cons(error), is("ok"));

    assertEquals(Try.failure(error), ensure);
  }
  
  @Test
  public void ensureOk() {
    Exception error = new Exception("error");

    Higher1<Try.µ, String> ensure = 
        monadError.ensure(Try.success("ok"), cons(error), is("ok"));

    assertEquals(Try.success("ok"), ensure);
  }
}
