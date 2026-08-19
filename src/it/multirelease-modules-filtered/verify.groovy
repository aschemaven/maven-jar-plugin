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

import java.util.jar.*;

/*
 * This IT stacks the three cases the whole-directory shortcut deliberately does NOT cover, so the plugin
 * falls back to per-file enumeration for all of them at once:
 *   - a module source hierarchy (modules foo.bar and foo.bar.more) -> one JAR per module,
 *   - multi-release (base classes plus a Java 16 override under META-INF/versions/16),
 *   - an include/exclude filter (the POM's excludes drop ExcludedByFilter.class).
 *
 * verify() below asserts the EXACT set of file entries per JAR: every file in the JAR must be listed here
 * (no extras), and every listed entry must be present (nothing missing). ExcludedByFilter.class is
 * intentionally NOT in the expected set -- if the filter were not honored, its presence would fail the
 * assertion. That is what proves the exclude works even for a modular, multi-release JAR.
 */
File target = new File(basedir, "target");

// Module foo.bar: base classes, the Java 16 override, the manifest and the Maven descriptor.
Set<String> content = new HashSet<>();
content.add("module-info.class")                          // this is a modular JAR
content.add("foo/MainFile.class")                         // base class, also the declared Main-Class
content.add("foo/OtherFile.class")                        // base class
content.add("META-INF/versions/16/foo/OtherFile.class")   // multi-release override for Java 16
content.add("META-INF/MANIFEST.MF")
content.add("META-INF/maven/org.apache.maven.plugins/multirelease-modules-filtered/pom.xml")
content.add("META-INF/maven/org.apache.maven.plugins/multirelease-modules-filtered/pom.properties")
// Note: foo/ExcludedByFilter.class is deliberately absent -- removed by the <excludes> filter.
verify(new File(target, "foo.bar-1.0-SNAPSHOT.jar"), content, "foo.MainFile")

content.clear()
// Module foo.bar.more: the filter does not match anything here, so the full set is expected.
content.add("module-info.class")
content.add("more/MainFile.class")
content.add("more/OtherFile.class")
content.add("META-INF/versions/16/more/OtherFile.class")
content.add("META-INF/MANIFEST.MF")
content.add("META-INF/maven/org.apache.maven.plugins/multirelease-modules-filtered/pom.xml")
content.add("META-INF/maven/org.apache.maven.plugins/multirelease-modules-filtered/pom.properties")
verify(new File(target, "foo.bar.more-1.0-SNAPSHOT.jar"), content, null)

// Asserts the JAR's file entries are EXACTLY the given set (directory entries are ignored here), and that
// the JAR is flagged Multi-Release with the expected Main-Class.
void verify(File artifact, Set<String> content, String mainClass)
{
    JarFile jar = new JarFile(artifact)
    Enumeration jarEntries = jar.entries()
    while (jarEntries.hasMoreElements())
    {
        JarEntry entry = (JarEntry) jarEntries.nextElement()
        if (!entry.isDirectory())
        {
            String name = entry.getName()
            assert content.remove(name) : "Missing entry: " + name
        }
    }
    assert content.isEmpty() : "Unexpected entries: " + content

    Attributes attributes = jar.getManifest().getMainAttributes()
    assert Objects.equals("true", attributes.get(Attributes.Name.MULTI_RELEASE))
    assert Objects.equals(mainClass, attributes.get(Attributes.Name.MAIN_CLASS))

    jar.close();
}
