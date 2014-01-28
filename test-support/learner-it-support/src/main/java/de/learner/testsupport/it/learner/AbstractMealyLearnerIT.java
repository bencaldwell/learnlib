/* Copyright (C) 2014 TU Dortmund
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
package de.learner.testsupport.it.learner;

import java.util.List;

import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import de.learner.testsupport.it.learner.LearnerVariantList.MealyLearnerVariantList;
import de.learner.testsupport.it.learner.internal.LearnerVariantListImpl.MealyLearnerVariantListImpl;
import de.learner.testsupport.it.learner.internal.SingleExampleAllVariantsITSubCase;
import de.learnlib.api.MembershipOracle.MealyMembershipOracle;
import de.learnlib.examples.LearningExample.MealyLearningExample;
import de.learnlib.examples.LearningExamples;
import de.learnlib.oracles.SimulatorOracle.MealySimulatorOracle;

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
