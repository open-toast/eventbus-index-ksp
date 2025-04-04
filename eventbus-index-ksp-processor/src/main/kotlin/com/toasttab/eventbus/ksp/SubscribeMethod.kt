/*
 * Copyright (c) 2025 Toast Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.toasttab.eventbus.ksp

import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

sealed interface MaybeSubscribeMethod

class InvalidSubscribeMethod(
    val why: String,
) : MaybeSubscribeMethod

data class SubscribeMethod(
    val rawClassName: TypeName,
    val file: KSFile,
    val method: String,
    val eventType: TypeName,
    var annotation: SubscribeAnnotation,
) : MaybeSubscribeMethod

fun Sequence<MaybeSubscribeMethod>.partition(): Pair<List<SubscribeMethod>, List<InvalidSubscribeMethod>> =
    Pair(mutableListOf<SubscribeMethod>(), mutableListOf<InvalidSubscribeMethod>()).also {
        for (method in this) {
            when (method) {
                is SubscribeMethod -> it.first.add(method)
                is InvalidSubscribeMethod -> it.second.add(method)
            }
        }
    }

sealed interface MaybeSubscribeAnnotation

data class SubscribeAnnotation(
    val threadMode: ClassName,
    val sticky: Boolean,
    val priority: Int,
) : MaybeSubscribeAnnotation

class InvalidSubscribeAnnotation(
    val why: String,
) : MaybeSubscribeAnnotation
