/* Copyright (C) 2013 TU Dortmund
 * This file is part of LearnLib, http://www.learnlib.de/.
 * 
 * LearnLib is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 3.0 as published by the Free Software Foundation.
 * 
 * LearnLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with LearnLib; if not, see
 * <http://www.gnu.de/documents/lgpl.en.html>.
 */
package de.learnlib.filters.reuse.test;

import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.Alphabets;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import de.learnlib.algorithms.lstargeneric.mealy.ExtensibleLStarMealyBuilder;
import de.learnlib.api.LearningAlgorithm.MealyLearner;
import de.learnlib.filters.reuse.ReuseCapableOracle;
import de.learnlib.filters.reuse.ReuseOracle;
import de.learnlib.filters.reuse.tree.ReuseNode;

public class LearningTest {
	private ReuseOracle<Integer, Integer, String> reuseOracle;

	/**
	 * {@inheritDoc}.
	 */
	@BeforeClass
	protected void setUp() {
		ReuseCapableOracle<Integer, Integer, String> reuseCapableOracle = new TestOracle(
				3);
		reuseOracle = new ReuseOracle<>(reuseCapableOracle);
	}

	@Test
	public void simpleTest() {
		Alphabet<Integer> sigma = Alphabets.integers(0, 3);

		MealyLearner<Integer, String> learner = new ExtensibleLStarMealyBuilder<Integer,String>()
			.withAlphabet(sigma)
			.withOracle(reuseOracle)
			.create();

		learner.startLearning();
	}

	class TestOracle implements ReuseCapableOracle<Integer, Integer, String> {
		private int threshold;

		public TestOracle(int threshold) {
			this.threshold = threshold;
		}

		@Override
		public QueryResult<Integer, String> continueQuery(Word<Integer> trace,
				ReuseNode<Integer, Integer, String> s) {

			Integer integer = s.getSystemState();

			WordBuilder<String> output = new WordBuilder<>();
			for (Integer symbol : trace) {
				if (integer + symbol <= threshold) {
					integer += symbol;
					output.add("ok");
				} else {
					output.add("error");
				}
			}

			QueryResult<Integer, String> result = new QueryResult<Integer, String>(
					output.toWord(), integer, true);

			return result;
		}

		@Override
		public QueryResult<Integer, String> processQuery(Word<Integer> trace) {

			Integer integer = new Integer(0);
			WordBuilder<String> output = new WordBuilder<>();
			for (Integer symbol : trace) {
				if (integer + symbol <= threshold) {
					integer += symbol;
					output.add("ok");
				} else {
					output.add("error");
				}
			}

			QueryResult<Integer, String> result = new QueryResult<Integer, String>(
					output.toWord(), integer, true);

			return result;
		}
	}
}