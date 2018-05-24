/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test
 */

import java.lang.invoke.MethodHandles;
import java.nicl.*;
import java.nicl.metadata.*;

public class Hello {
    @NativeHeader(libraries = { "Hello" })
    static interface hello {
        @NativeLocation(file="dummy", line=1, column=1, USR="c:@F@func")
        @NativeType(layout="()i32", ctype="dummy")
        public abstract int func();
    }

    public static void main(String[] args) {
        // "hello" library mentioned already in annotation (LibraryDependencies)
        // No need to explicitly call Libraries.loadLibrary!
        hello i = Libraries.bind(MethodHandles.lookup(), hello.class);
        int value = i.func();
        if (value != 42) {
            throw new RuntimeException("Expected 42, but got " + i);
        }
    }
}
