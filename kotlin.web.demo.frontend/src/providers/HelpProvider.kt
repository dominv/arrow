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

package providers

import views.ActionStatusMessage

class HelpProvider(
        private val onFail: (String, ActionStatusMessage)->Unit
) {
    private var helpElements : Array<HelpElement>? = null

    private fun loadAllHelpElements() {
        ajax(
                url = generateAjaxUrl("loadHelpForWords"),
                success = { data: Array<HelpElement> ->
                    if (checkDataForNull(data)) {
                        helpElements = data
                    } else {
                        onFail("Incorrect data format.", ActionStatusMessage.load_help_for_words_fail)
                    }
                },
                dataType = DataType.JSON,
                type = HTTPRequestType.GET,
                timeout = 30000,
                error = { jqXHR, textStatus, errorThrown ->
                    onFail(textStatus + " : " + errorThrown, ActionStatusMessage.load_help_for_words_fail)
                }
        )
    }

    init {
        loadAllHelpElements()
    }

    fun getHelpForWord(word: String): String?{
        return helpElements?.firstOrNull {
            it.word == word
        }?.help
    }

}

data class HelpElement(val word: String,val help: String)
