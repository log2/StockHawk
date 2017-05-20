package com.udacity.stockhawk.ui;

import android.content.Context;
import android.database.Cursor;
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

    public void setLine(Cursor cursor, TextView tvSymbol, TextView tvPrice, TextView tvChange) {

        tvSymbol.setText(cursor.getString(Contract.Quote.POSITION_SYMBOL));
        tvPrice.setText(dollarFormat.format(cursor.getFloat(Contract.Quote.POSITION_PRICE)));

        float rawAbsoluteChange = cursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
        float percentageChange = cursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

        if (rawAbsoluteChange > 0) {
            tvChange.setBackgroundResource(R.drawable.percent_change_pill_green);
        } else {
            tvChange.setBackgroundResource(R.drawable.percent_change_pill_red);
        }

        String change = dollarFormatWithPlus.format(rawAbsoluteChange);
        String percentage = percentageFormat.format(percentageChange / 100);

        if (displayModeSupplier.isDisplayModeAbsolute()) {
            tvChange.setText(change);
        } else {
            tvChange.setText(percentage);
        }
    }

    public void setLine(Cursor cursor, RemoteViews views, int symbolId, int priceId, int changeId) {
        views.setTextViewText(symbolId, cursor.getString(Contract.Quote.POSITION_SYMBOL));
        views.setTextViewText(priceId, dollarFormat.format(cursor.getFloat(Contract.Quote.POSITION_PRICE)));

        float rawAbsoluteChange = cursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
        float percentageChange = cursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

        // FIXME enable background change
        views.setInt(changeId, "setBackgroundResource", rawAbsoluteChange > 0 ? R.drawable.percent_change_pill_green : R.drawable.percent_change_pill_red);

        String change = dollarFormatWithPlus.format(rawAbsoluteChange);
        String percentage = percentageFormat.format(percentageChange / 100);

        if (displayModeSupplier.isDisplayModeAbsolute()) {
            views.setTextViewText(changeId, change);
        } else {
            views.setTextViewText(changeId, percentage);
        }
    }

    public interface DisplayModeSupplier {
        boolean isDisplayModeAbsolute();
    }
}
