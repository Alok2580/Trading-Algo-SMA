package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class DataFetcher {

    private static final String API_KEY = "6U4BYVEKFSHY012D";
    private static final String BASE_URL = "https://www.alphavantage.co/query";


    public List<StockData> fetchStockData(String symbol) {
        List<StockData> stockDataList = new ArrayList<>();

        try {

            String endpoint = BASE_URL + "?function=TIME_SERIES_DAILY&symbol=" + symbol + "&apikey=" + API_KEY;
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parsing the JSON response
            JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
            JsonObject timeSeries = jsonObject.getAsJsonObject("Time Series (Daily)");

            if (timeSeries != null) {
                for (String date : timeSeries.keySet()) {
                    JsonObject dailyData = timeSeries.getAsJsonObject(date);
                    double open = dailyData.get("1. open").getAsDouble();
                    double high = dailyData.get("2. high").getAsDouble();
                    double low = dailyData.get("3. low").getAsDouble();
                    double close = dailyData.get("4. close").getAsDouble();


                    stockDataList.add(new StockData(date, open, high, low, close));
                }
            } else {
                System.out.println("No data found for the symbol: " + symbol);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stockDataList;
    }

    public List<Double> calculateSMA(List<StockData> stockDataList, int period) {
        List<Double> smaValues = new ArrayList<>();

//       calculating SMA for each point where there are enough days
        for (int i = 0; i <= stockDataList.size() - period; i++) {
            double sum = 0.0;

            // Sum up the closing prices for the given period
            for (int j = 0; j < period; j++) {
                sum += stockDataList.get(i + j).getClose();
            }

            double sma = sum / period;
            smaValues.add(sma);
        }

        return smaValues;
    }

    public List<String> generateSignals(List<StockData> stockDataList, int period) {
        List<Double> smaValues = calculateSMA(stockDataList, period);
        List<String> signals = new ArrayList<>();

        // Track if we are currently holding a stock
        boolean isHolding = false;

        // Ensure that we start from the period index
        for (int i = period - 1; i < stockDataList.size(); i++) {
            double currentPrice = stockDataList.get(i).getClose();
            double previousPrice = stockDataList.get(i - 1).getClose();

            double currentSMA = smaValues.get(i - period + 1);

            // Generate buy/sell signals based on crossing points
            if (previousPrice < currentSMA && currentPrice > currentSMA && !isHolding) {
                signals.add("Buy on day " + (i + 1) + " at price " + currentPrice);
                isHolding = true;
            } else if (previousPrice > currentSMA && currentPrice < currentSMA && isHolding) {
                signals.add("Sell on day " + (i + 1) + " at price " + currentPrice);
                isHolding = false;
            } else {
                signals.add("Hold on day " + (i + 1));
            }
        }

        return signals;
    }

    public static void main(String[] args) {
        DataFetcher fetcher = new DataFetcher();


        List<StockData> stockDataList = fetcher.fetchStockData("NVDA");


        List<Double> smaValues = fetcher.calculateSMA(stockDataList, 10);


//        System.out.println("20-day SMA values:");
//        for (int i = 0; i < smaValues.size(); i++) {
//            System.out.println("SMA on day " + (i + 1) + ": " + smaValues.get(i));
//        }


        List<String> signals = fetcher.generateSignals(stockDataList, 10);


        System.out.println("Buy/Sell Signals based on 20-day SMA:");
        for (String signal : signals) {
            System.out.println(signal);
        }
    }
}

