/* Copyright (C) 2014 TU Dortmund
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
package de.learnlib.testsupport.it.learner;

import java.util.List;

import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

import de.learnlib.api.MembershipOracle.MealyMembershipOracle;
import de.learnlib.examples.LearningExample.MealyLearningExample;
import de.learnlib.examples.LearningExamples;
import de.learnlib.oracles.SimulatorOracle.MealySimulatorOracle;
import de.learnlib.testsupport.it.learner.LearnerVariantList.MealyLearnerVariantList;
import de.learnlib.testsupport.it.learner.LearnerVariantListImpl.MealyLearnerVariantListImpl;

/**
 * Abstract integration test for Mealy machine learning algorithms.
 * <p>
 * Mealy machine learning algorithms tested by this integration test are expected to
 * assume membership queries yield the full output word corresponding to the suffix
 * part of the query. If the learning algorithm only expects the last symbol as
 * output, use {@link AbstractMealySymLearnerIT}.
 *  
 * @author Malte Isberner
 *
 */
public abstract class AbstractMealyLearnerIT extends AbstractLearnerIT {

	
	// @Factory FIXME
	@Override
	public SingleExampleAllVariantsITSubCase<?,?,?>[] createExampleITCases() {
		List<? extends MealyLearningExample<?,?>> examples = LearningExamples.createMealyExamples();
		
		SingleExampleAllVariantsITSubCase<?,?,?>[] result = new SingleExampleAllVariantsITSubCase[examples.size()];
		int i = 0;
		for(MealyLearningExample<?,?> example : examples) {
			result[i++] = createAllVariantsITCase(example);
		}
		
		return result;
	}
	
	private <I,O>
	SingleExampleAllVariantsITSubCase<I, Word<O>, MealyMachine<?,I,?,O>> createAllVariantsITCase(MealyLearningExample<I,O> example) {
		Alphabet<I> alphabet = example.getAlphabet();
		MealyMembershipOracle<I,O> mqOracle
			= new MealySimulatorOracle<>(example.getReferenceAutomaton());
		MealyLearnerVariantListImpl<I,O> variants = new MealyLearnerVariantListImpl<>();
		addLearnerVariants(alphabet, mqOracle, variants);
		
		return new SingleExampleAllVariantsITSubCase<>(example, variants);
	}
	
	/**
	 * Adds, for a given setup, all the variants of the Mealy machine learner to be tested
	 * to the specified {@link LearnerVariantList variant list}.
	 * 
	 * @param alphabet the input alphabet
	 * @param mqOracle the membership oracle
	 * @param variants list to add the learner variants to
	 */
	protected abstract <I,O> void addLearnerVariants(
			Alphabet<I> alphabet,
			MealyMembershipOracle<I,O> mqOracle,
			MealyLearnerVariantList<I,O> variants);
}
