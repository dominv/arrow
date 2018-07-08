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

package views.tabs

import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLParagraphElement
import utils.codemirror.CodeMirror
import utils.unEscapeString
import kotlin.browser.document

class GeneratedCodeView(private val element: HTMLElement) {

    fun clear() {
        element.innerHTML = ""
    }

    fun setOutput(data: dynamic) {
        element.innerHTML = ""
        val generatedCode = document.createElement("p") as HTMLParagraphElement
        generatedCode.className = "consoleViewInfo"
        generatedCode.innerHTML = unEscapeString(data.text)
        element.appendChild(generatedCode)
    }

    fun showGeneratedCode(code: String) {
        element.innerHTML = ""
        val generatedCode = document.createElement("p") as HTMLParagraphElement
        generatedCode.className = "cm-s-default"
        generatedCode.innerHTML = unEscapeString(code)
        CodeMirror.runMode(code, "javascript", generatedCode)
        element.appendChild(generatedCode)
    }
}