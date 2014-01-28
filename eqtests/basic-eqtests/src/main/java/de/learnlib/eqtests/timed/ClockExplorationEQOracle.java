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
import java.util.List;
import net.automatalib.commons.util.collections.CollectionsUtil;
import net.automatalib.words.Word;
import de.learnlib.api.EquivalenceOracle;
import de.learnlib.api.SULTimed;
import de.learnlib.oracles.DefaultQuery;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
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
                        
                        //TEST: find uncertain clock guards with accessor prefix
                        HashMap<List<I>,Long> uncertainPrefixes = findUncertainPrefixes((CompactMealy<I,O>)hypothesis, inputs);
                        
                        // TEST: while uncertain clock guards exist
                        while (uncertainPrefixes != null && uncertainPrefixes.size()>0) {
                            //TEST: trimmed uncertain guards - keep trimming or remove uncertainty
                            Iterator it = uncertainPrefixes.entrySet().iterator();
                            while (it.hasNext()) {
                                Map.Entry pairs = (Map.Entry)it.next();
                                trimClockGuard(hypothesis, inputs, pairs);
                            }
                            
                            //TEST: find remaining uncertain clock guards    
                            uncertainPrefixes = findUncertainPrefixes((CompactMealy<I,O>)hypothesis, inputs);
                        }
                        
		return null;
	}

        HashMap<List<I>,Long> findUncertainPrefixes(CompactMealy<I,O> hypothesis, Collection<? extends I> inputs) {
            // TEST: Store access prefixes resulting in uncertain clock guards in a hash map (no duplicates?)
            HashMap<List<I>,Long> uncertainPrefixes = new HashMap<>();

            // Get all possible sequences of inputs from min depth to max depth
            for(List<? extends I> symList : CollectionsUtil.allTuples(inputs, minDepth, maxDepth)) {
                
                int cur = hypothesis.getInitialState();
                // collect all prefixes ending in a transition with uncertain clock guard
                O output = null;
                for (I sym : symList) { 
                    output = hypothesis.getOutput(cur, sym);
                    cur = hypothesis.getSuccessor(cur, sym);
                }
                // if the last transition contains an uncertain clock guard then add it to the list to trim
                if (outputContainsUncertainClockGuard(output.toString())) {
                    long clockGuard = clockGuardFromOutput(output.toString()); // get clockguard in ms
                    uncertainPrefixes.put((List<I>) symList, clockGuard);
                }
            }
            return uncertainPrefixes;
        }
        
        //TEST: does this trim guards?
        private <S, T> void trimClockGuard(MealyMachine<S, I, T, O> hypothesis, Collection<? extends I> inputs, Map.Entry<List<I>,Long> uncertainPrefix) {
            // The uncertain prefix comes as a hashmap pair with key: list of inputs <I> as a prefix; value: output <O> from the prefix
            // For each suffix of the given uncertain prefix
            for (I sym : inputs) {
                
                // Follow the prefix in hypothesis and SUL
                S cur = hypothesis.getInitialState();
                sul.pre();
                ListIterator<I> iterator = uncertainPrefix.getKey().listIterator(); // for each <I> in the prefix
                assert(uncertainPrefix.getKey().size() > 0);
                I step = uncertainPrefix.getKey().get(0);
                while (iterator.hasNext()) {
                    step = iterator.next(); // next step

                    // If this is the last step
                    if (!iterator.hasNext()) {
                        break;
                    }
                    else {
                        sul.step(step);
                        cur = hypothesis.getSuccessor(cur, step);
                    }
                }
                // perform the trimmed guard step
                O uncertainOutput = hypothesis.getOutput(cur, sym);
                assert(hypothesis instanceof MutableTransitionOutput);
                T uncertainTransition = hypothesis.getTransition(cur, sym);
                long clockGuard = uncertainPrefix.getValue();
                clockGuard -= 500L;
                clockGuard = clockGuard < 500L ? 500L : clockGuard;
                sul.step(step, clockGuard); // perform the step with trimmed clock guard on SUL
                cur = hypothesis.getSuccessor(cur, step); // get the next step in the hypothesis
                
                // check that the next symbol (after the prefix) has the same output as the hypothesis
                String expectedOutput = hypothesis.getOutput(cur, sym).toString();
                String observedOutput = sul.step(sym).toString();
                
                // Did the trimmed guard cause loss of equivalence?
                if (expectedOutput.equalsIgnoreCase(observedOutput)) {
                    // the trimmed guard did not affect the result so keep it trimmed and uncertain
                    String newOutput = symbolFromOutput(expectedOutput.toString()) +"[?"+ Math.round(clockGuard*2/1000)/2 + "]";
                    ((MutableTransitionOutput)hypothesis).setTransitionOutput(uncertainTransition, newOutput);
                } else {
                    // the trimmed guard affected the result - undo trim and remove uncertainty 
                    String newOutput = symbolFromOutput(expectedOutput.toString()) +"["+ clockGuardFromOutput(expectedOutput.toString()) + "]";
                    ((MutableTransitionOutput)hypothesis).setTransitionOutput(uncertainTransition, newOutput);
                    // Don't bother with any more suffixes
                    break;
                }
                sul.post();
            }
        }
        
        static Long clockGuardFromOutput(String output) {
            String[] tokens = output.split("[\\[\\?\\]]+");
            if (tokens.length < 2) {
                return null; // No clock guard from split
            }
            double secondsGuard = Double.valueOf(tokens[1]);
            Long clockGuard = Math.round(secondsGuard*1000); // get clockguard in ms
            return clockGuard;
        }
        
        static String symbolFromOutput(String output) {
            String[] tokens = output.split("[\\[\\?\\]]+");
            if (tokens.length < 1) {
                return null; // no tokens...?
            }
            String symbol = tokens[0]; // get clockguard in ms
            return symbol;
        }
        
        static boolean outputContainsUncertainClockGuard(String output) {
            // If no ? then no uncertainty
            if (!output.toString().contains("[?")) {
                return false;
            }
            // Tokenise and attempt to parse a long from the guard
            String[] tokens = output.split("[\\[\\?\\]]+");
            if (tokens.length < 2) {
                return false; // No clock guard from split
            }
            try {
                Double secondsGuard = Double.parseDouble(tokens[1]);
                Long clockGuard = Math.round(secondsGuard *1000);
            } catch (NumberFormatException e) {
                return false;
            }
            return true;
        }
}
