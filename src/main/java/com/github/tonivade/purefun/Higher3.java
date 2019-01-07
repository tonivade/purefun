/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Higher3<F extends Kind, A, B, C> extends Higher2<Higher1<F, A>, B, C>, Higher1<Higher1<Higher1<F, A>, B>, C> {

}
