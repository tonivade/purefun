/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface Fixer<T extends Witness, R> {

  R apply(T hkt);

}
