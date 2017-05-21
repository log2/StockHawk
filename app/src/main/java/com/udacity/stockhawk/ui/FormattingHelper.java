package com.udacity.stockhawk.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.github.mikephil.charting.data.CandleEntry;
import com.robinhood.spark.SparkAdapter;
import com.robinhood.spark.SparkView;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    private final DecimalFormat percentageFormatWithPlus;
    private final DisplayModeSupplier displayModeSupplier;

    private final DateFormat dateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT);

    public FormattingHelper(DisplayModeSupplier displayModeSupplier) {
        this.displayModeSupplier = displayModeSupplier;
        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix("+$");
        percentageFormatWithPlus = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormatWithPlus.setMaximumFractionDigits(2);
        percentageFormatWithPlus.setMinimumFractionDigits(2);
        percentageFormatWithPlus.setPositivePrefix("+");
        percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
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
    public static <T> List<T> reducePoints(@NonNull List<T> originalSeries, int maxSize) {
        if (maxSize >= originalSeries.size())
            return originalSeries;
        List<T> reducedSize = new ArrayList<>(maxSize);
        float mul = originalSeries.size() - 1;
        int div = maxSize - 1;
        for (int i = 0; i < maxSize; i++) {
            reducedSize.add(originalSeries.get((int) ((i * mul) / div)));
        }
        return reducedSize;
    }

    public void setLine(Context context, final Cursor cursor, final View itemView, final TextView tvSymbol, final TextView tvPrice, final TextView tvChange, final SparkView sparkView) {
        new PrettyPrinter(context) {
            @Override
            void display(String contentDescription, String symbol, String price, int backgroundResource, String change, List<HistoricalQuote> historicalQuotes) {
                tvSymbol.setText(symbol);
                tvPrice.setText(price);
                tvChange.setBackgroundResource(backgroundResource);
                tvChange.setText(change);
                itemView.setContentDescription(contentDescription);
                sparkView.setAdapter(new MyAdapter(reducePoints(historicalQuotes), getPrice(cursor)));
            }
        }.go(cursor);
    }

    private List<HistoricalQuote> reducePoints(List<HistoricalQuote> historicalQuotes) {
        return reducePoints(historicalQuotes, 30);
    }

    public void setLine(Context context, final Cursor cursor, final RemoteViews views, @SuppressWarnings("SameParameterValue") @IdRes final int symbolId, @SuppressWarnings("SameParameterValue") @IdRes final int priceId, @SuppressWarnings("SameParameterValue") @IdRes final int changeId) {
        new PrettyPrinter(context) {
            @Override
            void display(String contentDescription, String symbol, String price, int backgroundResource, String change, List<HistoricalQuote> historicalQuotes) {
                views.setTextViewText(symbolId, symbol);
                views.setTextViewText(priceId, price);
                //noinspection HardCodedStringLiteral
                views.setInt(changeId, "setBackgroundResource", backgroundResource);
                views.setTextViewText(changeId, change);
            }
        }.go(cursor);
    }

    public String format(Context context, String symbol, CandleEntry candleEntry, TextView detailDate, TextView detailLow, TextView detailHigh, TextView detailOpen, TextView detailClose, TextView detailAbsoluteChange, TextView detailPercentageChange) {
        float rawAbsoluteChange = candleEntry.getClose() - candleEntry.getOpen();
        float percentageChange = 100 * (rawAbsoluteChange / candleEntry.getOpen());

        int backgroundResource = rawAbsoluteChange > 0 ? R.drawable.percent_change_pill_green : R.drawable.percent_change_pill_red;

        String change = dollarFormatWithPlus.format(rawAbsoluteChange);
        String percentage = percentageFormatWithPlus.format(percentageChange / 100);
        String date = dateFormat.format(new Date((long) candleEntry.getX()));
        detailDate.setText(date);
        detailAbsoluteChange.setText(change);
        detailPercentageChange.setText(percentage);
        detailAbsoluteChange.setBackgroundResource(backgroundResource);
        detailPercentageChange.setBackgroundResource(backgroundResource);
        String low = dollarFormat.format(candleEntry.getLow());
        detailLow.setText(low);
        String high = dollarFormat.format(candleEntry.getHigh());
        detailHigh.setText(high);
        String open = dollarFormat.format(candleEntry.getOpen());
        detailOpen.setText(open);
        String close = dollarFormat.format(candleEntry.getClose());
        detailClose.setText(close);

        return String.format(context.getString(R.string.detailedStockContentDescription), date, symbol, low, high, open, close, describeChange(context, rawAbsoluteChange, percentageChange));
    }

    @NonNull
    private String describeChange(Context context, float rawAbsoluteChange, float percentageChange) {
        if (rawAbsoluteChange >= 0) {
            return String.format(context.getString(R.string.stockSymbolSoaredDescription), displayModeSupplier.isDisplayModeAbsolute() ? dollarFormat.format(rawAbsoluteChange) : percentageFormat.format(percentageChange / 100));
        } else {
            return String.format(context.getString(R.string.stockSymbolDecreasedDescription), displayModeSupplier.isDisplayModeAbsolute() ? dollarFormat.format(-rawAbsoluteChange) : percentageFormat.format(-percentageChange / 100));
        }
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
        private final Context context;

        PrettyPrinter(Context context) {
            this.context = context;
        }

        /**
         * Will code for a tuple, but any product class is appreciated.
         * Sir, would you mind sparing a tuple for a poor Scala traveller stranded in this pre-lambda world?
         */
        abstract void display(String contentDescription, String symbol, String price, int backgroundResource, String change, List<HistoricalQuote> historicalQuotes);

        void go(Cursor cursor) {
            String symbol = cursor.getString(Contract.Quote.POSITION_SYMBOL);
            String price = dollarFormat.format(getPrice(cursor));

            float rawAbsoluteChange = cursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
            float percentageChange = cursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

            int backgroundResource = rawAbsoluteChange > 0 ? R.drawable.percent_change_pill_green : R.drawable.percent_change_pill_red;

            String change = dollarFormatWithPlus.format(rawAbsoluteChange);
            String percentage = percentageFormatWithPlus.format(percentageChange / 100);
            String changeText = displayModeSupplier.isDisplayModeAbsolute() ? change : percentage;
            String historicalData = cursor.getString(Contract.Quote.POSITION_HISTORY);
            List<HistoricalQuote> historicalQuotes = QuoteSyncJob.parseHistoricalData(historicalData);

            String contentDescription = String.format(context.getString(R.string.stockContentDescription), symbol, price, describeChange(context, rawAbsoluteChange, percentageChange));
            display(contentDescription, symbol, price, backgroundResource, changeText, historicalQuotes);
        }


        float getPrice(Cursor cursor) {
            return cursor.getFloat(Contract.Quote.POSITION_PRICE);
        }
    }
}
