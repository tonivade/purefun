/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.monad.IO.FiberIO;

@ExtendWith(MockitoExtension.class)
public class IOFiberTest {
  
  @Test
  public void fiber(@Mock Consumer1<String> c1, @Mock Consumer1<String> c2) {
    IO<FiberIO<String>> fork1 = IO.delay(() -> {
      c1.accept(Thread.currentThread().getName());
      return "hola";
    }).fork();
    IO<FiberIO<String>> fork2 = IO.delay(() -> {
      c2.accept(Thread.currentThread().getName());
      return "adios";
    }).fork();
    
    IO<Tuple2<String, String>> flatMap = 
        fork1.flatMap(
          fiber1 -> fork2.flatMap(
              fiber2 -> fiber1.join().flatMap(
                  a -> fiber2.join().map(
                      b -> Tuple.of(a, b)))));
    
    assertEquals(Tuple.of("hola", "adios"), flatMap.unsafeRunSync());
    verify(c1).accept(startsWith("pool-1-thread-"));
    verify(c2).accept(startsWith("pool-1-thread-"));
  }
}
