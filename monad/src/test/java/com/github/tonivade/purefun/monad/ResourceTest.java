/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.instances.IOInstances.monadDefer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Kind;

@ExtendWith(MockitoExtension.class)
class ResourceTest {

  @Test
  void use(@Mock Consumer1<String> release) {
    Resource<IO_, String> resource = Resource.from(monadDefer(), IO.pure("hola"), release);
    
    Kind<IO_, String> use = resource.use(string -> IO.pure(string.toUpperCase()));
    
    assertEquals("HOLA", use.fix(IOOf::narrowK).unsafeRunSync());
    verify(release).accept("hola");
  }
  
  @Test
  void map(@Mock Consumer1<String> release) {
    Resource<IO_, String> resource = 
        Resource.from(monadDefer(), IO.pure("hola"), release).map(String::toUpperCase);
    
    Kind<IO_, Integer> use = resource.use(string -> IO.pure(string.length()));
    
    assertEquals(4, use.fix(IOOf::narrowK).unsafeRunSync());
    verify(release).accept("hola");
  }
}
