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

import de.learnlib.api.EquivalenceOracle;
import de.learnlib.api.SULTimed;
import de.learnlib.oracles.DefaultQuery;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.automatalib.automata.concepts.MutableTransitionOutput;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.automata.transout.impl.compact.CompactMealy;
import net.automatalib.commons.util.collections.CollectionsUtil;
import net.automatalib.words.Word;

/**
 * Finds transitions with uncertain clock guards and "trims" them to smallest equivalent.
 * 
 * Performs a complete exploration checking for outputs tagged with [?] clock guards.
 * The step clock limit is trimmed and output compared to see if the trim still results in equivalence.
 *         
 * Clock discovery walk:
 * 1) For each outgoing transition check the guard on the output and see if we can do it faster
 * 2) The output must match the hypothesis output
 * 3) The successor state signature must match the hypothesis successor state signature
 * 4) If output and successor match then "trim" the clock guard
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
        private final static Logger LOGGER = Logger.getGlobal();
	private long trimTime = 1000L; // the amount of time to trim off clock guards
        
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
        
        private <S, T> DefaultQuery<I, Word<O>> doFindCounterExample(MealyMachine<S, I, T, O> hypothesis, Collection<? extends I> inputs) {
                        
            LOGGER.fine("Started finding counter examples.");
            
            // find uncertain clock guards with accessor prefix
            HashMap<List<I>,Long> uncertainPrefixes = findUncertainPrefixes((CompactMealy<I,O>)hypothesis, inputs);

            // while uncertain clock guards exist
            while (uncertainPrefixes != null && uncertainPrefixes.size()>0) {
                // trimmed uncertain guards - keep trimming or remove uncertainty
                Iterator it = uncertainPrefixes.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pairs = (Map.Entry)it.next();
                    trimClockGuard(hypothesis, inputs, pairs);
                }

                // find remaining uncertain clock guards    
                uncertainPrefixes = findUncertainPrefixes((CompactMealy<I,O>)hypothesis, inputs);
            }
                        
		return null;
	}

        HashMap<List<I>,Long> findUncertainPrefixes(CompactMealy<I,O> hypothesis, Collection<? extends I> inputs) {
 
            LOGGER.fine("Started uncertain prefixes.");
            
            // Store access prefixes resulting in uncertain clock guards in a hash map (no duplicates?)
            HashMap<List<I>,Long> uncertainPrefixes = new HashMap<>();

            for (Integer state : hypothesis.getStates()) {
                List<? extends I> prefix = findAccessPrefix(state, hypothesis, inputs);
                for (I sym : inputs) { 
                    O output = hypothesis.getOutput(state, sym);
                    // if the last transition contains an uncertain clock guard then add it to the list to trim
                    if (outputContainsUncertainClockGuard(output.toString())) {
                        long clockGuard = clockGuardFromOutput(output.toString()); // get clockguard in ms
                        // Copy all symbols to a new list for storing in the uncertainprefixes map
                        List<I> copySymList = new ArrayList<>(prefix);
                        copySymList.add(sym);
                        uncertainPrefixes.put((List<I>) copySymList, clockGuard);
                        LOGGER.fine("Added uncertain prefix: " + copySymList.toString());
                    }
                }
            }
            return uncertainPrefixes;
        }
        
        List<? extends I> findAccessPrefix(Integer state, CompactMealy<I,O> hypothesis, Collection<? extends I> inputs) {
            int cur = hypothesis.getInitialState();
            // Get all possible sequences of inputs from min depth to max depth
            for(List<? extends I> prefix : CollectionsUtil.allTuples(inputs, minDepth, maxDepth)) {
                if (hypothesis.getState(prefix) == state) {
                    return prefix;
                }
            }
            return null;
        }
        
        private <S, T> void trimClockGuard(MealyMachine<S, I, T, O> hypothesis, Collection<? extends I> inputs, Map.Entry<List<I>,Long> uncertainPrefix) {
            // The uncertain prefix comes as a hashmap pair with key: list of inputs <I> as a prefix; value: output <O> from the prefix
            // For each suffix of the given uncertain prefix
            for (I sym : inputs) {
                try {
                    // Follow the prefix in hypothesis and SUL
                    S cur = hypothesis.getInitialState();
                    sul.pre();
                    ListIterator<I> iterator = uncertainPrefix.getKey().listIterator(); // for each <I> in the prefix
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
                    O uncertainOutput = hypothesis.getOutput(cur, step);
                    T uncertainTransition = hypothesis.getTransition(cur, step);
                    long clockGuard = uncertainPrefix.getValue();
                    clockGuard -= trimTime;
                    clockGuard = clockGuard < 0 ? 0 : clockGuard;
                    sul.step(step, clockGuard); // perform the step with trimmed clock guard on SUL
                    cur = hypothesis.getSuccessor(cur, step); // get the next step in the hypothesis

                    // check that the next symbol (after the prefix) has the same output as the hypothesis
                    String expectedOutput = hypothesis.getOutput(cur, sym).toString();
                    // use the clock guard from hypothesis for SUL clock limit so we get identical outputs
                    String observedOutput;
                    if (clockGuardFromOutput(expectedOutput)!=null) {
                        long clockLimit = clockGuardFromOutput(expectedOutput);
                        observedOutput = sul.step(sym, clockLimit).toString();
                    } else {
                        observedOutput = sul.step(sym).toString();
                    }

                    // Did the trimmed guard cause loss of equivalence?
                    if (outputsAreEquivalent(observedOutput, expectedOutput)) {
                        if (clockGuard <= 0L) {
                           // Trimmed all the way to zero so there is no guard
                            String newOutput = symbolFromOutput(uncertainOutput.toString());
                            LOGGER.fine("Trimmed clock guard to zero and removed: " + uncertainPrefix.toString() + "/" + newOutput);
                            ((MutableTransitionOutput)hypothesis).setTransitionOutput(uncertainTransition, newOutput);
                        } else {
                            // the trimmed guard did not affect the result so keep it trimmed and uncertain
                            String newOutput = symbolFromOutput(uncertainOutput.toString()) +"[?"+ Math.floor(clockGuard/1000.0f) + "]";
                            LOGGER.fine("Trimmed clock guard is still uncertain: " + uncertainPrefix.toString() + "/" + newOutput);
                            ((MutableTransitionOutput)hypothesis).setTransitionOutput(uncertainTransition, newOutput);
                        }
                    } else {
                        // the trimmed guard affected the result - undo trim and remove uncertainty 
                        String newOutput = symbolFromOutput(uncertainOutput.toString()) +"["+ Math.floor(uncertainPrefix.getValue()/1000.0f) + "]";
                        LOGGER.fine("Trimmed clock guard uncertainty removed: " + uncertainPrefix.toString() + "/" + newOutput);
                        ((MutableTransitionOutput)hypothesis).setTransitionOutput(uncertainTransition, newOutput);
                        // Don't bother with any more suffixes
                        break;
                    }
                } catch (IOException e) {
                    LOGGER.warning("SUL connection failed while trimming clock guard: " + e.toString());
                } finally {
                    try {
                        sul.post();
                    } catch (IOException ex) {
                        Logger.getLogger(ClockExplorationEQOracle.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
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
        
        static boolean outputsAreEquivalent(String observed, String expected) {
            String observedSym = symbolFromOutput(observed);
            String expectedSym = symbolFromOutput(expected);
            if (!observedSym.equalsIgnoreCase(expectedSym)) {
                return false;
            }
            Long observedGuard = clockGuardFromOutput(observed);
            Long expectedGuard = clockGuardFromOutput(expected);
            boolean observedIsUncertain = outputContainsUncertainClockGuard(observed);
            boolean expectedIsUncertain = outputContainsUncertainClockGuard(expected);
            
            // If both guards are null then the outputs can be equivalent
            if (expectedGuard == null && observedGuard == null) {
                return true;
            } 
            
            /* If the expected guard is null and the observed is > 0 this is an unobservable transition
            and the observed guard has just timed to the limit
            */
            if (expectedGuard == null && observedIsUncertain) {
                return true;
            }
            
            // If both guards have the same value then the outputs are equal
            if (observedGuard.longValue() == expectedGuard.longValue()) {
                return true;
            }
            
            return false;
        }
}
