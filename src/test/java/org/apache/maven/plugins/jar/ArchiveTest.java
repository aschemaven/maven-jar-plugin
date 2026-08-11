/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.jar;

import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Archive}, focused on the two behaviours that are otherwise only
 * exercised by integration tests whose outcome depends on the (unspecified) filesystem
 * directory-iteration order, and therefore pass on some platforms while failing on others.
 */
class ArchiveTest {

    /**
     * Creates an {@code Archive} suitable for a unit test. {@code forceCreation = true} skips the
     * existing-JAR timestamp check, so the (here {@code null}) logger is never dereferenced.
     */
    private static Archive archive(String moduleName, Path directory) {
        return new Archive(directory.resolve("out.jar"), moduleName, directory, true, null);
    }

    private static Manifest manifestWithMainClass(String value) {
        Manifest m = new Manifest();
        m.getMainAttributes().putValue("Manifest-Version", "1.0");
        m.getMainAttributes().put(Attributes.Name.MAIN_CLASS, value);
        return m;
    }

    private static Object mainClassOf(Manifest m) {
        return m.getMainAttributes().get(Attributes.Name.MAIN_CLASS);
    }

    // ---------------------------------------------------------------------------------------------
    // setMainClass ownership for a module-qualified `module/Class` main class.
    // ---------------------------------------------------------------------------------------------

    @Test
    void owningModuleClaimsMainClassAndRemovesItFromManifest() {
        Archive owner = archive("foo.bar", Path.of("."));
        Manifest m = manifestWithMainClass("foo.bar/foo.MainFile");
        // The owner keeps the main class (emitted via --main-class) ...
        assertTrue(owner.setMainClass(m));
        // ... and the raw `module/Class` value is removed from the written manifest.
        assertNull(mainClassOf(m));
    }

    @Test
    void nonOwningModuleRejectsMainClass() {
        Archive nonOwner = archive("foo.bar.more", Path.of("."));
        Manifest m = manifestWithMainClass("foo.bar/foo.MainFile");
        assertFalse(nonOwner.setMainClass(m));
        assertNull(mainClassOf(m));
    }

    // ---------------------------------------------------------------------------------------------
    // The regression: which module keeps the main class must not depend on processing order.
    // ToolExecutor gives each module a *copy* of the shared plugin manifest; this pins that the
    // owning module (and only it) keeps the main class in either order, and that the shared
    // manifest is never consumed.
    // ---------------------------------------------------------------------------------------------

    @Test
    void mainClassAssignmentIsIndependentOfModuleOrder() {
        assertOwnership("foo.bar", "foo.bar.more"); // owner processed first
        assertOwnership("foo.bar.more", "foo.bar"); // non-owner processed first
    }

    private static void assertOwnership(String first, String second) {
        Manifest shared = manifestWithMainClass("foo.bar/foo.MainFile");
        boolean firstKeeps = archive(first, Path.of(".")).setMainClass(new Manifest(shared));
        boolean secondKeeps = archive(second, Path.of(".")).setMainClass(new Manifest(shared));
        assertEquals("foo.bar".equals(first), firstKeeps, "wrong owner for module processed first");
        assertEquals("foo.bar".equals(second), secondKeeps, "wrong owner for module processed second");
        // Per-module copies must leave the shared plugin manifest untouched.
        assertEquals("foo.bar/foo.MainFile", mainClassOf(shared));
    }

    // ---------------------------------------------------------------------------------------------
    // The base (version-less) release must bind to the true `<module>` directory even when the
    // Archive was first created from a `META-INF/versions-modular/<n>/<module>` directory (which
    // happens when the file-tree walk visits the version directory first).
    // ---------------------------------------------------------------------------------------------

    @Test
    void baseReleaseBindingIsIndependentOfDirectoryOrder() {
        Path base = Path.of("classes", "foo.bar");
        Path v16 = Path.of("classes", "META-INF", "versions-modular", "16", "foo.bar");
        Runtime.Version r16 = Runtime.Version.parse("16");

        // Version-directory first (the failing order): Archive seeded from v16, base registered later.
        Archive a = archive("foo.bar", v16);
        a.newTargetRelease(v16, r16);
        a.newTargetRelease(base, null);
        assertEquals(base, a.baseRelease().directory, "base must rebind to the version-less directory");

        // Base-directory first: still correct.
        Archive b = archive("foo.bar", base);
        b.newTargetRelease(base, null);
        b.newTargetRelease(v16, r16);
        assertEquals(base, b.baseRelease().directory);
    }
}
