package com.github.tonivade.zeromock.core;

import static com.github.tonivade.zeromock.core.Try.success;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TryTest {
  @Test
  void testName() {
    Handler1<String, String> toUpperCase = string -> string.toUpperCase();
    
    Try<String> try1 = success("Hola mundo").map(toUpperCase);
    
    assertEquals(Try.success("HOLA MUNDO"), try1);
  }
}
