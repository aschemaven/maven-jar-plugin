---
title: Frequently Asked Questions
---

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<a id="top"></a>

# Frequently Asked Questions

1. [Why is there no documentation for the archive parameter?](#question1)
2. [Why does my build fail on JDK 17 or 18 with a reproducible build error?](#question2)

<a id="question1"></a>

### Why is there no documentation for the archive parameter?

The `archive` parameter is defined by the Maven Archiver library, not by the Maven JAR Plugin. Read the
[Maven Archiver documentation](/shared/maven-archiver/index.html).

<a id="question2"></a>

### Why does my build fail on JDK 17 or 18 with a reproducible build error?

Maven 4 requests a [reproducible build](https://maven.apache.org/guides/mini/guide-reproducible-builds.html)
by default: its super POM defines the `project.build.outputTimestamp` property for every project. To honor
it, the plugin normalizes the timestamps of the JAR entries using the `--date` option of the JDK `jar` tool,
which is available only with JDK version 19 or later.

Rather than silently produce a non-reproducible archive on JDK 17 or 18, the plugin fails the build. You have
three options:

* run the build with JDK 19 or later (the compilation target release can still be 17 or lower);
* opt out of the reproducible build by clearing the property &mdash;
  `<project.build.outputTimestamp></project.build.outputTimestamp>` in your POM, or
  `-Dproject.build.outputTimestamp=` on the command line; or
* use version 3.x of the Maven JAR Plugin, which normalizes the timestamps through Maven Archiver and
  therefore does not depend on the `jar` tool.
