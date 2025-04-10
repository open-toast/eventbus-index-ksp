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

package com.toasttab.eventbus.ksp.test

import org.greenrobot.eventbus.SubscriberMethodDescriptor
import org.greenrobot.eventbus.ThreadMode
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isNotNull
import strikt.assertions.isSameInstanceAs
import strikt.assertions.isTrue

class TestIndexIntegrationTest {
    @Test
    fun `correct index generated for java subscribers`() {
        expectThat(TestIndex.getSubscriberInfo(JavaService::class.java)).isNotNull().and {
            get { shouldCheckSuperclass() }.isTrue()
            get { subscriberClass }.isSameInstanceAs(JavaService::class.java)
            get { subscriberMethods.map(::SubscriberMethodDescriptor) }.containsExactly(
                SubscriberMethodDescriptor(
                    "onEvent",
                    ThreadMode.ASYNC,
                    EventA::class.java,
                    false,
                    0,
                ),
            )
        }
    }

    @Test
    fun `correct index generated for kotlin subscribers`() {
        expectThat(TestIndex.getSubscriberInfo(KotlinService::class.java)).isNotNull().and {
            get { shouldCheckSuperclass() }.isTrue()
            get { subscriberClass }.isSameInstanceAs(KotlinService::class.java)
            get { subscriberMethods.map(::SubscriberMethodDescriptor) }.containsExactly(
                SubscriberMethodDescriptor(
                    "onA",
                    ThreadMode.POSTING,
                    EventA::class.java,
                    false,
                    0,
                ),
                SubscriberMethodDescriptor(
                    "onB",
                    ThreadMode.MAIN,
                    EventB::class.java,
                    true,
                    2,
                ),
                SubscriberMethodDescriptor(
                    "onC",
                    ThreadMode.ASYNC,
                    EventC::class.java,
                    false,
                    0,
                ),
            )
        }
    }
}
