/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Operator2;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monoid;
import com.github.tonivade.purefun.typeclasses.Traverse;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

@HigherKind
public final class Cofree<F extends Kind, A> {

  private final Functor<F> functor;
  private final A head;
  private final Eval<Higher1<F, Cofree<F, A>>> tail;

  private Cofree(Functor<F> functor, A head, Eval<Higher1<F, Cofree<F, A>>> tail) {
    this.functor = checkNonNull(functor);
    this.head = checkNonNull(head);
    this.tail = checkNonNull(tail);
  }

  public A extract() {
    return head;
  }

  public Higher1<F, Cofree<F, A>> tailForced() {
    return tail.value();
  }

  public Cofree<F, A> runTail() {
    return of(functor, head, Eval.now(tail.value()));
  }

  public Cofree<F, A> run() {
    return of(functor, head, Eval.now(transformTail(Cofree::run).value()));
  }

  public <B> Cofree<F, B> map(Function1<A, B> mapper) {
    return transform(mapper, c -> c.map(mapper));
  }

  public <B> Cofree<F, B> coflatMap(Function1<Cofree<F, A>, B> mapper) {
    return of(functor, mapper.apply(this), transformTail(c -> c.coflatMap(mapper)));
  }

  // XXX: remove eval applicative instance parameter, if instances project is added then cyclic dependency problem
  public <B> Eval<B> fold(Applicative<Eval.µ> applicative, Traverse<F> traverse, Function2<A, Higher1<F, B>, Eval<B>> mapper) {
    Eval<Higher1<F, B>> eval =
        traverse.traverse(applicative, tailForced(), c -> c.fold(applicative, traverse, mapper).kind1())
            .fix1(Eval::narrowK);
    return eval.flatMap(fb -> mapper.apply(extract(), fb));
  }

  public <B> Eval<B> reduce(Applicative<Eval.µ> applicative, Traverse<F> traverse,
                            Function1<A, B> initial, Operator2<B> combine) {
    return fold(applicative, traverse,
        (a, fb) -> Eval.later(() -> traverse.fold(Monoid.of(initial.apply(a), combine), fb)));
  }

  public Eval<String> reduceToString(Applicative<Eval.µ> applicative, Traverse<F> traverse, Operator2<String> join) {
    return reduce(applicative, traverse, String::valueOf, join);
  }

  public <B> Cofree<F, B> transform(Function1<A, B> headMap, Function1<Cofree<F, A>, Cofree<F, B>> tailMap) {
    return of(functor, transformHead(headMap), transformTail(tailMap));
  }

  private <B> B transformHead(Function1<A, B> headMap) {
    return headMap.apply(head);
  }

  private <B> Eval<Higher1<F, Cofree<F, B>>> transformTail(Function1<Cofree<F, A>, Cofree<F, B>> tailMap) {
    return tail.map(t -> functor.map(t, tailMap));
  }

  public static <F extends Kind, A> Cofree<F, A> unfold(Functor<F> functor, A head, Function1<A, Higher1<F, A>> unfold) {
    return of(functor, head, Eval.later(() -> functor.map(unfold.apply(head), a -> unfold(functor, a, unfold))));
  }

  public static <F extends Kind, A> Cofree<F, A> of(Functor<F> functor, A head, Eval<Higher1<F, Cofree<F, A>>> tail) {
    return new Cofree<>(functor, head, tail);
  }
}
