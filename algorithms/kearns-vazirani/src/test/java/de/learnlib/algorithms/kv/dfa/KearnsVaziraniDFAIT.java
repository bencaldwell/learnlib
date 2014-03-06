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
package de.learnlib.algorithms.kv.dfa;

import de.learnlib.api.MembershipOracle.DFAMembershipOracle;
import de.learnlib.testsupport.it.learner.AbstractDFALearnerIT;
import de.learnlib.testsupport.it.learner.LearnerVariantList.DFALearnerVariantList;

import net.automatalib.words.Alphabet;

import org.testng.annotations.Test;

/**
 * Function test for the Kearns/Vazirani algorithm for DFA learning.
 * 
 * @author Malte Isberner
 *
 */
@Test
public class KearnsVaziraniDFAIT extends AbstractDFALearnerIT {
	
	@Override
	protected <I> void addLearnerVariants(Alphabet<I> alphabet,
			DFAMembershipOracle<I> mqOracle, DFALearnerVariantList<I> variants) {
		variants.addLearnerVariant("KearnsVaziraniDFA", new KearnsVaziraniDFA<>(alphabet, mqOracle));
	}

}
