# Zeromock Core

This module is the core of the zeromock project. It defines all the basic classes and interfaces 
used in the rest of the project.

Initially the module only holds a few basic interfaces and it has grown to become an entire
functional programming library (well, a humble one).

Also, I have to say that this library is largely inspired in vavr library (thanks to the author)
 and also in scala standard library.

## Data types

All this data types implements this methods: `get`, `map`, `flatMap`, `filter`, `fold` and `flatten`.

### Option

Is an alternative to `Optional` of Java standard library. It can contains two values, a `some` or a `none`

```
Option<String> some = Option.some("Hello world");

Option<String> none = Option.none();
```

### Try

Is an implementation of scala `Try` in Java. It can contains two values, a `success` or a `failure`.

```
Try<String> success = Try.success("Hello world");

Try<String> failure = Try.failure(new RuntimeException("Error"));
```

### Either

Is an implementation of scala `Either` in Java.

```
Either<Integer, String> right = Either.right("Hello world");

Either<Integer, String> left = Either.left(100);
```

### Validation

This type represents two different states, valid or invalid, an also it allows to combine several
validations using `map2` to `map5` methods.

```
Validation<String, String> name = Validation.valid("John Smith");
Validation<String, String> email = Validation.valid("john.smith@example.net");

Valdation<String, Person> person = Valiidation.map2(name, email, Person::new); // Person has a constructor with two String parameters, name and email.
```

## Tuples

These classes allow to hold some values together, as tuples. There are tuples from 1 to 5.

```
Tuple1<String> tuple1 = Tuple1.of("Hello world");

Tuple2<String, Integer> tuple2 = Tuple2.of("John Smith", 100);
```

## Data structures

Java doesn't define immutable collections, so I have implemented some of them.

### Sequence

Is the equivalent to java `Collection` interface. It defines all the common methods.

### ImmutableList

It represents a linked list. It has a head and a tail.

### ImmutableSet

It represents a set of elements. This elements cannot be duplicated.

### ImmutableArray

TODO: not implemented yet

It represents an array. You can access to the elements by its position in the array.

### ImmutableMap

This class represents a hash map.

### ImmutableTree

TODO: not implemented yet

This class represents a binary tree.

## Monads

Also I have implemented some Monads that allows to combine some operations.

### State Monad

Is the traditional State Modad from FP languages, like Haskel or Scala. It allows to combine 
operations over a state. The state should be a immutable class. It recives an state and generates
a tuple with the new state and an intermediate result.

```
State<ImmutableList<String>, Option<String>> read = State.state(list -> Tuple2.of(list.tail(), list.head()));
  
Tuple2<ImmutableList<String>, Option<String>> result = read.run(ImmutableList.of("a", "b", "c"));
    
assertEquals(Tuple2.of(ImmutableList.of("b", "c"), Option.some("a")), result);
```

### Reader Monad

This is an implementation of Reader Monad. It allows to combine operations over a common input.
It can be used to inject dependencies.

```
Reader<ImmutableList<String>, String> read2 = Reader.reader(list -> list.tail().head().orElse(""));

String result = read2.eval(ImmutableList.of("a", "b", "c"));

assertEqual("b", result);
```
