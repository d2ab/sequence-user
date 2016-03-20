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

import org.d2ab.sequence.Sequence;

/**
 * Test class for the sequence library.
 */
public class SequenceUser {
	public static void main(String[] args) {
		Sequence<String> messages = Sequence.of(1, 2, 3, 4, 5).map(i -> i + " x " + i + " = " + (i * i));
		for (String s : messages)
			System.out.println(s);
	}
}
