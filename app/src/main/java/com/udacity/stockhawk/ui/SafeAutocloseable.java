package com.udacity.stockhawk.ui;

/**
 * Created by gallucci on 20/05/2017.
 */

interface SafeAutocloseable extends AutoCloseable {
    /**
     * We won't throw anything, promise!
     */
    void close();
}
