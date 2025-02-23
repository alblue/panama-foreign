/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

/*
 * @test
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestUpcallStubs
 */

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.testng.annotations.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.ref.Cleaner;

import static jdk.incubator.foreign.MemoryLayouts.JAVA_INT;
import static org.testng.Assert.assertFalse;

public class TestUpcallStubs {

    static final CLinker abi = CLinker.getInstance();
    static final MethodHandle MH_dummy;

    static {
        try {
            MH_dummy = MethodHandles.lookup()
                .findStatic(TestUpcallStubs.class, "dummy", MethodType.methodType(void.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new BootstrapMethodError(e);
        }
    }

    private static MemorySegment getStub() {
        return abi.upcallStub(MH_dummy, FunctionDescriptor.ofVoid(), ResourceScope.ofConfined());
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testNoAccess() {
        MemorySegment stub = getStub();
        VarHandle vh = JAVA_INT.varHandle(int.class);
        vh.set(stub, 10);
        stub.scope().close();
    }

    @Test
    public void testFree() {
        MemorySegment stub = getStub();
        stub.scope().close();
        assertFalse(stub.scope().isAlive());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testAlreadyFreed() {
        MemorySegment stub = getStub();
        stub.scope().close();
        // should fail
        stub.scope().close();
    }

    @DataProvider
    public static Object[][] badAddresses() {
        return new Object[][]{
            { MemoryAddress.ofLong(42) /* random address */ },
            { MemorySegment.ofArray(new int []{ 1, 2, 3 }).address() /* heap address */ }
        };
    }

    // where
    public static void dummy() {}

}
