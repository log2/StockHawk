package com.udacity.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.ui.DelayedWarning;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.Utils;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.quotes.stock.StockQuote;

public final class QuoteSyncJob {

    @SuppressWarnings("HardCodedStringLiteral")
    public static final String ACTION_DATA_UPDATED = "com.udacity.stockhawk.ACTION_DATA_UPDATED";
    private static final int ONE_OFF_ID = 2;
    private static final int PERIOD = 300000;
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    private static final int YEARS_OF_HISTORY = 2;

    private QuoteSyncJob() {
    }

    static void getQuotes(final Context context) {

        Timber.d("Running sync job");

        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.YEAR, -YEARS_OF_HISTORY);

        try {

            Set<String> stockPref = PrefUtils.getStocks(context);
            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            String[] stockArray = stockPref.toArray(new String[stockPref.size()]);

            Timber.d(stockCopy.toString());

            if (stockArray.length == 0) {
                return;
            }


            // FIXED missing network does not induce strange states, apparently
//            Xaio-lu says:
//            "When I opened this app for the first time without a network connection, it was a confusing blank screen. I would love a message that tells me why the screen is blank or whether my stock quotes are out of date."
            Map<String, Stock> quotes = YahooFinance.get(stockArray);
            Iterator<String> iterator = stockCopy.iterator();

            Timber.d(quotes.toString());

            ArrayList<ContentValues> quoteCVs = new ArrayList<>();

            List<String> badStockSymbols = new ArrayList<>();

            while (iterator.hasNext()) {
                final String symbol = iterator.next();


                Stock stock = quotes.get(symbol);


                // FIXED, bad stock symbol are removed as soon as possible add a fix for this
//                Jamal says:
//                "I found a bug in your app. Right now when I search for a stock quote that doesn't exist, the app crashes."
                StockQuote quote = stock.getQuote();
                if (quote.getPrice() == null) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // FIXME can we switch to snackbar with action? But where can we find a view?
                            Toast.makeText(context, MessageFormat.format(context.getString(R.string.unknownStockSymbol), symbol), Toast.LENGTH_LONG).show();
                        }
                    });
                    badStockSymbols.add(symbol);
                    continue;
                }
                float price = quote.getPrice().floatValue();
                float change = quote.getChange().floatValue();
                float percentChange = quote.getChangeInPercent().floatValue();

                // WARNING! Don't request historical data for a stock that doesn't exist!
                // The request will hang forever X_x
                //List<HistoricalQuote> history = stock.getHistory(from, to, Interval.WEEKLY);

                // Note for reviewer:
                // Due to the problems with Yahoo API I have commented the line above
                // and included this one to fetch the history from MockUtils
                // This should be enough to develop and review while the API is down
                // Ref. "Yahoo Finance API Fix" (https://docs.google.com/document/d/1nGSXWBcpvvvBlTZSVY5U3_di8jB051EtNgHy7sgxKwA/edit)
                List<HistoricalQuote> history = MockUtils.getHistory(symbol, price);

                String historySerialized = serializeHistory(history);
                ContentValues quoteCV = new ContentValues();
                quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);


                quoteCV.put(Contract.Quote.COLUMN_HISTORY, historySerialized);

                quoteCVs.add(quoteCV);

            }

            for (String badStockSymbol : badStockSymbols) {
                PrefUtils.removeStock(context, badStockSymbol);
            }

            context.getContentResolver()
                    .bulkInsert(
                            Contract.Quote.URI,
                            quoteCVs.toArray(new ContentValues[quoteCVs.size()]));

            Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
            context.sendBroadcast(dataUpdatedIntent);

        } catch (IOException exception) {
            Timber.e(exception, "Error fetching stock quotes");
        }
    }

    @NonNull
    private static String serializeHistory(List<HistoricalQuote> history) {
        StringBuilder historyBuilder = new StringBuilder();

        for (HistoricalQuote it : history) {
            historyBuilder.append(it.getDate().getTimeInMillis());
            historyBuilder.append(",");
            historyBuilder.append(it.getOpen());
            historyBuilder.append(",");
            historyBuilder.append(it.getHigh());
            historyBuilder.append(",");
            historyBuilder.append(it.getLow());
            historyBuilder.append(",");
            historyBuilder.append(it.getClose());
            historyBuilder.append(",");
            historyBuilder.append(it.getAdjClose());
            historyBuilder.append(",");
            historyBuilder.append(it.getVolume());
            historyBuilder.append("\n");
        }
        return historyBuilder.toString();
    }

    private static void schedulePeriodic(Context context) {
        Timber.d("Scheduling a periodic task");


        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, QuoteJobService.class));


        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        scheduler.schedule(builder.build());
    }

    public static synchronized void initialize(final Context context, DelayedWarning loadWarning) {
        schedulePeriodic(context);
        syncImmediately(context);
    }

    public static synchronized void syncImmediately(Context context) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);
        } else {

            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));


            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            scheduler.schedule(builder.build());
        }
    }

    private static HistoricalQuote parseLine(String line) {
        String[] data = line.split(",");
        return new HistoricalQuote("",
                parseDate(Utils.getLong(data[0])),
                Utils.getBigDecimal(data[1]),
                Utils.getBigDecimal(data[3]),
                Utils.getBigDecimal(data[2]),
                Utils.getBigDecimal(data[4]),
                Utils.getBigDecimal(data[5]),
                Utils.getLong(data[6])
        );
    }

    private static Calendar parseDate(long aLong) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(aLong);
        return c;
    }

    public static List<HistoricalQuote> parseHistoricalData(String historicalData) {
        try {
            List<HistoricalQuote> history = new ArrayList<>();

            BufferedReader br = new BufferedReader(new StringReader(historicalData));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                HistoricalQuote historicalQuote = parseLine(line);
                history.add(historicalQuote);
            }

            return history;
        } catch (IOException e) {
            throw new RuntimeException("Could not possibly happen", e);
        }
    }
}
