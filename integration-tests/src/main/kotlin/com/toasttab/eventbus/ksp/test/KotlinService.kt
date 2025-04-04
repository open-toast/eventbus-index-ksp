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

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@Suppress("UNUSED_PARAMETER")
class KotlinService {
    @Subscribe
    fun onA(event: EventA) {
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true, priority = 2)
    fun onB(event: EventB) {
    }

    @Subscribe
    fun tooManyArgs(event: EventA, garbage: Int) {
    }
}
