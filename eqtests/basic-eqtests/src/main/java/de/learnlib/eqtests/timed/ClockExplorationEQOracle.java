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
package de.learnlib.eqtests.timed;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.automatalib.automata.concepts.DetOutputAutomaton;
import net.automatalib.commons.util.collections.CollectionsUtil;
import net.automatalib.words.Word;
import de.learnlib.api.EquivalenceOracle;
import de.learnlib.api.MembershipOracle;
import de.learnlib.api.SULTimed;
import de.learnlib.oracles.DefaultQuery;
import net.automatalib.automata.transout.MealyMachine;

/**
 * Finds transitions with uncertain clock guards and "trims" them to smallest equivalent.
 * 
 * Performs a complete exploration checking for outputs tagged with [?] clock guards.
 * The step clock limit is trimmed and output compared to see if the trim still results in equivalence.
 * 
 * Based on CompleteExplorationEQOracle by Malte Isberner.
 * 
 * @author Ben Caldwell <benny.caldwell@gmail.com>
 *
 * @param <I> input symbol class
 * @param <O> output class
 */
public class ClockExplorationEQOracle<I, O> implements
		EquivalenceOracle.MealyEquivalenceOracle<I,O> {
	
	private int minDepth;
	private int maxDepth;
	private final SULTimed<I, O> sul;
	
	/**
	 * Constructor.
	 * @param sulOracle interface to the system under learning
	 * @param maxDepth maximum exploration depth
	 */
	public ClockExplorationEQOracle(SULTimed<I, O> sulOracle, int maxDepth) {
		this(sulOracle, 1, maxDepth);
	}
	
	/**
	 * Constructor.
	 * @param sulOracle interface to the system under learning
	 * @param minDepth minimum exploration depth
	 * @param maxDepth maximum exploration depth
	 */
	public ClockExplorationEQOracle(SULTimed<I, O> sulOracle, int minDepth, int maxDepth) {
		if(maxDepth < minDepth)
			maxDepth = minDepth;
		
		this.minDepth = minDepth;
		this.maxDepth = maxDepth;
		
		this.sul = sulOracle;
	}

	/**
	 * 
	 * @param hypothesis
	 * @param inputs
	 * @return null or a counterexample
	 */
	@Override
	public DefaultQuery<I, Word<O>> findCounterExample(MealyMachine<?,I,?,O> hypothesis,
			Collection<? extends I> inputs) {
		return doFindCounterExample(hypothesis, inputs);
	}
        
        private <S, T> DefaultQuery<I, Word<O>> doFindCounterExample(
			MealyMachine<S, I, T, O> hypothesis, Collection<? extends I> inputs) {
            
		for(List<? extends I> symList : CollectionsUtil.allTuples(inputs, minDepth, maxDepth)) {

                        S cur = hypothesis.getInitialState();
                        S candidateState = null;
                        I candidateInput = null;
                        String candidateOutput = null;
                        //TODO:
                        /**
                         * 1) Check for an unknown clock guard
                         * 2) Trim the guard and see if the output and successor signature still match
                         * 3) If equivalence is maintained then save the new query as a counterexample
                         */
                        
                        sul.pre();
                        for (I sym : symList) {
                            //TEST: check for uncertain clock guard - in the form "*[?TIME]*"
                            String[] hypTokens = hypothesis.getOutput(cur, sym).toString().split("[\\[\\?\\]]");
                            String hypOutput = hypTokens[0];
                            if (candidateState != null) {
                                //TODO: is the output different? Adjust the candidate if it was
                                String[] sulTokens = sul.step(sym).toString().split("[\\[\\?\\]]");
                                cur = hypothesis.getSuccessor(cur, sym); //update the position of the hypothesis to keep it locked to the SUL

                                if (!sulTokens[0].contentEquals(hypOutput)) {
                                    //TODO: The outputs are different the guard was overtrimmed - add 500ms back onto it?
                                    
                                }
                                
                                // Clear the candidates for the next round
                                candidateState = null;
                                candidateInput = null;
                                candidateOutput = null;
                            }
                            else if (hypTokens.length < 2) {
                                // TODO: No candidates and no uncertain guards on this one, just step SUL and hypothesis
                                
                            }
                            else {
                                //TODO: there is existing candidate but there is a new candidate to check
                                String hypOutput = tokens[0];
                                long stepClockLimit = Long.valueOf(tokens[1]) * 1000L; // convert the guard in seconds to ms    

                                //TODO: trim the guard
                                long trimmedStepClockLimit = stepClockLimit > 500 ? stepClockLimit - 500 : 0;
                                
                                candidateState = cur;
                                candidateInput = sym;

                                //TODO: is the output different? Adjust the candidate if it was
                                String output = sul.step(sym,trimmedStepClockLimit).toString();
                                String[] outputTokens = output.split("[\\?\\]]");
                                if (!outputTokens[0].contentEquals(hypOutput)) {
                                    // The outputs are different the guard was overtrimmed
                                    String certainOutput = 
                                }
                                
                                //TODO: is the successor still the same?
                                
                            }
                        }
                        sul.post();
                        
                        }
                        Word<I> queryWord = Word.fromList(symList);
			DefaultQuery<I,O> query = new DefaultQuery<>(queryWord);
			O hypOutput = hypothesis.computeOutput(queryWord);                        
                        
                        //TODO: is the successor ID still the same?
                        
                        //TODO: if the trimmed guard maintains equivalence return it as a counterexample
			
			
			if(!Objects.equals(hypOutput, query.getOutput()))
				return query;
		}
		
		return null;
	}

}
