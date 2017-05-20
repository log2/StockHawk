package com.udacity.stockhawk.ui;

/**
 * Created by gallucci on 20/05/2017.
 */
public interface SafeAutocloseable extends AutoCloseable {
    /**
     * We won't throw anything, promise!
     */
    void close();
}
