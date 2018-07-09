/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.webdemo.backend;

import org.jetbrains.webdemo.kotlin.KotlinWrapper;
import org.jetbrains.webdemo.kotlin.KotlinWrappersManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class Initializer {
    public static void initializeExecutorsPolicyFile(Path templateFilePath) throws IOException {
        for (KotlinWrapper wrapper : KotlinWrappersManager.INSTANCE.getAllWrappers()) {
            String templateFileContent = new String(Files.readAllBytes(templateFilePath));
            String wrapperPath = wrapper.getWrapperFolder() + "/libraries";
            String policyFileContent = templateFileContent.replaceAll("@LIBS_DIR@", BackendSettings.EXECUTORS_LIBS_DIR.replaceAll("\\\\", "/"));
            policyFileContent = policyFileContent.replaceAll("@KOTLIN_RUNTIME@", wrapper.getKotlinRuntimeJar().toString().replaceAll("\\\\", "/"));
            policyFileContent = policyFileContent.replaceAll("@WRAPPERS_LIB@", wrapperPath.replaceAll("\\\\", "/"));
            try (PrintWriter policyFile = new PrintWriter(wrapper.getWrapperFolder().resolve("executors.policy").toFile())) {
                policyFile.write(policyFileContent);
            }

        }
    }
}
