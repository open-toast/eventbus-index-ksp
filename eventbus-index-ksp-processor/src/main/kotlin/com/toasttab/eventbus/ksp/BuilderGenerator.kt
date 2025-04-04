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
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
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
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

private const val SUBSCRIBE_SHORT = "Subscribe"
private const val SUBSCRIBE = "org.greenrobot.eventbus.$SUBSCRIBE_SHORT"

private const val OPTION_EVENT_BUS_INDEX = "eventBusIndex"

private val SUBSCRIBER_INFO_INDEX = ClassName("org.greenrobot.eventbus.meta", "SubscriberInfoIndex")
private val SUBSCRIBER_INFO = ClassName("org.greenrobot.eventbus.meta", "SubscriberInfo")
private val SUBSCRIBER_METHOD_INFO = ClassName("org.greenrobot.eventbus.meta", "SubscriberMethodInfo")
private val SIMPLE_SUBSCRIBER_INFO = ClassName("org.greenrobot.eventbus.meta", "SimpleSubscriberInfo")

private val CLASS_STAR = Class::class.asClassName().parameterizedBy(STAR)

fun KSName.asClassName() = ClassName(getQualifier(), getShortName())

class SubscriberTypeKey(
    val rawClassName: TypeName,
    val file: KSFile,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubscriberTypeKey

        return rawClassName == other.rawClassName
    }

    override fun hashCode(): Int {
        return rawClassName.hashCode()
    }
}

class AnnotationArgs(
    val threadMode: ClassName,
    val sticky: Boolean,
    val priority: Int,
)

class BuilderGenerator(
    private val codeGenerator: CodeGenerator,
    private val options: Map<String, String>,
    private val logger: KSPLogger,
) : SymbolProcessor {
    fun subscribeMethods(resolver: Resolver) = resolver.getSymbolsWithAnnotation(SUBSCRIBE).filterIsInstance<KSFunctionDeclaration>()

    fun args(method: KSFunctionDeclaration, annotation: KSAnnotation): AnnotationArgs? {
        val args = annotation.arguments.associateBy({ it.name!!.getShortName() }, { it.value })

        val threadMode = args["threadMode"]

        if (threadMode !is KSType) {
            logger.warn("@Subscribe.threadMode = $threadMode on $method is not an enum value")
            return null
        }

        val sticky = args["sticky"]

        if (sticky !is Boolean) {
            logger.warn("@Subscribe.sticky = $sticky on $method is not a Boolean value")
            return null
        }

        val priority = args["priority"]

        if (priority !is Int) {
            logger.warn("@Subscribe.priority = $priority on $method is not an Int value")
            return null
        }

        return AnnotationArgs(threadMode.toClassName(), sticky, priority)
    }

    fun KSAnnotated.subscriberAnnotation() = annotations.find {
        it.shortName.getShortName() == SUBSCRIBE_SHORT &&
            it.annotationType.resolve().declaration.qualifiedName?.asString() == SUBSCRIBE
    }

    fun describe(method: KSAnnotated): SubscribeMethodDescriptor? {
        if (method !is KSFunctionDeclaration) {
            logger.warn("$method is not a method")
            return null
        }

        val parent = method.parentDeclaration

        if (parent == null) {
            logger.warn("$method lacks a parent declaration")
            return null
        }

        val parentName = parent.qualifiedName

        if (parentName == null) {
            logger.warn("$method's parent does not have a name")
            return null
        }

        val parentFile = parent.containingFile

        if (parentFile == null) {
            logger.warn("$method's parent does not have a file")
            return null
        }

        val annotation = method.subscriberAnnotation()

        if (annotation == null) {
            logger.warn("$method's is missing the @Subscribe annotation, something must be very wrong")
            return null
        }

        if (method.parameters.size != 1) {
            logger.warn("$method's parameters must have exactly one parameter")
            return null
        }

        val eventType = method.parameters[0].type.toTypeName()

        val args = args(method, annotation) ?: return null

        return SubscribeMethodDescriptor(
            SubscriberTypeKey(parentName.asClassName(), parentFile),
            method.simpleName.getShortName(),
            eventType,
            args,
        )
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val subscribers = subscribeMethods(resolver).mapNotNull {
            describe(it)
        }.groupBy { it.type }

        if (subscribers.isNotEmpty()) {
            val name = ClassName.bestGuess(options.getValue(OPTION_EVENT_BUS_INDEX))

            FileSpec.builder(name).addType(
                TypeSpec.classBuilder(name)
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
                                                subscription.args.threadMode,
                                                subscription.args.priority,
                                                subscription.args.sticky,
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
            ).build().writeTo(codeGenerator, Dependencies(true, *subscribers.keys.map { it.file }.toTypedArray()))
        }

        return emptyList()
    }
}
