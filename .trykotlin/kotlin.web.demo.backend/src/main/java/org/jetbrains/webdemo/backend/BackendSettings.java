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


/**
 * Created by Semyon.Atamas on 2/11/2015.
 */
public class BackendSettings {
    public static String JAVA_HOME = null;
    public static String JAVA_EXECUTE = "";
    public static String CLASS_PATH = "";
    public static String EXECUTORS_LIBS_DIR = "";
    public static String KOTLIN_LIBS_DIR = "";
    public static String KOTLIN_ERROR_MESSAGE = "Exception in Kotlin kotlin: a bug was reported to developers.";
    public static int TIMEOUT_FOR_EXECUTION = 10000; //seconds
    public static int MAX_OUTPUT_SIZE = 100 * 1024;

    public static String OUTPUT_DIRECTORY = "out";
}
