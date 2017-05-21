package com.udacity.stockhawk.data;


import android.net.Uri;
import android.provider.BaseColumns;

import com.google.common.collect.ImmutableList;

public final class Contract {

    @SuppressWarnings({"HardCodedStringLiteral", "DuplicateStringLiteralInspection"})
    static final String AUTHORITY = "com.udacity.stockhawk";
    @SuppressWarnings("HardCodedStringLiteral")
    static final String PATH_QUOTE = "quote";
    @SuppressWarnings("HardCodedStringLiteral")
    static final String PATH_QUOTE_WITH_SYMBOL = "quote/*";
    @SuppressWarnings({"HardCodedStringLiteral", "StringConcatenation"})
    private static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

    private Contract() {
        throw new AssertionError("No Contract instances for you!");
    }

    @SuppressWarnings("unused")
    public static final class Quote implements BaseColumns {

        public static final Uri URI = BASE_URI.buildUpon().appendPath(PATH_QUOTE).build();
        @SuppressWarnings("HardCodedStringLiteral")
        public static final String COLUMN_SYMBOL = "symbol";
        @SuppressWarnings("HardCodedStringLiteral")
        public static final String COLUMN_PRICE = "price";
        @SuppressWarnings("HardCodedStringLiteral")
        public static final String COLUMN_ABSOLUTE_CHANGE = "absolute_change";
        @SuppressWarnings("HardCodedStringLiteral")
        public static final String COLUMN_PERCENTAGE_CHANGE = "percentage_change";
        @SuppressWarnings("HardCodedStringLiteral")
        public static final String COLUMN_HISTORY = "history";
        public static final int POSITION_ID = 0;
        public static final int POSITION_SYMBOL = 1;
        public static final int POSITION_PRICE = 2;
        public static final int POSITION_ABSOLUTE_CHANGE = 3;
        public static final int POSITION_PERCENTAGE_CHANGE = 4;
        public static final int POSITION_HISTORY = 5;
        public static final ImmutableList<String> QUOTE_COLUMNS = ImmutableList.of(
                _ID,
                COLUMN_SYMBOL,
                COLUMN_PRICE,
                COLUMN_ABSOLUTE_CHANGE,
                COLUMN_PERCENTAGE_CHANGE,
                COLUMN_HISTORY
        );
        @SuppressWarnings("HardCodedStringLiteral")
        static final String TABLE_NAME = "quotes";

        public static Uri makeUriForStock(String symbol) {
            return URI.buildUpon().appendPath(symbol).build();
        }

        static String getStockFromUri(Uri queryUri) {
            return queryUri.getLastPathSegment();
        }


    }

}
