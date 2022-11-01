/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.api.operation.test.bml;

import static com.oracle.truffle.api.operation.test.bml.BaseBytecodeNode.OP_CONST;
import static com.oracle.truffle.api.operation.test.bml.BaseBytecodeNode.OP_JUMP_FALSE;
import static com.oracle.truffle.api.operation.test.bml.BaseBytecodeNode.OP_LD_LOC;
import static com.oracle.truffle.api.operation.test.bml.BaseBytecodeNode.OP_LESS;
import static com.oracle.truffle.api.operation.test.bml.BaseBytecodeNode.OP_MOD;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterSwitch;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleSafepoint;
import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.impl.UnsafeFrameAccess;
import com.oracle.truffle.api.nodes.BytecodeOSRNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.RootNode;

public class ManualBytecodeNode extends BaseBytecodeNode {
    protected ManualBytecodeNode(TruffleLanguage<?> language, FrameDescriptor frameDescriptor, short[] bc) {
        super(language, frameDescriptor, bc);
    }

    @Override
    @BytecodeInterpreterSwitch
    @ExplodeLoop(kind = LoopExplosionKind.MERGE_EXPLODE)
    protected Object executeAt(VirtualFrame frame, int startBci, int startSp) {
        short[] localBc = bc;
        int bci = startBci;
        int sp = startSp;

        Counter loopCounter = new Counter();

        loop: while (true) {
            short opcode = localBc[bci];
            CompilerAsserts.partialEvaluationConstant(opcode);
            switch (opcode) {
                // ( -- )
                case OP_JUMP: {
                    int nextBci = localBc[bci + 1];
                    CompilerAsserts.partialEvaluationConstant(nextBci);
                    if (nextBci <= bci) {
                        Object result = backwardsJumpCheck(frame, sp, loopCounter, nextBci);
                        if (result != null) {
                            return result;
                        }
                    }
                    bci = nextBci;
                    continue loop;
                }
                // (i1 i2 -- i3)
                case OP_ADD: {
                    int lhs = frame.getInt(sp - 2);
                    int rhs = frame.getInt(sp - 1);
                    frame.setInt(sp - 2, lhs + rhs);
                    sp -= 1;
                    bci += 1;
                    continue loop;
                }
                // (i1 i2 -- i3)
                case OP_MOD: {
                    int lhs = frame.getInt(sp - 2);
                    int rhs = frame.getInt(sp - 1);
                    frame.setInt(sp - 2, lhs % rhs);
                    sp -= 1;
                    bci += 1;
                    continue loop;
                }
                // ( -- i)
                case OP_CONST: {
                    frame.setInt(sp, (localBc[bci + 1] << 16) | (localBc[bci + 2] & 0xffff));
                    sp += 1;
                    bci += 3;
                    continue loop;
                }
                // (b -- )
                case OP_JUMP_FALSE: {
                    boolean cond = frame.getBoolean(sp - 1);
                    sp -= 1;
                    if (!cond) {
                        bci = localBc[bci + 1];
                        continue loop;
                    } else {
                        bci += 2;
                        continue loop;
                    }
                }
                // (i1 i2 -- b)
                case OP_LESS: {
                    int lhs = frame.getInt(sp - 2);
                    int rhs = frame.getInt(sp - 1);
                    frame.setBoolean(sp - 2, lhs < rhs);
                    sp -= 1;
                    bci += 1;
                    continue loop;
                }
                // (i -- )
                case OP_RETURN: {
                    return frame.getInt(sp - 1);
                }
                // (i -- )
                case OP_ST_LOC: {
                    frame.copy(sp - 1, localBc[bci + 1]);
                    sp -= 1;
                    bci += 2;
                    continue loop;
                }
                // ( -- i)
                case OP_LD_LOC: {
                    frame.copy(localBc[bci + 1], sp);
                    sp += 1;
                    bci += 2;
                    continue loop;
                }
                default:
                    CompilerDirectives.shouldNotReachHere();
            }
        }
    }
}

@GeneratedBy(ManualUnsafeBytecodeNode.class) // needed for UFA
class ManualUnsafeBytecodeNode extends BaseBytecodeNode {
    protected ManualUnsafeBytecodeNode(TruffleLanguage<?> language, FrameDescriptor frameDescriptor, short[] bc) {
        super(language, frameDescriptor, bc);
    }

    private static final UnsafeFrameAccess UFA = UnsafeFrameAccess.lookup();

    @Override
    @BytecodeInterpreterSwitch
    @ExplodeLoop(kind = LoopExplosionKind.MERGE_EXPLODE)
    protected Object executeAt(VirtualFrame frame, int startBci, int startSp) {
        short[] localBc = bc;
        int bci = startBci;
        int sp = startSp;

        Counter loopCounter = new Counter();

        frame.getArguments();

        loop: while (true) {
            short opcode = UFA.unsafeShortArrayRead(localBc, bci);
            CompilerAsserts.partialEvaluationConstant(opcode);
            switch (opcode) {
                // ( -- )
                case OP_JUMP: {
                    int nextBci = UFA.unsafeShortArrayRead(localBc, bci + 1);
                    CompilerAsserts.partialEvaluationConstant(nextBci);
                    if (nextBci <= bci) {
                        Object result = backwardsJumpCheck(frame, sp, loopCounter, nextBci);
                        if (result != null) {
                            return result;
                        }
                    }
                    bci = nextBci;
                    continue loop;
                }
                // (i1 i2 -- i3)
                case OP_ADD: {
                    int lhs = UFA.unsafeGetInt(frame, sp - 2);
                    int rhs = UFA.unsafeGetInt(frame, sp - 1);
                    UFA.unsafeSetInt(frame, sp - 2, lhs + rhs);
                    sp -= 1;
                    bci += 1;
                    continue loop;
                }
                // (i1 i2 -- i3)
                case OP_MOD: {
                    int lhs = UFA.unsafeGetInt(frame, sp - 2);
                    int rhs = UFA.unsafeGetInt(frame, sp - 1);
                    UFA.unsafeSetInt(frame, sp - 2, lhs % rhs);
                    sp -= 1;
                    bci += 1;
                    continue loop;
                }
                // ( -- i)
                case OP_CONST: {
                    UFA.unsafeSetInt(frame, sp, (UFA.unsafeShortArrayRead(localBc, bci + 2) << 16) | (UFA.unsafeShortArrayRead(localBc, bci + 1) & 0xffff));
                    sp += 1;
                    bci += 3;
                    continue loop;
                }
                // (b -- )
                case OP_JUMP_FALSE: {
                    boolean cond = UFA.unsafeGetBoolean(frame, sp - 1);
                    sp -= 1;
                    if (!cond) {
                        bci = UFA.unsafeShortArrayRead(localBc, bci + 1);
                        continue loop;
                    } else {
                        bci += 2;
                        continue loop;
                    }
                }
                // (i1 i2 -- b)
                case OP_LESS: {
                    int lhs = UFA.unsafeGetInt(frame, sp - 2);
                    int rhs = UFA.unsafeGetInt(frame, sp - 1);
                    UFA.unsafeSetBoolean(frame, sp - 2, lhs < rhs);
                    sp -= 1;
                    bci += 1;
                    continue loop;
                }
                // (i -- )
                case OP_RETURN: {
                    return UFA.unsafeGetInt(frame, sp - 1);
                }
                // (i -- )
                case OP_ST_LOC: {
                    UFA.unsafeCopyPrimitive(frame, sp - 1, UFA.unsafeShortArrayRead(localBc, bci + 1));
                    sp -= 1;
                    bci += 2;
                    continue loop;
                }
                // ( -- i)
                case OP_LD_LOC: {
                    UFA.unsafeCopyPrimitive(frame, UFA.unsafeShortArrayRead(localBc, bci + 1), sp);
                    sp += 1;
                    bci += 2;
                    continue loop;
                }
                default:
                    CompilerDirectives.shouldNotReachHere();
            }
        }
    }
}

@GeneratedBy(ManualUnsafeSuperinstructionBytecodeNode.class) // needed for UFA
class ManualUnsafeSuperinstructionBytecodeNode extends BaseBytecodeNode {
    protected ManualUnsafeSuperinstructionBytecodeNode(TruffleLanguage<?> language, FrameDescriptor frameDescriptor, short[] bc) {
        super(language, frameDescriptor, bc);
    }

    private static final UnsafeFrameAccess UFA = UnsafeFrameAccess.lookup();

    @Override
    @BytecodeInterpreterSwitch
    @ExplodeLoop(kind = LoopExplosionKind.MERGE_EXPLODE)
    protected Object executeAt(VirtualFrame frame, int startBci, int startSp) {
        short[] localBc = bc;
        int bci = startBci;
        int sp = startSp;

        Counter loopCounter = new Counter();

        frame.getArguments();

        loop: while (true) {
            short opcode = UFA.unsafeShortArrayRead(localBc, bci);
            CompilerAsserts.partialEvaluationConstant(opcode);
            switch (opcode) {
                // ( -- )
                case OP_JUMP: {
                    int nextBci = UFA.unsafeShortArrayRead(localBc, bci + 1);
                    CompilerAsserts.partialEvaluationConstant(nextBci);
                    if (nextBci <= bci) {
                        Object result = backwardsJumpCheck(frame, sp, loopCounter, nextBci);
                        if (result != null) {
                            return result;
                        }
                    }
                    bci = nextBci;
                    continue loop;
                }
                // (i1 i2 -- i3)
                case OP_ADD: {
                    int lhs = UFA.unsafeGetInt(frame, sp - 2);
                    int rhs = UFA.unsafeGetInt(frame, sp - 1);
                    UFA.unsafeSetInt(frame, sp - 2, lhs + rhs);
                    sp -= 1;
                    bci += 1;
                    continue loop;
                }
                // (i1 i2 -- i3)
                case OP_MOD: {
                    int lhs = UFA.unsafeGetInt(frame, sp - 2);
                    int rhs = UFA.unsafeGetInt(frame, sp - 1);
                    UFA.unsafeSetInt(frame, sp - 2, lhs % rhs);
                    sp -= 1;
                    bci += 1;
                    continue loop;
                }
                // ( -- i)
                case OP_CONST: {
                    UFA.unsafeSetInt(frame, sp, (UFA.unsafeShortArrayRead(localBc, bci + 2) << 16) | (UFA.unsafeShortArrayRead(localBc, bci + 1) & 0xffff));
                    sp += 1;
                    bci += 3;
                    continue loop;
                }
                // (b -- )
                case OP_JUMP_FALSE: {
                    boolean cond = UFA.unsafeGetBoolean(frame, sp - 1);
                    sp -= 1;
                    if (!cond) {
                        bci = UFA.unsafeShortArrayRead(localBc, bci + 1);
                        continue loop;
                    } else {
                        bci += 2;
                        continue loop;
                    }
                }
                // (i1 i2 -- b)
                case OP_LESS: {
                    int lhs = UFA.unsafeGetInt(frame, sp - 2);
                    int rhs = UFA.unsafeGetInt(frame, sp - 1);
                    UFA.unsafeSetBoolean(frame, sp - 2, lhs < rhs);
                    sp -= 1;
                    bci += 1;
                    continue loop;
                }
                // (i -- )
                case OP_RETURN: {
                    return UFA.unsafeGetInt(frame, sp - 1);
                }
                // (i -- )
                case OP_ST_LOC: {
                    UFA.unsafeCopyPrimitive(frame, sp - 1, UFA.unsafeShortArrayRead(localBc, bci + 1));
                    sp -= 1;
                    bci += 2;
                    continue loop;
                }
                // ( -- i)
                case OP_LD_LOC: {
                    UFA.unsafeCopyPrimitive(frame, UFA.unsafeShortArrayRead(localBc, bci + 1), sp);
                    sp += 1;
                    bci += 2;
                    continue loop;
                }

                case OP_SI_0: {
                    // ld_loc
                    {
                        UFA.unsafeCopyPrimitive(frame, UFA.unsafeShortArrayRead(localBc, bci + 1), sp);
                        sp += 1;
                        bci += 2;
                    }
                    // const
                    {
                        UFA.unsafeSetInt(frame, sp, (UFA.unsafeShortArrayRead(localBc, bci + 2) << 16) | (UFA.unsafeShortArrayRead(localBc, bci + 1) & 0xffff));
                        sp += 1;
                        bci += 3;
                    }
                    // mod
                    {
                        int lhs = UFA.unsafeGetInt(frame, sp - 2);
                        int rhs = UFA.unsafeGetInt(frame, sp - 1);
                        UFA.unsafeSetInt(frame, sp - 2, lhs % rhs);
                        sp -= 1;
                        bci += 1;
                    }
                    // st_loc
                    {
                        UFA.unsafeCopyPrimitive(frame, sp - 1, UFA.unsafeShortArrayRead(localBc, bci + 1));
                        sp -= 1;
                        bci += 2;
                    }
                    continue loop;
                }
                case OP_SI_1: {
                    // ld_loc
                    {
                        UFA.unsafeCopyPrimitive(frame, UFA.unsafeShortArrayRead(localBc, bci + 1), sp);
                        sp += 1;
                        bci += 2;
                    }
                    // const
                    {
                        UFA.unsafeSetInt(frame, sp, (UFA.unsafeShortArrayRead(localBc, bci + 2) << 16) | (UFA.unsafeShortArrayRead(localBc, bci + 1) & 0xffff));
                        sp += 1;
                        bci += 3;
                    }
                    // mod
                    {
                        int lhs = UFA.unsafeGetInt(frame, sp - 2);
                        int rhs = UFA.unsafeGetInt(frame, sp - 1);
                        UFA.unsafeSetInt(frame, sp - 2, lhs % rhs);
                        sp -= 1;
                        bci += 1;
                    }
                    // const
                    {
                        UFA.unsafeSetInt(frame, sp, (UFA.unsafeShortArrayRead(localBc, bci + 2) << 16) | (UFA.unsafeShortArrayRead(localBc, bci + 1) & 0xffff));
                        sp += 1;
                        bci += 3;
                    }
                    // less
                    {
                        int lhs = UFA.unsafeGetInt(frame, sp - 2);
                        int rhs = UFA.unsafeGetInt(frame, sp - 1);
                        UFA.unsafeSetBoolean(frame, sp - 2, lhs < rhs);
                        sp -= 1;
                        bci += 1;
                    }
                    // jump_false
                    {
                        boolean cond = UFA.unsafeGetBoolean(frame, sp - 1);
                        sp -= 1;
                        if (!cond) {
                            bci = UFA.unsafeShortArrayRead(localBc, bci + 1);
                            continue loop;
                        } else {
                            bci += 2;
                            continue loop;
                        }
                    }
                }

                default:
                    CompilerDirectives.shouldNotReachHere();
            }
        }
    }
}

@GeneratedBy(ManualUnsafeWrappedBytecodeNode.class) // needed for UFA
class ManualUnsafeWrappedBytecodeNode extends BaseBytecodeNode {
    protected ManualUnsafeWrappedBytecodeNode(TruffleLanguage<?> language, FrameDescriptor frameDescriptor, short[] bc) {
        super(language, frameDescriptor, bc);
    }

    private static final UnsafeFrameAccess UFA = UnsafeFrameAccess.lookup();

    @ValueType
    private static class Tuple2 {
        @CompilationFinal private final int x0;
        @CompilationFinal private final int x1;

        Tuple2(int x0, int x1) {
            this.x0 = x0;
            this.x1 = x1;
        }
    }

    private Tuple2 executeBranch(VirtualFrame frame, int sp, short[] localBc, int bci, Counter loopCounter) {
        int nextBci = UFA.unsafeShortArrayRead(localBc, bci + 1);
        CompilerAsserts.partialEvaluationConstant(nextBci);
        if (nextBci <= bci) {
            Object result = backwardsJumpCheck(frame, sp, loopCounter, nextBci);
            if (result != null) {
                throw new UnsupportedOperationException();
            }
        }
        return new Tuple2(sp, nextBci);
    }

    private static Tuple2 executeAdd(VirtualFrame frame, int sp, int bci) {
        int lhs = UFA.unsafeGetInt(frame, sp - 2);
        int rhs = UFA.unsafeGetInt(frame, sp - 1);
        UFA.unsafeSetInt(frame, sp - 2, lhs + rhs);
        return new Tuple2(sp - 1, bci + 1);
    }

    private static Tuple2 executeMod(VirtualFrame frame, int sp, int bci) {
        int lhs = UFA.unsafeGetInt(frame, sp - 2);
        int rhs = UFA.unsafeGetInt(frame, sp - 1);
        UFA.unsafeSetInt(frame, sp - 2, lhs % rhs);
        return new Tuple2(sp - 1, bci + 1);
    }

    private static Tuple2 executeConst(VirtualFrame frame, int sp, short[] bc, int bci) {
        UFA.unsafeSetInt(frame, sp, (UFA.unsafeShortArrayRead(bc, bci + 2) << 16) | (UFA.unsafeShortArrayRead(bc, bci + 1) & 0xffff));
        return new Tuple2(sp + 1, bci + 3);
    }

    private static Tuple2 executeBranchFalse(VirtualFrame frame, int sp, short[] bc, int bci) {
        boolean cond = UFA.unsafeGetBoolean(frame, sp - 1);
        if (!cond) {
            return new Tuple2(sp - 1, UFA.unsafeShortArrayRead(bc, bci + 1));
        } else {
            return new Tuple2(sp - 1, bci + 2);
        }
    }

    private static Tuple2 executeLess(VirtualFrame frame, int sp, int bci) {
        int lhs = UFA.unsafeGetInt(frame, sp - 2);
        int rhs = UFA.unsafeGetInt(frame, sp - 1);
        UFA.unsafeSetBoolean(frame, sp - 2, lhs < rhs);
        return new Tuple2(sp - 1, bci + 1);
    }

    private static Tuple2 executeStoreLoc(VirtualFrame frame, int sp, short[] bc, int bci) {
        UFA.unsafeCopyPrimitive(frame, sp - 1, UFA.unsafeShortArrayRead(bc, bci + 1));
        return new Tuple2(sp - 1, bci + 2);
    }

    private static Tuple2 executeLoadLoc(VirtualFrame frame, int sp, short[] bc, int bci) {
        UFA.unsafeCopyPrimitive(frame, UFA.unsafeShortArrayRead(bc, bci + 1), sp);
        return new Tuple2(sp + 1, bci + 2);
    }

    @Override
    @BytecodeInterpreterSwitch
    @ExplodeLoop(kind = LoopExplosionKind.MERGE_EXPLODE)
    protected Object executeAt(VirtualFrame frame, int startBci, int startSp) {
        short[] localBc = bc;
        int bci = startBci;
        int sp = startSp;

        Counter loopCounter = new Counter();

        frame.getArguments();

        loop: while (true) {
            short opcode = UFA.unsafeShortArrayRead(localBc, bci);
            CompilerAsserts.partialEvaluationConstant(opcode);
            switch (opcode) {
                // ( -- )
                case OP_JUMP: {
                    Tuple2 t = executeBranch(frame, sp, bc, bci, loopCounter);
                    sp = t.x0;
                    bci = t.x1;
                    continue loop;
                }
                // (i1 i2 -- i3)
                case OP_ADD: {
                    Tuple2 t = executeAdd(frame, sp, bci);
                    sp = t.x0;
                    bci = t.x1;
                    continue loop;
                }
                // (i1 i2 -- i3)
                case OP_MOD: {
                    Tuple2 t = executeMod(frame, sp, bci);
                    sp = t.x0;
                    bci = t.x1;
                    continue loop;
                }
                // ( -- i)
                case OP_CONST: {
                    Tuple2 t = executeConst(frame, sp, localBc, bci);
                    sp = t.x0;
                    bci = t.x1;
                    continue loop;
                }
                // (b -- )
                case OP_JUMP_FALSE: {
                    Tuple2 t = executeBranchFalse(frame, sp, localBc, bci);
                    sp = t.x0;
                    bci = t.x1;
                    continue loop;
                }
                // (i1 i2 -- b)
                case OP_LESS: {
                    Tuple2 t = executeLess(frame, sp, bci);
                    sp = t.x0;
                    bci = t.x1;
                    continue loop;
                }
                // (i -- )
                case OP_RETURN: {
                    return UFA.unsafeGetInt(frame, sp - 1);
                }
                // (i -- )
                case OP_ST_LOC: {
                    Tuple2 t = executeStoreLoc(frame, sp, localBc, bci);
                    sp = t.x0;
                    bci = t.x1;
                    continue loop;
                }
                // ( -- i)
                case OP_LD_LOC: {
                    Tuple2 t = executeLoadLoc(frame, sp, localBc, bci);
                    sp = t.x0;
                    bci = t.x1;
                    continue loop;
                }
                default:
                    CompilerDirectives.shouldNotReachHere();
            }
        }
    }
}

abstract class BaseBytecodeNode extends RootNode implements BytecodeOSRNode {

    protected BaseBytecodeNode(TruffleLanguage<?> language, FrameDescriptor frameDescriptor, short[] bc) {
        super(language, frameDescriptor);
        this.bc = bc;
    }

    @CompilationFinal(dimensions = 1) protected short[] bc;

    static final short OP_JUMP = 1;
    static final short OP_CONST = 2;
    static final short OP_ADD = 3;
    static final short OP_JUMP_FALSE = 4;
    static final short OP_LESS = 5;
    static final short OP_RETURN = 6;
    static final short OP_ST_LOC = 7;
    static final short OP_LD_LOC = 8;
    static final short OP_MOD = 9;
    static final short OP_SI_0 = 10;
    static final short OP_SI_1 = 11;

    @CompilerDirectives.ValueType
    protected static class Counter {
        int count;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeAt(frame, 0, 0);
    }

    protected abstract Object executeAt(VirtualFrame osrFrame, int startBci, int startSp);

    protected final Object backwardsJumpCheck(VirtualFrame frame, int sp, Counter loopCounter, int nextBci) {
        if (CompilerDirectives.hasNextTier() && ++loopCounter.count >= 256) {
            TruffleSafepoint.poll(this);
            LoopNode.reportLoopCount(this, 256);
            loopCounter.count = 0;
        }

        if (CompilerDirectives.inInterpreter() && BytecodeOSRNode.pollOSRBackEdge(this)) {
            Object osrResult = BytecodeOSRNode.tryOSR(this, (sp << 16) | nextBci, null, null, frame);
            if (osrResult != null) {
                return osrResult;
            }
        }

        return null;
    }

    public Object executeOSR(VirtualFrame osrFrame, int target, Object interpreterState) {
        return executeAt(osrFrame, target & 0xffff, target >> 16);
    }

    @CompilationFinal private Object osrMetadata;

    public Object getOSRMetadata() {
        return osrMetadata;
    }

    public void setOSRMetadata(Object osrMetadata) {
        this.osrMetadata = osrMetadata;
    }
}

class ManualBytecodeNodeNBE extends BaseBytecodeNode {

    protected ManualBytecodeNodeNBE(TruffleLanguage<?> language, FrameDescriptor frameDescriptor, short[] bc) {
        super(language, frameDescriptor, bc);
    }

    @Override
    @BytecodeInterpreterSwitch
    @ExplodeLoop(kind = LoopExplosionKind.MERGE_EXPLODE)
    protected Object executeAt(VirtualFrame frame, int startBci, int startSp) {
        short[] localBc = bc;
        int bci = startBci;
        int sp = startSp;

        Counter counter = new Counter();

        loop: while (true) {
            short opcode = localBc[bci];
            CompilerAsserts.partialEvaluationConstant(opcode);
            switch (opcode) {
                // ( -- )
                case OP_JUMP: {
                    int nextBci = localBc[bci + 1];
                    CompilerAsserts.partialEvaluationConstant(nextBci);
                    if (nextBci <= bci) {
                        Object result = backwardsJumpCheck(frame, sp, counter, nextBci);
                        if (result != null) {
                            return result;
                        }
                    }
                    bci = nextBci;
                    continue loop;
                }
                // (i1 i2 -- i3)
                case OP_ADD: {
                    int lhs = (int) frame.getObject(sp - 2);
                    int rhs = (int) frame.getObject(sp - 1);
                    frame.setObject(sp - 2, lhs + rhs);
                    frame.clear(sp - 1);
                    sp -= 1;
                    bci += 1;
                    continue loop;
                }
                // (i1 i2 -- i3)
                case OP_MOD: {
                    int lhs = (int) frame.getObject(sp - 2);
                    int rhs = (int) frame.getObject(sp - 1);
                    frame.setObject(sp - 2, lhs % rhs);
                    frame.clear(sp - 1);
                    sp -= 1;
                    bci += 1;
                    continue loop;
                }
                // ( -- i)
                case OP_CONST: {
                    frame.setObject(sp, (localBc[bci + 1] << 16) | (localBc[bci + 2] & 0xffff));
                    sp += 1;
                    bci += 3;
                    continue loop;
                }
                // (b -- )
                case OP_JUMP_FALSE: {
                    boolean cond = frame.getObject(sp - 1) == Boolean.TRUE;
                    frame.clear(sp - 1);
                    sp -= 1;
                    if (!cond) {
                        bci = localBc[bci + 1];
                        continue loop;
                    } else {
                        bci += 2;
                        continue loop;
                    }
                }
                // (i1 i2 -- b)
                case OP_LESS: {
                    int lhs = (int) frame.getObject(sp - 2);
                    int rhs = (int) frame.getObject(sp - 1);
                    frame.setObject(sp - 2, lhs < rhs);
                    frame.clear(sp - 1);
                    sp -= 1;
                    bci += 1;
                    continue loop;
                }
                // (i -- )
                case OP_RETURN: {
                    return frame.getObject(sp - 1);
                }
                // (i -- )
                case OP_ST_LOC: {
                    frame.copy(sp - 1, localBc[bci + 1]);
                    frame.clear(sp - 1);
                    sp -= 1;
                    bci += 2;
                    continue loop;
                }
                // ( -- i)
                case OP_LD_LOC: {
                    frame.copy(localBc[bci + 1], sp);
                    sp += 1;
                    bci += 2;
                    continue loop;
                }
                default:
                    CompilerDirectives.shouldNotReachHere();
            }
        }
    }
}
