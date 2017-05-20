package com.udacity.stockhawk.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.IdRes;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

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

    public void setLine(Cursor cursor, final TextView tvSymbol, final TextView tvPrice, final TextView tvChange) {
        new PrettyPrinter() {
            @Override
            void display(String symbol, String price, int backgroundResource, String change) {
                tvSymbol.setText(symbol);
                tvPrice.setText(price);
                tvChange.setBackgroundResource(backgroundResource);
                tvChange.setText(change);
            }
        }.go(cursor);
    }

    public void setLine(final Cursor cursor, final RemoteViews views, @IdRes final int symbolId, @IdRes final int priceId, @IdRes final int changeId) {
        new PrettyPrinter() {
            @Override
            void display(String symbol, String price, int backgroundResource, String change) {
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

    private abstract class PrettyPrinter {
        /**
         * Will code for a tuple, but any product class is appreciated.
         * Sir, would you mind sparing a tuple for a poor Scala traveller stranded in this pre-lambda world?
         */
        abstract void display(String symbol, String price, int backgroundResource, String change);

        void go(Cursor cursor) {
            String symbol = cursor.getString(Contract.Quote.POSITION_SYMBOL);
            String price = dollarFormat.format(cursor.getFloat(Contract.Quote.POSITION_PRICE));

            float rawAbsoluteChange = cursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
            float percentageChange = cursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

            int backgroundResource = rawAbsoluteChange > 0 ? R.drawable.percent_change_pill_green : R.drawable.percent_change_pill_red;

            String change = dollarFormatWithPlus.format(rawAbsoluteChange);
            String percentage = percentageFormat.format(percentageChange / 100);
            String changeText = displayModeSupplier.isDisplayModeAbsolute() ? change : percentage;

            display(symbol, price, backgroundResource, changeText);
        }
    }
}
