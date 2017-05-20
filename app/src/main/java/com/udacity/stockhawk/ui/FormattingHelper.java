package com.udacity.stockhawk.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.robinhood.spark.SparkAdapter;
import com.robinhood.spark.SparkView;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import yahoofinance.histquotes.HistoricalQuote;

/**
 * Created by gallucci on 20/05/2017.
 */

public class FormattingHelper {
    private final DecimalFormat dollarFormatWithPlus;
    private final DecimalFormat dollarFormat;
    private final DecimalFormat percentageFormat;
    private final DisplayModeSupplier displayModeSupplier;

    public FormattingHelper(DisplayModeSupplier displayModeSupplier) {
        this.displayModeSupplier = displayModeSupplier;
        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix("+$");
        percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
        percentageFormat.setPositivePrefix("+");
    }

    public FormattingHelper(final Context context) {
        this(new DisplayModeSupplier() {
            @Override
            public boolean isDisplayModeAbsolute() {
                return PrefUtils.getDisplayMode(context)
                        .equals(context.getString(R.string.pref_display_mode_absolute_key));
            }
        });
    }

    @NonNull
    public static List<HistoricalQuote> reducePoints(@NonNull List<HistoricalQuote> historicalQuotes, int maxSize) {
        List<HistoricalQuote> reducedSize = new ArrayList<>(maxSize);
        for (int i = 0; i < maxSize; i++) {
            float mul = historicalQuotes.size() - 1;
            int div = maxSize - 1;
            reducedSize.add(historicalQuotes.get((int) ((i * mul) / div)));
        }
        return reducedSize;
    }

    public void setLine(final Cursor cursor, final TextView tvSymbol, final TextView tvPrice, final TextView tvChange, final SparkView sparkView) {
        new PrettyPrinter() {
            @Override
            void display(String symbol, String price, int backgroundResource, String change, List<HistoricalQuote> historicalQuotes) {
                tvSymbol.setText(symbol);
                tvPrice.setText(price);
                tvChange.setBackgroundResource(backgroundResource);
                tvChange.setText(change);

                sparkView.setAdapter(new MyAdapter(reducePoints(historicalQuotes), getPrice(cursor)));
            }
        }.go(cursor);
    }

    private List<HistoricalQuote> reducePoints(List<HistoricalQuote> historicalQuotes) {
        return reducePoints(historicalQuotes, 30);
    }

    public void setLine(final Cursor cursor, final RemoteViews views, @IdRes final int symbolId, @IdRes final int priceId, @IdRes final int changeId) {
        new PrettyPrinter() {
            @Override
            void display(String symbol, String price, int backgroundResource, String change, List<HistoricalQuote> historicalQuotes) {
                views.setTextViewText(symbolId, symbol);
                views.setTextViewText(priceId, price);
                views.setInt(changeId, "setBackgroundResource", backgroundResource);
                views.setTextViewText(changeId, change);
            }
        }.go(cursor);
    }

    public interface DisplayModeSupplier {
        boolean isDisplayModeAbsolute();
    }

    public class MyAdapter extends SparkAdapter {
        private final List<HistoricalQuote> historicalQuotes;
        private final float baseLine;

        public MyAdapter(List<HistoricalQuote> historicalQuotes, float baseLine) {
            this.historicalQuotes = historicalQuotes;
            this.baseLine = baseLine;
        }

        @Override
        public boolean hasBaseLine() {
            return true;
        }

        @Override
        public float getBaseLine() {
            return baseLine;
        }

        @Override
        public int getCount() {
            return historicalQuotes.size();
        }

        @Override
        public Object getItem(int index) {
            return getY(index);
        }

        @Override
        public float getY(int index) {
            return historicalQuotes.get(index).getClose().floatValue();
        }

        @Override
        public float getX(int index) {
            return (float) (historicalQuotes.get(index).getDate().getTimeInMillis());
        }
    }

    private abstract class PrettyPrinter {
        /**
         * Will code for a tuple, but any product class is appreciated.
         * Sir, would you mind sparing a tuple for a poor Scala traveller stranded in this pre-lambda world?
         */
        abstract void display(String symbol, String price, int backgroundResource, String change, List<HistoricalQuote> historicalQuotes);

        void go(Cursor cursor) {
            String symbol = cursor.getString(Contract.Quote.POSITION_SYMBOL);
            String price = dollarFormat.format(getPrice(cursor));

            float rawAbsoluteChange = cursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
            float percentageChange = cursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

            int backgroundResource = rawAbsoluteChange > 0 ? R.drawable.percent_change_pill_green : R.drawable.percent_change_pill_red;

            String change = dollarFormatWithPlus.format(rawAbsoluteChange);
            String percentage = percentageFormat.format(percentageChange / 100);
            String changeText = displayModeSupplier.isDisplayModeAbsolute() ? change : percentage;
            String historicalData = cursor.getString(Contract.Quote.POSITION_HISTORY);
            List<HistoricalQuote> historicalQuotes = QuoteSyncJob.parseHistoricalData(historicalData);

            display(symbol, price, backgroundResource, changeText, historicalQuotes);
        }

        float getPrice(Cursor cursor) {
            return cursor.getFloat(Contract.Quote.POSITION_PRICE);
        }
    }
}
