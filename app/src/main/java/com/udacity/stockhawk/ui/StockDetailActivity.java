package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import yahoofinance.histquotes.HistoricalQuote;

public class StockDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<HistoricalQuote>> {

    public static final String STOCK_KEY = "stock";
    private static final int DATA_LOADER = 42;
    @BindView(R.id.chart)
    CandleStickChart candleStickChart;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);

        ButterKnife.bind(this);
        Intent intent = getIntent();
        getSupportLoaderManager().initLoader(DATA_LOADER, null, this);
        if (intent != null && intent.hasExtra(STOCK_KEY)) {
            String stock = intent.getStringExtra(STOCK_KEY);

            LoaderManager loaderManager = getSupportLoaderManager();
            Loader<List<HistoricalQuote>> loader = loaderManager.getLoader(DATA_LOADER);

            Bundle bundle = new Bundle();
            bundle.putString("STOCK", stock);
            if (loader == null) {
                loaderManager.initLoader(DATA_LOADER, bundle, this);
            } else {
                loaderManager.restartLoader(DATA_LOADER, bundle, this);
            }
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        candleStickChart.setBackgroundColor(Color.WHITE);

        candleStickChart.setMaxVisibleValueCount(200);
        candleStickChart.setPinchZoom(true);
        XAxis xAxis = candleStickChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            private DateFormat dateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT);

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return dateFormat.format(new Date((long) value));
            }
        });
        xAxis.setLabelRotationAngle(45f);
        xAxis.setGranularityEnabled(true);
        xAxis.setGranularity(TimeUnit.DAYS.toMillis(14));
        xAxis.setTextSize(14);
        xAxis.setDrawGridLines(true);

        YAxis leftAxis = candleStickChart.getAxisLeft();
//        leftAxis.setEnabled(false);
        leftAxis.setLabelCount(7, false);
        leftAxis.setDrawGridLines(true);
        leftAxis.setDrawAxisLine(true);

        candleStickChart.setVisibleXRange(10, 15);
        candleStickChart.setAutoScaleMinMaxEnabled(true);

        YAxis rightAxis = candleStickChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    @Override
    public Loader<List<HistoricalQuote>> onCreateLoader(int id, final Bundle args) {
        return new AsyncTaskLoader<List<HistoricalQuote>>(this) {
            @Override
            protected void onStartLoading() {
                if (args == null) {
                    return;
                }
                forceLoad();
            }

            @Override
            public List<HistoricalQuote> loadInBackground() {
                String stock = args.getString("STOCK");
                Cursor cursor = getContentResolver().query(Contract.Quote.makeUriForStock(stock), null, null, null, null);
                try {
                    if (cursor != null && cursor.getCount() == 1) {
                        cursor.moveToFirst();
                        String historicalData = cursor.getString(Contract.Quote.POSITION_HISTORY);
                        List<HistoricalQuote> historicalQuotes = QuoteSyncJob.parseHistoricalData(historicalData);
                        return historicalQuotes;
                    }
                    return null;
                } finally {
                    if (cursor != null)
                        cursor.close();
                }
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<List<HistoricalQuote>> loader, List<HistoricalQuote> data) {
        List<HistoricalQuote> reducedData = FormattingHelper.reducePoints(data, 12);
        List<CandleEntry> yVals1 = new ArrayList<CandleEntry>(reducedData.size());

        for (HistoricalQuote historicalQuote : reducedData) {

            float high = historicalQuote.getHigh().floatValue();
            float low = historicalQuote.getLow().floatValue();
            float open = historicalQuote.getOpen().floatValue();
            float close = historicalQuote.getClose().floatValue();
            CandleEntry candleEntry = new CandleEntry(
                    (float) (historicalQuote.getDate().getTimeInMillis()), high,
                    low, open,
                    close,
                    null // getResources().getDrawable(android.R.drawable.star_big_on)
            );
            //Timber.v(low + " " + open + " - " + close + " - " + high);
            yVals1.add(candleEntry);
        }

        Collections.sort(yVals1, new Comparator<CandleEntry>() {
            @Override
            public int compare(CandleEntry o1, CandleEntry o2) {
                return Float.compare(o1.getX(), o2.getX());
            }
        });
        CandleDataSet set1 = new CandleDataSet(yVals1, "Data Set");
        set1.setDecreasingColor(Color.RED);
        set1.setDecreasingPaintStyle(Paint.Style.FILL);
        set1.setIncreasingColor(Color.GREEN);
        set1.setIncreasingPaintStyle(Paint.Style.FILL);
        set1.setBarSpace(12);
        set1.setShadowColorSameAsCandle(true);
        set1.setShadowWidth(12);
        set1.setShowCandleBar(true);
        set1.setDrawVerticalHighlightIndicator(true);
        set1.setNeutralColor(Color.GRAY);
        set1.setShadowColor(Color.YELLOW);
        set1.setValueTextSize(16);
        set1.setDrawValues(true);
        set1.setDrawHorizontalHighlightIndicator(true);
        set1.setHighlightLineWidth(1);

        candleStickChart.setData(new CandleData(set1));
        candleStickChart.invalidate();
    }

    @Override
    public void onLoaderReset(Loader<List<HistoricalQuote>> loader) {

    }
}
