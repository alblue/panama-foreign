/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.foreign.abi.x64.windows;

import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.internal.foreign.AbstractCLinker;
import jdk.internal.foreign.MemoryScope;
import jdk.internal.foreign.abi.Binding;
import jdk.internal.foreign.abi.SharedUtils;
import jdk.internal.foreign.abi.UpcallStubs;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * ABI implementation based on Windows ABI AMD64 supplement v.0.99.6
 */
public final class Windowsx64Linker extends AbstractCLinker {

    public static final int MAX_INTEGER_ARGUMENT_REGISTERS = 4;
    public static final int MAX_INTEGER_RETURN_REGISTERS = 1;
    public static final int MAX_VECTOR_ARGUMENT_REGISTERS = 4;
    public static final int MAX_VECTOR_RETURN_REGISTERS = 1;
    public static final int MAX_REGISTER_ARGUMENTS = 4;
    public static final int MAX_REGISTER_RETURNS = 1;

    private static Windowsx64Linker instance;

    static final long ADDRESS_SIZE = 64; // bits

    private static final MethodHandle MH_unboxVaList;
    private static final MethodHandle MH_boxVaList;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MH_unboxVaList = lookup.findVirtual(VaList.class, "address",
                MethodType.methodType(MemoryAddress.class));
            MH_boxVaList = MethodHandles.insertArguments(lookup.findStatic(Windowsx64Linker.class, "newVaListOfAddress",
                MethodType.methodType(VaList.class, MemoryAddress.class, ResourceScope.class)), 1, ResourceScope.globalScope());
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Windowsx64Linker getInstance() {
        if (instance == null) {
            instance = new Windowsx64Linker();
        }
        return instance;
    }

    public static VaList newVaList(Consumer<VaList.Builder> actions, ResourceScope scope) {
        WinVaList.Builder builder = WinVaList.builder(scope);
        actions.accept(builder);
        return builder.build();
    }

    @Override
    @CallerSensitive
    public final MethodHandle downcallHandle(MethodType type, FunctionDescriptor function) {
        Reflection.ensureNativeAccess(Reflection.getCallerClass());
        Objects.requireNonNull(type);
        Objects.requireNonNull(function);
        MethodType llMt = SharedUtils.convertVaListCarriers(type, WinVaList.CARRIER);
        MethodHandle handle = CallArranger.arrangeDowncall(llMt, function);
        if (!type.returnType().equals(MemorySegment.class)) {
            // not returning segment, just insert a throwing allocator
            handle = MethodHandles.insertArguments(handle, 1, SharedUtils.THROWING_ALLOCATOR);
        }
        handle = SharedUtils.unboxVaLists(type, handle, MH_unboxVaList);
        return handle;
    }

    @Override
    @CallerSensitive
    public final MemorySegment upcallStub(MethodHandle target, FunctionDescriptor function, ResourceScope scope) {
        Reflection.ensureNativeAccess(Reflection.getCallerClass());
        Objects.requireNonNull(scope);
        Objects.requireNonNull(target);
        Objects.requireNonNull(function);
        target = SharedUtils.boxVaLists(target, MH_boxVaList);
        return UpcallStubs.upcallAddress(CallArranger.arrangeUpcall(target, target.type(), function), (MemoryScope) scope);
    }

    public static VaList newVaListOfAddress(MemoryAddress ma, ResourceScope scope) {
        return WinVaList.ofAddress(ma, scope);
    }

    public static VaList emptyVaList() {
        return WinVaList.empty();
    }
}
