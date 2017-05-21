package com.udacity.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import java.text.MessageFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        StockAdapter.StockAdapterOnClickHandler {

    private static final int STOCK_LOADER = 0;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.recycler_view)
    RecyclerView stockRecyclerView;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.error)
    TextView error;
    private StockAdapter adapter;

    @Override
    public void onClick(String symbol) {
//        Adebowale says:
//        "Stock Hawk allows me to track the current price of stocks, but to track their prices over time, I need to use an external program. It would be wonderful if you could show more detail on a stock, including its price over time."
        // FIXME add support for details
        Timber.d("Symbol clicked: %s", symbol);
        Intent intent = new Intent(this, StockDetailActivity.class);
        intent.putExtra(StockDetailActivity.STOCK_KEY, symbol);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FIXME here we get 300 ms of delay, due to initial layout inflating
        // How to overcome this???
        Timber.d("One");
        setContentView(R.layout.activity_main);
        Timber.d("One and a half");
        ButterKnife.bind(this);

        Timber.d("two");
        adapter = new StockAdapter(this, this);
        stockRecyclerView.setAdapter(adapter);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        Timber.d("three");
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);

        Timber.d("four");
        onRefresh();

        Timber.d("five");

        try (DelayedWarning loadWarning = prepareLoadWarning()) {
            QuoteSyncJob.initialize(this, loadWarning);
        }
        Timber.d("six");

        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                final String symbol = adapter.getSymbolAtPosition(viewHolder.getAdapterPosition());
                final MainActivity context = MainActivity.this;
                PrefUtils.removeStock(context, symbol);
                int deleted = getContentResolver().delete(Contract.Quote.makeUriForStock(symbol), null, null);
                if (deleted > 0) {
                    broadcastUpdate();
                    Snackbar.make(stockRecyclerView, MessageFormat.format(getString(R.string.stockStymbolDeleted), symbol), Snackbar.LENGTH_LONG).setAction("UNDO", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            addStockAndSync(symbol);
                            Snackbar.make(stockRecyclerView, MessageFormat.format(getString(R.string.stockSymbolAddedAgain), symbol), Snackbar.LENGTH_LONG).show();
                        }
                    }).show();
                } else
                    Snackbar.make(stockRecyclerView, R.string.deletionFailed, Snackbar.LENGTH_LONG).show();
            }
        }).attachToRecyclerView(stockRecyclerView);
    }

    private void broadcastUpdate() {
        Intent updateDataIntent = new Intent(QuoteSyncJob.ACTION_DATA_UPDATED);
        sendBroadcast(updateDataIntent);
    }

    private DelayedWarning prepareLoadWarning() {
        final Snackbar progress = Snackbar.make(stockRecyclerView, R.string.warnNetworkDelays, Snackbar.LENGTH_LONG);
        return DelayedWarning.on(new Runnable() {
            @Override
            public void run() {
                progress.show();
            }
        }, new Runnable() {
            @Override
            public void run() {
                progress.dismiss();
            }
        });
    }

    private boolean networkUp() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    @Override
    public void onRefresh() {

        // FIXME add micro-message to compensate for unusual delays during network synchronization
//        Xaio-lu says:
//        "When I opened this app for the first time without a network connection, it was a confusing blank screen. I would love a message that tells me why the screen is blank or whether my stock quotes are out of date."

        Timber.d("Starting update");
        doSyncImmediately();
        Timber.d("Update completed");


        if (!networkUp() && adapter.getItemCount() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_network));
            error.setVisibility(View.VISIBLE);
        } else if (!networkUp()) {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, R.string.toast_no_connectivity, Toast.LENGTH_LONG).show();
        } else if (PrefUtils.getStocks(this).isEmpty()) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_stocks));
            error.setVisibility(View.VISIBLE);
        } else {
            error.setVisibility(View.GONE);
        }
    }

    private void doSyncImmediately() {
        try (DelayedWarning loadWarning = prepareLoadWarning()) {
            QuoteSyncJob.syncImmediately(this, loadWarning);
        }
    }

    public void button(@SuppressWarnings("UnusedParameters") View view) {
        new AddStockDialog().show(getFragmentManager(), "StockDialogFragment");
    }

    void addStock(String symbol) {
        if (symbol != null && !symbol.isEmpty()) {

            if (networkUp()) {
                swipeRefreshLayout.setRefreshing(true);
            } else {
                String message = getString(R.string.toast_stock_added_no_connectivity, symbol);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }

            addStockAndSync(symbol);
        }
    }

    private void addStockAndSync(String symbol) {
        PrefUtils.addStock(this, symbol);
        doSyncImmediately();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Contract.Quote.URI,
                Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                null, null, Contract.Quote.COLUMN_SYMBOL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swipeRefreshLayout.setRefreshing(false);

        if (data.getCount() != 0) {
            error.setVisibility(View.GONE);
        }
        adapter.setCursor(data);
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        adapter.setCursor(null);
    }


    private void setDisplayModeMenuItemIcon(MenuItem item) {
        if (PrefUtils.getDisplayMode(this)
                .equals(getString(R.string.pref_display_mode_absolute_key))) {
            item.setIcon(R.drawable.ic_percentage);
        } else {
            item.setIcon(R.drawable.ic_dollar);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_settings, menu);
        MenuItem item = menu.findItem(R.id.action_change_units);
        setDisplayModeMenuItemIcon(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            PrefUtils.toggleDisplayMode(this);
            setDisplayModeMenuItemIcon(item);
            adapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
