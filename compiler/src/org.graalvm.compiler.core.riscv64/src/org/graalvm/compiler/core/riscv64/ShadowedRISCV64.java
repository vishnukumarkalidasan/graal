/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.compiler.core.riscv64;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterArray;

public class ShadowedRISCV64 {
    public static final Class<?> riscv64 = RISCV64ReflectionUtil.getArch(false);

    public static final Register x0 = RISCV64ReflectionUtil.readStaticField(riscv64, "x0");
    public static final Register x1 = RISCV64ReflectionUtil.readStaticField(riscv64, "x1");
    public static final Register x2 = RISCV64ReflectionUtil.readStaticField(riscv64, "x2");
    public static final Register x3 = RISCV64ReflectionUtil.readStaticField(riscv64, "x3");
    public static final Register x4 = RISCV64ReflectionUtil.readStaticField(riscv64, "x4");
    public static final Register x5 = RISCV64ReflectionUtil.readStaticField(riscv64, "x5");
    public static final Register x6 = RISCV64ReflectionUtil.readStaticField(riscv64, "x6");
    public static final Register x7 = RISCV64ReflectionUtil.readStaticField(riscv64, "x7");
    public static final Register x8 = RISCV64ReflectionUtil.readStaticField(riscv64, "x8");
    public static final Register x9 = RISCV64ReflectionUtil.readStaticField(riscv64, "x9");
    public static final Register x10 = RISCV64ReflectionUtil.readStaticField(riscv64, "x10");
    public static final Register x11 = RISCV64ReflectionUtil.readStaticField(riscv64, "x11");
    public static final Register x12 = RISCV64ReflectionUtil.readStaticField(riscv64, "x12");
    public static final Register x13 = RISCV64ReflectionUtil.readStaticField(riscv64, "x13");
    public static final Register x14 = RISCV64ReflectionUtil.readStaticField(riscv64, "x14");
    public static final Register x15 = RISCV64ReflectionUtil.readStaticField(riscv64, "x15");
    public static final Register x16 = RISCV64ReflectionUtil.readStaticField(riscv64, "x16");
    public static final Register x17 = RISCV64ReflectionUtil.readStaticField(riscv64, "x17");
    public static final Register x18 = RISCV64ReflectionUtil.readStaticField(riscv64, "x18");
    public static final Register x19 = RISCV64ReflectionUtil.readStaticField(riscv64, "x19");
    public static final Register x20 = RISCV64ReflectionUtil.readStaticField(riscv64, "x20");
    public static final Register x21 = RISCV64ReflectionUtil.readStaticField(riscv64, "x21");
    public static final Register x22 = RISCV64ReflectionUtil.readStaticField(riscv64, "x22");
    public static final Register x23 = RISCV64ReflectionUtil.readStaticField(riscv64, "x23");
    public static final Register x24 = RISCV64ReflectionUtil.readStaticField(riscv64, "x24");
    public static final Register x25 = RISCV64ReflectionUtil.readStaticField(riscv64, "x25");
    public static final Register x26 = RISCV64ReflectionUtil.readStaticField(riscv64, "x26");
    public static final Register x27 = RISCV64ReflectionUtil.readStaticField(riscv64, "x27");
    public static final Register x28 = RISCV64ReflectionUtil.readStaticField(riscv64, "x28");
    public static final Register x29 = RISCV64ReflectionUtil.readStaticField(riscv64, "x29");
    public static final Register x30 = RISCV64ReflectionUtil.readStaticField(riscv64, "x30");
    public static final Register x31 = RISCV64ReflectionUtil.readStaticField(riscv64, "x31");

    public static final Register f0 = RISCV64ReflectionUtil.readStaticField(riscv64, "f0");
    public static final Register f1 = RISCV64ReflectionUtil.readStaticField(riscv64, "f1");
    public static final Register f2 = RISCV64ReflectionUtil.readStaticField(riscv64, "f2");
    public static final Register f3 = RISCV64ReflectionUtil.readStaticField(riscv64, "f3");
    public static final Register f4 = RISCV64ReflectionUtil.readStaticField(riscv64, "f4");
    public static final Register f5 = RISCV64ReflectionUtil.readStaticField(riscv64, "f5");
    public static final Register f6 = RISCV64ReflectionUtil.readStaticField(riscv64, "f6");
    public static final Register f7 = RISCV64ReflectionUtil.readStaticField(riscv64, "f7");
    public static final Register f8 = RISCV64ReflectionUtil.readStaticField(riscv64, "f8");
    public static final Register f9 = RISCV64ReflectionUtil.readStaticField(riscv64, "f9");
    public static final Register f10 = RISCV64ReflectionUtil.readStaticField(riscv64, "f10");
    public static final Register f11 = RISCV64ReflectionUtil.readStaticField(riscv64, "f11");
    public static final Register f12 = RISCV64ReflectionUtil.readStaticField(riscv64, "f12");
    public static final Register f13 = RISCV64ReflectionUtil.readStaticField(riscv64, "f13");
    public static final Register f14 = RISCV64ReflectionUtil.readStaticField(riscv64, "f14");
    public static final Register f15 = RISCV64ReflectionUtil.readStaticField(riscv64, "f15");
    public static final Register f16 = RISCV64ReflectionUtil.readStaticField(riscv64, "f16");
    public static final Register f17 = RISCV64ReflectionUtil.readStaticField(riscv64, "f17");
    public static final Register f18 = RISCV64ReflectionUtil.readStaticField(riscv64, "f18");
    public static final Register f19 = RISCV64ReflectionUtil.readStaticField(riscv64, "f19");
    public static final Register f20 = RISCV64ReflectionUtil.readStaticField(riscv64, "f20");
    public static final Register f21 = RISCV64ReflectionUtil.readStaticField(riscv64, "f21");
    public static final Register f22 = RISCV64ReflectionUtil.readStaticField(riscv64, "f22");
    public static final Register f23 = RISCV64ReflectionUtil.readStaticField(riscv64, "f23");
    public static final Register f24 = RISCV64ReflectionUtil.readStaticField(riscv64, "f24");
    public static final Register f25 = RISCV64ReflectionUtil.readStaticField(riscv64, "f25");
    public static final Register f26 = RISCV64ReflectionUtil.readStaticField(riscv64, "f26");
    public static final Register f27 = RISCV64ReflectionUtil.readStaticField(riscv64, "f27");
    public static final Register f28 = RISCV64ReflectionUtil.readStaticField(riscv64, "f28");
    public static final Register f29 = RISCV64ReflectionUtil.readStaticField(riscv64, "f29");
    public static final Register f30 = RISCV64ReflectionUtil.readStaticField(riscv64, "f30");
    public static final Register f31 = RISCV64ReflectionUtil.readStaticField(riscv64, "f31");

    public static final RegisterArray allRegisters = RISCV64ReflectionUtil.readStaticField(riscv64, "allRegisters");
}
