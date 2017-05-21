package com.udacity.stockhawk.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.sync.AppIdentity;
import com.udacity.stockhawk.ui.FormattingHelper;
import com.udacity.stockhawk.ui.StockDetailActivity;

import timber.log.Timber;


/**
 * Created by gallucci on 20/05/2017.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class QuoteWidgetRemoteViewsService extends RemoteViewsService {
    public QuoteWidgetRemoteViewsService() {
        Timber.d("QWRS instantiated");
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Timber.d("onGetViewFactory called");
        final FormattingHelper formattingHelper = new FormattingHelper(new FormattingHelper.DisplayModeSupplier() {
            @Override
            public boolean isDisplayModeAbsolute() {
                return false;
            }
        });
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Do nothing here
            }

            @Override
            public void onDataSetChanged() {
                closeCursorIfAny();
                try (AppIdentity appIdentity = AppIdentity.with()) {
                    data = getContentResolver().query(Contract.Quote.URI, new String[]{}, null, null, Contract.Quote.COLUMN_SYMBOL);
                }
            }

            private void closeCursorIfAny() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public void onDestroy() {
                closeCursorIfAny();
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_collection_item);
                formattingHelper.setLine(getApplicationContext(), data, views, R.id.symbol, R.id.price, R.id.change);

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(StockDetailActivity.STOCK_KEY, data.getString(Contract.Quote.POSITION_SYMBOL));
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_collection_item);
            }

            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            private void setRemoteContentDescription(RemoteViews views, String description) {
                views.setContentDescription(R.id.symbol, description);
            }

            @Override
            public int getViewTypeCount() {
                return 1; // Just one type of list item
            }

            @Override
            public long getItemId(int position) {
                if (data != null && data.moveToPosition(position))
                    return data.getLong(Contract.Quote.POSITION_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                // Our ids don't change
                return true;
            }
        };
    }
}
