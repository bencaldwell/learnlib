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
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.logging.Logger;
import net.automatalib.automata.concepts.MutableTransitionOutput;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.automata.transout.impl.compact.CompactMealy;

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
        private final static Logger LOGGER = Logger.getLogger(ClockExplorationEQOracle.class.getName());
        
        private static class TimedStep<I,O> {
            private I input;
            private O output;
            
            public TimedStep(I input, O output) {
                this.input = input;
                this.output = output;
            }
            
            // copy constructor
            public TimedStep(TimedStep other) {
                this.input = (I)other.input;
                this.output = (O)other.output;
            }
            
            public I getInput() {
                return input;
            }
            
            public O getOutput() {
                return output;
            }
        }
	
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
                        
                        //TODO: find uncertain clock guards
                        List<List<TimedStep>> uncertainPrefixes = findUncertainPrefixes(hypothesis, inputs);
                        
                        // TEST: while uncertain clock guards exist
                        while (uncertainPrefixes != null && uncertainPrefixes.size()>0) {
                            //TEST: trimmed uncertain guards - keep trimming or remove uncertainty
                            for (List<TimedStep> prefix : uncertainPrefixes) {
                                trimClockGuard(hypothesis, inputs, prefix);
                            }
                            
                            //TEST: find remaining uncertain clock guards    
                            uncertainPrefixes = findUncertainPrefixes(hypothesis, inputs);
                        }
                        
		return null;
	}

        private <S, T> List<List<TimedStep>> findUncertainPrefixes(MealyMachine<S, I, T, O> hypothesis, Collection<? extends I> inputs) {
            List<List<TimedStep>> uncertainPrefixes = new ArrayList<>();

            // Get all possible sequences of inputs from min depth to max depth
            for(List<? extends I> symList : CollectionsUtil.allTuples(inputs, minDepth, maxDepth)) {
                
                S cur = hypothesis.getInitialState();
                // collect all prefixes ending in a transition with uncertain clock guard
                List<TimedStep> prefix = new ArrayList<>();
                for (I sym : symList) { 
                    O output = hypothesis.getOutput(cur, sym);
                    cur = hypothesis.getSuccessor(cur, sym);
                    prefix.add(new TimedStep(sym, output));
                    
                    // if this transition contains an uncertain clock guard then add it to the list to trim
                    if (output.toString().contains("[?")) {
                        uncertainPrefixes.add(prefix);
                        // Copy the prefix to continue finding prefixes with uncertain clock guards
                        List<TimedStep> newPrefix = new ArrayList<>();
                        for (TimedStep step : prefix) {
                            newPrefix.add(step);
                        }
                        prefix = newPrefix;
                    }
                }
            }
            return uncertainPrefixes;
        }
        
        //TEST: does this trim guards?
        private <S, T> void trimClockGuard(MealyMachine<S, I, T, O> hypothesis, Collection<? extends I> inputs, List<TimedStep> uncertainPrefix) {
            
            for (I sym : inputs) {
                S cur = hypothesis.getInitialState();
                sul.pre();

                ListIterator<TimedStep> iterator = uncertainPrefix.listIterator();
                assert(uncertainPrefix.size() > 0);
                TimedStep step = uncertainPrefix.get(0);
                while (iterator.hasNext()) {
                    step = iterator.next(); // next step

                    // If this is the last step
                    if (!iterator.hasNext()) {
                        break;
                    }
                    else {
                        sul.step((I)step.input);
                        cur = hypothesis.getSuccessor(cur, (I)step.input);
                    }
                }
                // perform the trimmed guard step
                O uncertainOutput = hypothesis.getOutput(cur, sym);
                assert(hypothesis instanceof MutableTransitionOutput);
                T uncertainTransition = hypothesis.getTransition(cur, sym);
                String[] tokens = uncertainOutput.toString().split("[\\[\\?\\]]");
                assert(tokens.length > 1);
                long clockGuard = Long.valueOf(tokens[1])*1000L; // get clockguard in ms
                clockGuard -= 500L;
                clockGuard = clockGuard < 500L ? 500L : clockGuard;
                sul.step((I)step.input, clockGuard);
                cur = hypothesis.getSuccessor(cur, (I)step.input);
                
                // check that the next symbol has the same output as the hypothesis
                String expectedOutput = hypothesis.getOutput(cur, sym).toString();
                String observedOutput = sul.step(sym).toString();
                
                // is the hypothesis output still uncertain?
                if (observedOutput.contains("[?")) {
                    if (expectedOutput.equalsIgnoreCase(observedOutput)) {
                        // the trimmed guard did not affect the result so keep it trimmed and uncertain
                        String newOutput = tokens[0]+"[?"+ Math.round(clockGuard*2/1000)/2 + "]";
                        ((MutableTransitionOutput)hypothesis).setTransitionOutput(uncertainTransition, newOutput);
                    } else {
                        // the trimmed guard affected the result - undo trim and remove uncertainty
                        String newOutput = tokens[0]+"["+ tokens[1] + "]";
                        ((MutableTransitionOutput)hypothesis).setTransitionOutput(uncertainTransition, newOutput);
                    }
                }
                sul.post();
            }
        }

}
