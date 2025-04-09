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

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toClassName

const val SUBSCRIBE_SHORT = "Subscribe"
const val SUBSCRIBE = "org.greenrobot.eventbus.$SUBSCRIBE_SHORT"

private fun parseSubscribeAnnotation(annotation: KSAnnotation): MaybeSubscribeAnnotation {
    val args = annotation.arguments.associateBy({ it.name!!.getShortName() }, { it.value })

    val threadMode = args["threadMode"]

    val threadModelEnumValue = if (threadMode is KSType) {
        // KSP1
        threadMode.toClassName()
    } else if (threadMode is KSClassDeclaration) {
        // KSP2
        threadMode.toClassName()
    } else{
        return InvalidSubscribeAnnotation("@Subscribe.threadMode = $threadMode is not a valid enum value")
    }

    val sticky = args["sticky"]

    if (sticky !is Boolean) {
        return InvalidSubscribeAnnotation("@Subscribe.sticky = $sticky is not a Boolean value")
    }

    val priority = args["priority"]

    if (priority !is Int) {
        return InvalidSubscribeAnnotation("@Subscribe.priority = $priority is not an Int value")
    }

    return SubscribeAnnotation(threadModelEnumValue, sticky, priority)
}

private fun KSAnnotated.subscriberAnnotation() = annotations.find {
    it.shortName.getShortName() == SUBSCRIBE_SHORT &&
        it.annotationType.resolve().declaration.qualifiedName?.asString() == SUBSCRIBE
}

fun parseSubscribeMethod(method: KSAnnotated): MaybeSubscribeMethod {
    if (method !is KSFunctionDeclaration) {
        return InvalidSubscribeMethod("$method is not a method")
    }

    val parent = method.parentDeclaration

    if (parent !is KSClassDeclaration) {
        return InvalidSubscribeMethod("$method lacks a valid parent declaration")
    }

    val parentFile = parent.containingFile ?: return InvalidSubscribeMethod("$method's parent does not have a file")

    val annotation = method.subscriberAnnotation()
        ?: return InvalidSubscribeMethod("$method's is missing the @Subscribe annotation, something must be very wrong")

    if (method.parameters.size != 1) {
        return InvalidSubscribeMethod("$method's parameters must have exactly one parameter")
    }

    val eventType = (method.parameters[0].type.resolve().declaration as KSClassDeclaration).toClassName()

    return when (val subscribeAnnotation = parseSubscribeAnnotation(annotation)) {
        is SubscribeAnnotation -> SubscribeMethod(
            parent.toClassName(),
            parentFile,
            method.simpleName.getShortName(),
            eventType,
            subscribeAnnotation
        )

        is InvalidSubscribeAnnotation -> InvalidSubscribeMethod("$method's ${subscribeAnnotation.why}")
    }
}
