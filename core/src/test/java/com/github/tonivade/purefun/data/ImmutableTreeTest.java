/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.data.ImmutableList.toImmutableList;
import static com.github.tonivade.purefun.data.ImmutableTree.toImmutableTree;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.data.Sequence.treeOf;
import static java.util.Collections.emptyNavigableSet;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.type.Option;

public class ImmutableTreeTest {

  private final Function1<String, String> toUpperCase = String::toUpperCase;

  @Test
  public void notEmptyTree() {
    ImmutableTree<String> tree = ImmutableTree.of("c", "b", "a");

    assertAll(() -> assertEquals(3, tree.size()),
              () -> assertFalse(tree.isEmpty()),
              () -> assertTrue(tree.contains("a")),
              () -> assertFalse(tree.contains("z")),
              () -> assertEquals(tree, tree.reverse()),
              () -> assertEquals(ImmutableTree.of("a", "b", "c"), tree),
              () -> assertEquals(ImmutableTree.of("a", "b", "c"), tree.reverse()),
              () -> assertEquals("abc", tree.fold("", (a, b) -> a + b)),
              () -> assertEquals("cba", tree.reverse().fold("", (a, b) -> a + b)),
              () -> assertEquals("abc", tree.foldRight("", (a, b) -> a + b)),
              () -> assertEquals("cba", tree.reverse().foldRight("", (a, b) -> a + b)),
              () -> assertEquals("abc", tree.foldLeft("", (a, b) -> a + b)),
              () -> assertEquals("cba", tree.reverse().foldLeft("", (a, b) -> a + b)),
              () -> assertEquals(Option.some("abc"), tree.reduce((a, b) -> a + b)),
              () -> assertEquals(treeOf("a", "b", "c"), tree),
              () -> assertEquals(ImmutableTree.from(Arrays.asList("a", "b", "c")), tree),
              () -> assertEquals(new TreeSet<>(Arrays.asList("a", "b", "c")), tree.toNavigableSet()),
              () -> assertEquals(treeOf("a", "b", "c"), tree.append("c")),
              () -> assertEquals(treeOf("a", "b", "c", "z"), tree.append("z")),
              () -> assertEquals(treeOf("a", "b"), tree.remove("c")),
              () -> assertEquals(treeOf("a", "b", "c"), tree.remove("z")),
              () -> assertEquals(treeOf("a", "b", "c"), tree.map(identity())),
              () -> assertEquals(treeOf("A", "B", "C"), tree.map(toUpperCase)),
              () -> assertEquals(treeOf("A", "B", "C"), tree.flatMap(toUpperCase.sequence())),
              () -> assertEquals(treeOf("a", "b", "c"), tree.filter(e -> e.length() > 0)),
              () -> assertEquals(ImmutableTree.empty(), tree.filter(e -> e.length() > 1)),
              () -> assertEquals(Option.some("a"), tree.head()),
              () -> assertEquals(Option.some("c"), tree.tail()),
              () -> assertEquals(treeOf("b", "c"), tree.tailTree("a")),
              () -> assertEquals(treeOf("a", "b"), tree.headTree("c")),
              () -> assertEquals(Option.some("b"), tree.higher("a")),
              () -> assertEquals(Option.none(), tree.higher("c")),
              () -> assertEquals(Option.none(), tree.lower("a")),
              () -> assertEquals(Option.some("b"), tree.lower("c")),
              () -> assertEquals(Option.some("a"), tree.floor("a")),
              () -> assertEquals(Option.some("c"), tree.floor("c")),
              () -> assertEquals(Option.some("a"), tree.ceiling("a")),
              () -> assertEquals(Option.some("c"), tree.ceiling("c")),
              () -> assertEquals(tree, tree.stream().collect(toImmutableTree())),
              () -> assertEquals(listOf(Tuple.of(0, "a"), Tuple.of(1, "b"), Tuple.of(2, "c")),
                  tree.zipWithIndex().collect(toImmutableList())),
              () -> assertThrows(UnsupportedOperationException.class, tree.iterator()::remove)
              );
  }

  @Test
  public void emptyTree() {
    ImmutableTree<String> tree = ImmutableTree.empty();

    assertAll(() -> assertEquals(0, tree.size()),
              () -> assertTrue(tree.isEmpty()),
              () -> assertFalse(tree.contains("z")),
              () -> assertEquals(tree, tree.reverse()),
              () -> assertEquals("", tree.fold("", (a, b) -> a + b)),
              () -> assertEquals("", tree.reverse().fold("", (a, b) -> a + b)),
              () -> assertEquals(Option.none(), tree.reduce((a, b) -> a + b)),
              () -> assertEquals(ImmutableTree.empty(), tree),
              () -> assertEquals(ImmutableTree.from(Collections.emptyList()), tree),
              () -> assertEquals(emptyNavigableSet(), tree.toNavigableSet()),
              () -> assertEquals(treeOf("z"), tree.append("z")),
              () -> assertEquals(ImmutableTree.empty(), tree.remove("c")),
              () -> assertEquals(ImmutableTree.empty(), tree.map(identity())),
              () -> assertEquals(ImmutableTree.empty(), tree.map(toUpperCase)),
              () -> assertEquals(ImmutableTree.empty(), tree.flatMap(toUpperCase.sequence())),
              () -> assertEquals(ImmutableTree.empty(), tree.filter(e -> e.length() > 1)),
              () -> assertEquals(Option.none(), tree.head()),
              () -> assertEquals(Option.none(), tree.tail()),
              () -> assertEquals(ImmutableTree.empty(), tree.tailTree("a")),
              () -> assertEquals(ImmutableTree.empty(), tree.headTree("c")),
              () -> assertEquals(Option.none(), tree.higher("a")),
              () -> assertEquals(Option.none(), tree.higher("c")),
              () -> assertEquals(Option.none(), tree.lower("a")),
              () -> assertEquals(Option.none(), tree.lower("c")),
              () -> assertEquals(Option.none(), tree.floor("a")),
              () -> assertEquals(Option.none(), tree.floor("c")),
              () -> assertEquals(Option.none(), tree.ceiling("a")),
              () -> assertEquals(Option.none(), tree.ceiling("c"))
              );
  }

  @Test
  void serialization() throws IOException, ClassNotFoundException {
    ImmutableTree<Integer> tree = treeOf(1, 2, 3, 4, 5);

    var output = new ByteArrayOutputStream();
    try (var objectOutputStream = new ObjectOutputStream(output)) {
      objectOutputStream.writeObject(tree);
      objectOutputStream.writeObject(Sequence.emptyTree());
    }

    Object result = null;
    Object empty = null;
    try (var objectInputStream = new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()))) {
      result = objectInputStream.readObject();
      empty = objectInputStream.readObject();
    }

    assertEquals(tree, result);
    assertSame(Sequence.emptyTree(), empty);
  }
}
