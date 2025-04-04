package com.toasttab.eventbus.ksp.test;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class JavaService {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(EventA e) { }

    @Subscribe
    void onEvent(EventB e) { }
}
