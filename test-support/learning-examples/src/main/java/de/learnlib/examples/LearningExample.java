/* Copyright (C) 2014-2015 TU Dortmund
 * This file is part of LearnLib, http://www.learnlib.de/.
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
package de.learnlib.examples;

import net.automatalib.automata.UniversalAutomaton;
import net.automatalib.automata.concepts.SuffixOutput;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

public interface LearningExample<I,D,A extends UniversalAutomaton<?, I, ?, ?, ?> & SuffixOutput<I,D>> {
	
	public static interface DFALearningExample<I> extends LearningExample<I,Boolean,DFA<?,I>> {
	}
	
	public static interface MealyLearningExample<I,O> extends LearningExample<I,Word<O>,MealyMachine<?,I,?,O>> {
	}
	
	public A getReferenceAutomaton();
	
	public Alphabet<I> getAlphabet();

}
