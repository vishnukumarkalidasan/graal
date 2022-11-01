package com.oracle.truffle.api.operation.test;

import java.util.Random;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.CompilerControl.Mode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import com.oracle.truffle.api.CompilerDirectives;

@State(Scope.Benchmark)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(1)
public class Playground {

    private Random r = new Random();

    private static class TestTuple {
        private final short lhs;
        private final short rhs;

        private TestTuple(short lhs, short rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }
    }

    private static TestTuple otherThing(short lhs, short rhs) {
        return new TestTuple(lhs, rhs);
    }

    @CompilerControl(Mode.DONT_INLINE)
    private static int performThing(boolean b, short lhs, short rhs) {
        TestTuple tt = otherThing(lhs, rhs);
        TestTuple t2 = otherThing(rhs, lhs);
        return b ? tt.lhs : t2.lhs;
    }

    @Benchmark
    public void simpleTest() {
        CompilerDirectives.blackhole(performThing(r.nextBoolean(), (short) r.nextInt(), (short) r.nextInt()));
    }
}
