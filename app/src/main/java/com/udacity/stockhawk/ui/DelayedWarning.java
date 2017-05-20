package com.udacity.stockhawk.ui;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.View;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by gallucci on 04/02/2017.
 * Copied from my own project at https://raw.githubusercontent.com/log2/PopMoviesLog/master/app/src/main/java/com/example/log2/popmovies/helpers/DelayedWarning.java
 */
public class DelayedWarning implements SafeAutocloseable {
    private final AtomicBoolean hidden = new AtomicBoolean(false);
    private final Runnable delayHideAction;

    private DelayedWarning(final Runnable delayShowAction, Runnable delayHideAction) {
        this.delayHideAction = delayHideAction;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!hidden.get()) delayShowAction.run();
            }
        }, 100);
    }

    @NonNull
    public static DelayedWarning showingTemporarily(final View view) {
        return on(new Runnable() {
            @Override
            public void run() {
                view.setVisibility(View.VISIBLE);
            }
        }, new Runnable() {
            @Override
            public void run() {
                view.setVisibility(View.INVISIBLE);
            }
        });
    }

    public static DelayedWarning on(final Runnable delayShowAction, Runnable delayHideAction) {
        return new DelayedWarning(delayShowAction, delayHideAction);
    }

    @Override
    public void close() {
        delayHideAction.run();
        hidden.set(true);
    }
}