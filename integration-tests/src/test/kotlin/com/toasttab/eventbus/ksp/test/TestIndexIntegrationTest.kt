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

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isNotNull
import strikt.assertions.isSameInstanceAs
import strikt.assertions.isTrue

class TestIndexIntegrationTest {
    private val index = TestIndex()

    @Test
    fun `index generated for java subscribers`() {
        expectThat(index.getSubscriberInfo(JavaService::class.java)).isNotNull().and {
            get { shouldCheckSuperclass() }.isTrue()
            get { subscriberClass }.isSameInstanceAs(JavaService::class.java)
            get { subscriberMethods.toList() }.hasSize(2)
        }
    }

    @Test
    fun `index generated for kotlin subscribers`() {
        expectThat(index.getSubscriberInfo(Service::class.java)).isNotNull().and {
            get { shouldCheckSuperclass() }.isTrue()
            get { subscriberClass }.isSameInstanceAs(Service::class.java)
        }
    }
}
