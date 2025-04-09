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

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.writeTo

private val SUBSCRIBER_INFO_INDEX = ClassName("org.greenrobot.eventbus.meta", "SubscriberInfoIndex")
private val SUBSCRIBER_INFO = ClassName("org.greenrobot.eventbus.meta", "SubscriberInfo")
private val SUBSCRIBER_METHOD_INFO = ClassName("org.greenrobot.eventbus.meta", "SubscriberMethodInfo")
private val SIMPLE_SUBSCRIBER_INFO = ClassName("org.greenrobot.eventbus.meta", "SimpleSubscriberInfo")

private val CLASS_STAR = Class::class.asClassName().parameterizedBy(STAR)

private class SubscriberTypeKey(
    val rawClassName: TypeName,
    val file: KSFile,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return rawClassName == (other as SubscriberTypeKey).rawClassName
    }

    override fun hashCode(): Int {
        return rawClassName.hashCode()
    }
}

fun CodeGenerator.write(
    className: String,
    methods: List<SubscribeMethod>,
) {
    val subscribers = methods.groupBy { SubscriberTypeKey(it.rawClassName, it.file) }

    val name = ClassName.bestGuess(className)

    FileSpec.builder(name).addType(
        TypeSpec.objectBuilder(name)
            .addSuperinterface(SUBSCRIBER_INFO_INDEX)
            .addProperty(
                PropertySpec.builder(
                    "index",
                    Map::class.asClassName().parameterizedBy(
                        CLASS_STAR,
                        SUBSCRIBER_INFO,
                    ),
                    KModifier.PRIVATE,
                ).initializer(
                    CodeBlock.builder()
                        .add("mapOf(")
                        .apply {
                            for ((type, subscriptions) in subscribers) {
                                add(
                                    "%T::class.java to %T(%T::class.java, true, arrayOf(\n",
                                    type.rawClassName,
                                    SIMPLE_SUBSCRIBER_INFO,
                                    type.rawClassName,
                                )

                                indent()

                                for (subscription in subscriptions) {
                                    add(
                                        "%T(%S, %T::class.java, %T, %L, %L),\n",
                                        SUBSCRIBER_METHOD_INFO,
                                        subscription.method,
                                        subscription.eventType,
                                        subscription.annotation.threadMode,
                                        subscription.annotation.priority,
                                        subscription.annotation.sticky,
                                    )
                                }

                                unindent()

                                add(")),\n")
                            }
                        }
                        .add(")")
                        .build(),
                ).build(),
            )
            .addFunction(
                FunSpec.builder("getSubscriberInfo")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter(
                        ParameterSpec("subscriberClass", CLASS_STAR),
                    ).returns(
                        SUBSCRIBER_INFO.copy(nullable = true),
                    )
                    .addCode("return %N[%N]", "index", "subscriberClass")
                    .build(),
            ).build(),
    ).build().writeTo(
        this,
        Dependencies(
            aggregating = true,
            sources = subscribers.keys.map { it.file }.toTypedArray()
        )
    )
}
