package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import yahoofinance.histquotes.HistoricalQuote;

public class StockDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<HistoricalQuote>> {

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
        if (intent != null && intent.hasExtra(Intent.EXTRA_KEY_EVENT)) {
            String stock = intent.getStringExtra(Intent.EXTRA_KEY_EVENT);

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
                Cursor cursor = getContentResolver().query(Contract.Quote.URI, null, Contract.Quote.POSITION_SYMBOL + " = ?", new String[]{stock}, null);
                try {
                    if (cursor != null && cursor.getCount() == 1) {
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
        List<CandleEntry> yVals1 = new ArrayList<CandleEntry>(data.size());

        for (HistoricalQuote historicalQuote : data) {

            yVals1.add(new CandleEntry(
                    (float) (historicalQuote.getDate().getTimeInMillis()), historicalQuote.getHigh().floatValue(),
                    historicalQuote.getLow().floatValue(), historicalQuote.getOpen().floatValue(),
                    historicalQuote.getClose().floatValue(),
                    getResources().getDrawable(android.R.drawable.star_on)
            ));
        }

        CandleDataSet set1 = new CandleDataSet(yVals1, "Data Set");
        candleStickChart.setData(new CandleData(set1));
    }

    @Override
    public void onLoaderReset(Loader<List<HistoricalQuote>> loader) {

    }
}
