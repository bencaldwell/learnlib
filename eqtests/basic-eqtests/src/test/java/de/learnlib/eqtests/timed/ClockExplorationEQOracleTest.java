/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.eqtests.timed;

import com.caldwellsoftware.plclearning.PLCSUL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.automatalib.automata.transout.impl.compact.CompactMealy;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.FastAlphabet;
import net.automatalib.words.impl.Symbol;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author bencaldwell
 */
public class ClockExplorationEQOracleTest {
    
    @Test
    public void testUncertainClockGuardDetection() {
        String test1 = "!IN[?10.5]";
        Assert.assertTrue(ClockExplorationEQOracle.outputContainsUncertainClockGuard(test1));
        
        test1 = "!IN[10.5]";
        Assert.assertFalse(ClockExplorationEQOracle.outputContainsUncertainClockGuard(test1));
        
        test1 = "RST";
        Assert.assertFalse(ClockExplorationEQOracle.outputContainsUncertainClockGuard(test1));
    }
    
    @Test
    public void testGetClockGuard() {
        String test1 = "!IN[?10.5]";
        Assert.assertEquals((long)ClockExplorationEQOracle.clockGuardFromOutput(test1), 10500L);
        
        test1 = "!IN[100.5]";
        Assert.assertEquals((long)ClockExplorationEQOracle.clockGuardFromOutput(test1), 100500L);
        
        test1 = "!IN[0.4]";
        Assert.assertEquals((long)ClockExplorationEQOracle.clockGuardFromOutput(test1), 400L);
    }
    
    @Test
    public void testGetSymbol() {
        String test1 = "!IN[?10.5]";
        Assert.assertEquals(ClockExplorationEQOracle.symbolFromOutput(test1),"!IN");
        
        test1 = "!IN[100.5]";
        Assert.assertEquals(ClockExplorationEQOracle.symbolFromOutput(test1),"!IN");
        
        test1 = "!IN";
        Assert.assertEquals(ClockExplorationEQOracle.symbolFromOutput(test1),"!IN");
    }
    
    @Test
    public void testHypothesis() {
        List<Symbol> inputs = new ArrayList<>();
        Symbol in = new Symbol("IN");
        inputs.add(in);
        Symbol not_in = new Symbol("!IN");
        inputs.add(not_in);
        Symbol rst = new Symbol("RST");
        inputs.add(rst);
        Symbol not_rst = new Symbol("!RST");
        inputs.add(not_rst);
        Alphabet<Symbol> alphabet = new FastAlphabet<>(inputs);
        
        CompactMealy<Symbol, String> hypothesis = new CompactMealy<>(alphabet);
        int zero = hypothesis.addInitialState();
        
        // Inputs with no effect at 0 state
        hypothesis.addTransition(zero, not_in, zero, "0[?10.0]");
        hypothesis.addTransition(zero, rst, zero, "0[?10.0]");
        hypothesis.addTransition(zero, not_rst, zero, "0[?10.0]");
        
        // input to transition to 1 state
        int one = hypothesis.addState();
        hypothesis.addTransition(zero, in, one, "1[5.0]");
        
        // inputs with no effect in 1 state
        hypothesis.addTransition(one, in, one, "1[?10.0]");
        hypothesis.addTransition(one, rst, one, "1[?10.0]");
        hypothesis.addTransition(one, not_rst, one, "1[?10.0]");
        
        // transition to 2 state
        int two = hypothesis.addState();
        hypothesis.addTransition(one, not_in, two, "1[?10.0]");
        // transition from 2 back to 1
        hypothesis.addTransition(two, in, one, "1[?10.0]");
        
        // inputs with no effect in 2 state
        hypothesis.addTransition(two, not_rst, two, "1[?10.0]");
        hypothesis.addTransition(two, not_in, two, "1[?10.0]");
        
        // transition to 0 state
        hypothesis.addTransition(two, rst, zero, "0");
        
        PLCSUL sul = null;
        int maxDepth = 3;
        // clock trimming oracle works?
        ClockExplorationEQOracle eqoracle = new ClockExplorationEQOracle(sul, maxDepth);
        
        HashMap<List<Symbol>,Long> uncertainPrefixes = eqoracle.findUncertainPrefixes(hypothesis,alphabet);

        // Check that the correct (minimal) number of prefixes was found
        Assert.assertTrue(uncertainPrefixes.size() == 10);
        
    }
}
