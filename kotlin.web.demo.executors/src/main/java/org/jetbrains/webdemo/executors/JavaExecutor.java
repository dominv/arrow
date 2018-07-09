package org.jetbrains.webdemo.executors;/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Created by Semyon.Atamas on 11/27/2014.
 */
public class JavaExecutor {
    private static ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private static ErrorStream errorOutputStream = new ErrorStream(outputStream);
    private static OutStream standardOutputStream = new OutStream(outputStream);

    public static void main(String[] args) {
        PrintStream defaultOutputStream = System.out;
        try {
            System.setOut(new PrintStream(standardOutputStream));
            System.setErr(new PrintStream(errorOutputStream));

            RunOutput outputObj = new RunOutput();
            String className;
            if (args.length > 0) {
                className = args[0];
                try {
                    Method mainMethod = Class.forName(className).getMethod("main", String[].class);
                    mainMethod.invoke(null, (Object) Arrays.copyOfRange(args, 1, args.length));
                } catch (InvocationTargetException e) {
                    outputObj.exception = e.getCause();
                } catch (NoSuchMethodException e) {
                    System.err.println("No main method found in project.");
                } catch (ClassNotFoundException e) {
                    System.err.println("No main method found in project.");
                }
            } else {
                System.err.println("No main method found in project.");
            }

            System.out.flush();
            System.err.flush();
            System.setOut(defaultOutputStream);
            outputObj.text = outputStream.toString()
                    .replaceAll("</errStream><errStream>", "")
                    .replaceAll("</outStream><outStream>", "");
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(Throwable.class, new ThrowableSerializer());
            objectMapper.registerModule(module);
            System.out.print(objectMapper.writeValueAsString(outputObj));
        } catch (Throwable e) {
            System.setOut(defaultOutputStream);
            System.out.println("{\"text\":\"<errStream>" + e.getClass().getName() + ": " + e.getMessage());
            System.out.print("</errStream>\"}");
        }

    }
}

class RunOutput {
    public String text = "";
    public Throwable exception = null;
}
