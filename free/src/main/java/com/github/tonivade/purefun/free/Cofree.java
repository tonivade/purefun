/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.type.EvalOf.toEval;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Operator2;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Eval_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monoid;
import com.github.tonivade.purefun.typeclasses.Traverse;

@HigherKind
public final class Cofree<F extends Witness, A> implements CofreeOf<F, A> {

  private final Functor<F> functor;
  private final A head;
  private final Eval<Kind<F, Cofree<F, A>>> tail;

  private Cofree(Functor<F> functor, A head, Eval<Kind<F, Cofree<F, A>>> tail) {
    this.functor = checkNonNull(functor);
    this.head = checkNonNull(head);
    this.tail = checkNonNull(tail);
  }

  public A extract() {
    return head;
  }

  public Kind<F, Cofree<F, A>> tailForced() {
    return tail.value();
  }

  public Cofree<F, A> runTail() {
    return of(functor, head, Eval.now(tail.value()));
  }

  public Cofree<F, A> run() {
    Eval<Kind<F, Cofree<F, A>>> transformTail = transformTail(Cofree::run);
    return of(functor, head, Eval.now(transformTail.value()));
  }

  public <B> Cofree<F, B> map(Function1<? super A, ? extends B> mapper) {
    return transform(mapper, c -> c.map(mapper));
  }

  public <B> Cofree<F, B> coflatMap(Function1<? super Cofree<F, ? extends A>, ? extends B> mapper) {
    return of(functor, mapper.apply(this), transformTail(c -> c.coflatMap(mapper)));
  }

  // XXX: remove eval applicative instance parameter, if instances project is added then cyclic dependency problem
  public <B> Eval<B> fold(Applicative<Eval_> applicative, Traverse<F> traverse, Function2<A, Kind<F, B>, Eval<B>> mapper) {
    Eval<Kind<F, B>> eval =
        traverse.traverse(applicative, tailForced(), c -> c.fold(applicative, traverse, mapper))
            .fix(toEval());
    return eval.flatMap(fb -> mapper.apply(extract(), fb));
  }

  public <B> Eval<B> reduce(Applicative<Eval_> applicative, Traverse<F> traverse,
                            Function1<A, B> initial, Operator2<B> combine) {
    return fold(applicative, traverse,
        (a, fb) -> Eval.later(() -> traverse.fold(Monoid.of(initial.apply(a), combine), fb)));
  }

  public Eval<String> reduceToString(Applicative<Eval_> applicative, Traverse<F> traverse, Operator2<String> join) {
    return reduce(applicative, traverse, String::valueOf, join);
  }

  public <B> Cofree<F, B> transform(Function1<? super A, ? extends B> headMap, 
      Function1<? super Cofree<F, ? extends A>, ? extends Cofree<F, ? extends B>> tailMap) {
    return of(functor, transformHead(headMap), transformTail(tailMap));
  }

  private <B> B transformHead(Function1<? super A, ? extends B> headMap) {
    return headMap.apply(head);
  }

  private <B> Eval<Kind<F, Cofree<F, B>>> transformTail(
      Function1<? super Cofree<F, ? extends A>, ? extends Cofree<F, ? extends B>> tailMap) {
    return tail.map(t -> functor.map(t, tailMap.andThen(CofreeOf::narrowK)));
  }

  public static <F extends Witness, A> Cofree<F, A> unfold(Functor<F> functor, A head, Function1<A, Kind<F, A>> unfold) {
    return of(functor, head, Eval.later(() -> functor.map(unfold.apply(head), a -> unfold(functor, a, unfold))));
  }

  public static <F extends Witness, A> Cofree<F, A> of(Functor<F> functor, A head, Eval<Kind<F, Cofree<F, A>>> tail) {
    return new Cofree<>(functor, head, tail);
  }
}
