/*
 * Copyright 2016 Daniel Skogquist Åborg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.d2ab.sequence.user;

import org.d2ab.collection.Maps;
import org.d2ab.sequence.*;
import org.d2ab.util.Pair;
import org.junit.Test;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Character.toUpperCase;
import static java.lang.Math.sqrt;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class SequenceDocumentationTest {
	@Test
	public void filterAndMap() {
		List<String> evens =
				Sequence.of(1, 2, 3, 4, 5, 6, 7, 8, 9).filter(x -> x % 2 == 0).map(Object::toString).toList();

		assertThat(evens, contains("2", "4", "6", "8"));
	}

	@Test
	public void reuseOfSequence() {
		Sequence<Integer> singulars = Sequence.range(1, 9); // Digits 1..9

		// using sequence of ints 1..9 first time to get odd numbers between 1 and 9
		Sequence<Integer> odds = singulars.step(2);
		assertThat(odds, contains(1, 3, 5, 7, 9));

		// re-using the same sequence again to get squares of numbers between 4 and 8
		Sequence<Integer> squares = singulars.startingFrom(4).endingAt(8).map(i -> i * i);
		assertThat(squares, contains(16, 25, 36, 49, 64));
	}

	@Test
	public void sequenceInForeach() {
		Sequence<Integer> sequence = Sequence.ints().limit(5);

		int x = 1;
		for (int i : sequence)
			assertThat(i, is(x++));

		assertThat(x, is(6));
	}

	@Test
	public void functionalInterface() {
		List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);

		// Sequence as @FunctionalInterface of list's Iterator
		Sequence<Integer> sequence = list::iterator;

		// Operate on sequence as any other sequence using default methods
		Sequence<String> transformed = sequence.map(Object::toString);

		assertThat(transformed.limit(3), contains("1", "2", "3"));
	}

	@Test
	public void fromIterator() {
		Iterator<Integer> iterator = Arrays.asList(1, 2, 3, 4, 5).iterator();

		Sequence<Integer> sequence = Sequence.once(iterator);

		assertThat(sequence, contains(1, 2, 3, 4, 5));
		assertThat(sequence, is(emptyIterable()));
	}

	@Test
	public void caching() {
		Iterator<Integer> iterator = Arrays.asList(1, 2, 3, 4, 5).iterator();

		Sequence<Integer> sequence = Sequence.cache(iterator);

		assertThat(sequence, contains(1, 2, 3, 4, 5));
		assertThat(sequence, contains(1, 2, 3, 4, 5));
	}

	@Test
	public void removeAll() {
		List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));

		Sequence.from(list).filter(x -> x % 2 != 0).removeAll();

		assertThat(list, contains(2, 4));
	}

	@Test
	public void updatingCollection() {
		List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));

		Sequence<Integer> sequence = Sequence.from(list).filter(x -> x % 2 == 0);
		assertThat(sequence, contains(2, 4));

		list.add(6);
		assertThat(sequence, contains(2, 4, 6));
	}

	@SuppressWarnings("SpellCheckingInspection")
	@Test
	public void streamToSequenceAndBack() {
		Sequence<String> paired = Sequence.once(Stream.of("a", "b", "c", "d")).pairs().flatten();

		assertThat(paired.stream().collect(Collectors.toList()), contains("a", "b", "b", "c", "c", "d"));
	}

	@Test
	public void fibonacci() {
		Sequence<Integer> fibonacci = Sequence.recurse(Pair.of(0, 1), p -> p.shiftLeft(p.apply(Integer::sum)))
		                                      .map(Pair::getLeft)
		                                      .endingAt(34);

		assertThat(fibonacci, contains(0, 1, 1, 2, 3, 5, 8, 13, 21, 34));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void recurseThrowableCause() {
		Exception e = new IllegalStateException(new IllegalArgumentException(new NullPointerException()));

		Sequence<Throwable> sequence = Sequence.recurse(e, Throwable::getCause).untilNull();

		assertThat(sequence,
		           contains(instanceOf(IllegalStateException.class), instanceOf(IllegalArgumentException.class),
		                    instanceOf(NullPointerException.class)));
	}

	@Test
	public void factorial() {
		Sequence<Long> thirteen = Sequence.longs().limit(13);

		long factorial = thirteen.reduce(1L, (r, i) -> r * i);

		assertThat(factorial, is(6227020800L));
	}

	@Test
	public void toMapFromSeparateSequences() {
		Sequence<Integer> keys = Sequence.of(1, 2, 3);
		Sequence<String> values = Sequence.of("1", "2", "3");

		Sequence<Pair<Integer, String>> keyValueSequence = keys.interleave(values);
		Map<Integer, String> map = keyValueSequence.toMap();

		assertThat(map, is(equalTo(Maps.builder(1, "1").put(2, "2").put(3, "3").build())));
	}

	@Test
	public void toMapFromPairs() {
		Map<String, Integer> map = Maps.builder("1", 1).put("2", 2).put("3", 3).put("4", 4).build();

		Sequence<Pair<String, Integer>> sequence = Sequence.from(map)
		                                                   .map(Pair::from)
		                                                   .filter(p -> p.test((s, i) -> i != 2))
		                                                   .map(p -> p.map((s, i) -> Pair.of(s + " x 2", i * 2)));

		assertThat(sequence.toMap(), is(equalTo(Maps.builder("1 x 2", 2).put("3 x 2", 6).put("4 x 2", 8).build())));
	}

	@Test
	public void entrySequence() {
		Map<String, Integer> original = Maps.builder("1", 1).put("2", 2).put("3", 3).put("4", 4).build();

		EntrySequence<Integer, String> oddsInverted =
				EntrySequence.from(original).filter((k, v) -> v % 2 != 0).map((k, v) -> Maps.entry(v, k));

		assertThat(oddsInverted.toMap(), is(equalTo(Maps.builder(1, "1").put(3, "3").build())));
	}

	@Test
	public void biSequence() {
		BiSequence<String, Integer> presidents =
				BiSequence.ofPairs("Abraham Lincoln", 1861, "Richard Nixon", 1969, "George Bush", 2001, "Barack Obama",
				                   2005);

		Sequence<String> joinedOffice = presidents.toSequence((n, y) -> n + " (" + y + ")");

		assertThat(joinedOffice, contains("Abraham Lincoln (1861)", "Richard Nixon (1969)", "George Bush (2001)",
		                                  "Barack Obama (2005)"));
	}

	@Test
	public void snakeCase() {
		CharSeq snakeCase = CharSeq.from("Hello Lexicon").map(c -> (c == ' ') ? '_' : c).map(Character::toLowerCase);

		assertThat(snakeCase.asString(), is("hello_lexicon"));
	}

	@Test
	public void intSequence() {
		IntSequence squares = IntSequence.positive().map(i -> i * i);

		assertThat(squares.limit(5), contains(1, 4, 9, 16, 25));
	}

	@Test
	public void longSequence() {
		LongSequence negativeOdds = LongSequence.negative().step(2);

		assertThat(negativeOdds.limit(5), contains(-1L, -3L, -5L, -7L, -9L));
	}

	@Test
	public void doubleSequence() {
		DoubleSequence squareRoots = IntSequence.positive().toDoubles().map(Math::sqrt);

		assertThat(squareRoots.limit(3), contains(sqrt(1), sqrt(2), sqrt(3)));
	}

	@Test
	public void capitalize() {
		CharSeq titleCase = CharSeq.from("hello_lexicon")
		                           .mapBack('_', (p, c) -> p == '_' ? toUpperCase(c) : c)
		                           .map(c -> (c == '_') ? ' ' : c);

		assertThat(titleCase.asString(), is("Hello Lexicon"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void partition() {
		Sequence<Sequence<Integer>> batched = Sequence.of(1, 2, 3, 4, 5, 6, 7, 8, 9).batch(3);

		assertThat(batched, contains(contains(1, 2, 3), contains(4, 5, 6), contains(7, 8, 9)));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void partitionConsonantsVowels() {
		String vowels = "aeoiuy";

		Sequence<String> consonantsVowels = CharSeq.from("terrain")
		                                           .batch((a, b) -> (vowels.indexOf(a) == -1) !=
		                                                            (vowels.indexOf(b) == -1))
		                                           .map(CharSeq::asString);

		assertThat(consonantsVowels, contains("t", "e", "rr", "ai", "n"));
	}

	@Test
	public void readReader() throws IOException {
		Reader reader = new StringReader("hello world\n" + "goodbye world\n");

		Sequence<String> titleCase = CharSeq.read(reader)
		                                    .mapBack('\n',
		                                             (p, n) -> p == '\n' || p == ' ' ? Character.toUpperCase(n) : n)
		                                    .split('\n')
		                                    .map(CharSeq::asString);

		assertThat(titleCase, contains("Hello World", "Goodbye World"));

		reader.close();
	}

	@Test
	public void readInputStream() throws IOException {
		InputStream inputStream = new ByteArrayInputStream(new byte[]{0xD, 0xE, 0xA, 0xD, 0xB, 0xE, 0xE, 0xF});

		String hexString = IntSequence.read(inputStream).toSequence(Integer::toHexString).join("");

		assertThat(hexString, is("deadbeef"));

		inputStream.close();
	}
}
