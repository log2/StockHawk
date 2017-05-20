package com.udacity.stockhawk.sync;

import android.os.Binder;

import com.udacity.stockhawk.ui.SafeAutocloseable;

/**
 * Created by gallucci on 20/05/2017.
 */

public abstract class AppIdentity implements SafeAutocloseable {
    public static AppIdentity with() {
        final long identityToken = Binder.clearCallingIdentity();
        return new AppIdentity() {
            @Override
            public void close() {
                Binder.restoreCallingIdentity(identityToken);
            }
        };
    }
}
