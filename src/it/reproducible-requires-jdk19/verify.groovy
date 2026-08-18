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

// The build is expected to have failed (invoker.buildResult = failure). Confirm that it failed for
// the right reason: the plugin refused to produce a non-reproducible archive on this Java version.
File buildLog = new File(basedir, "build.log")
if (!buildLog.isFile()) {
    System.err.println("build.log is missing: " + buildLog)
    return false
}

String log = buildLog.getText("UTF-8")
if (!log.contains("A reproducible build was requested") || !log.contains("requires Java 19 or later")) {
    System.err.println("build.log does not contain the expected reproducible-build failure message")
    return false
}

return true
