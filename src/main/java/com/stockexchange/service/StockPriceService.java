package com.stockexchange.service;

import com.stockexchange.model.StockPrice;
import com.stockexchange.repository.StockPriceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class StockPriceService {

  private final StockPriceRepository repository;
  private final Map<String, Double> initialPrices = new HashMap<>();
  @Value("${stock-exchange.volatility:0.02}")
  private double volatility;
  @Value("#{'${stock-exchange.symbols}'.split(',')}")
  private List<String> symbols;

  public StockPriceService(StockPriceRepository repository) {
    this.repository = repository;
    initializePrices();
  }

  private void initializePrices() {
    initialPrices.put("AAPL", 175.50);
    initialPrices.put("GOOGL", 140.25);
    initialPrices.put("MSFT", 380.75);
    initialPrices.put("TSLA", 242.50);
    initialPrices.put("AMZN", 155.80);
    initialPrices.put("NVDA", 495.20);
    initialPrices.put("META", 380.30);
    initialPrices.put("NFLX", 460.90);
    initialPrices.put("AMD", 145.60);
    initialPrices.put("INTC", 43.75);
  }

  @Transactional
  public StockPrice generateAndSavePrice(String symbol) {
    Optional<StockPrice> latestOpt = repository.findLatestBySymbol(symbol);

    StockPrice latest;
    if (latestOpt.isPresent()) {
      latest = latestOpt.get();
    } else {
      latest = initializeSymbol(symbol);
    }

    double currentPrice = latest.getPrice();
    double maxChange = currentPrice * volatility;
    double change = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * maxChange;
    double newPrice = Math.max(1.0, currentPrice + change);
    double changePercent = (change / currentPrice) * 100;

    StockPrice newStockPrice = new StockPrice();
    newStockPrice.setSymbol(symbol);
    newStockPrice.setPrice(newPrice);
    newStockPrice.setTimestamp(Instant.now());
    newStockPrice.setChange(change);
    newStockPrice.setChangePercent(changePercent);
    newStockPrice.setDayHigh(
        Math.max(latest.getDayHigh() != null ? latest.getDayHigh() : currentPrice, newPrice));
    newStockPrice.setDayLow(
        Math.min(latest.getDayLow() != null ? latest.getDayLow() : currentPrice, newPrice));
    newStockPrice.setOpenPrice(latest.getOpenPrice());
    newStockPrice.setPreviousClose(latest.getPreviousClose());
    newStockPrice.setVolume(
        Math.max(0, latest.getVolume() + ThreadLocalRandom.current().nextLong(-50000, 50000)));

    return repository.save(newStockPrice);
  }

  private StockPrice initializeSymbol(String symbol) {
    double basePrice = initialPrices.getOrDefault(symbol, 100.0);
    StockPrice price = new StockPrice();
    price.setSymbol(symbol);
    price.setPrice(basePrice);
    price.setTimestamp(Instant.now());
    price.setChange(0.0);
    price.setChangePercent(0.0);
    price.setOpenPrice(basePrice);
    price.setPreviousClose(basePrice);
    price.setDayHigh(basePrice);
    price.setDayLow(basePrice);
    price.setVolume(ThreadLocalRandom.current().nextLong(1000000, 10000000));
    return repository.save(price);
  }

  public List<String> getAvailableSymbols() {
    return new ArrayList<>(symbols);
  }

  public List<StockPrice> getCurrentPrices() {
    List<StockPrice> prices = new ArrayList<>();
    for (String symbol : symbols) {
      repository.findLatestBySymbol(symbol).ifPresent(prices::add);
    }
    return prices;
  }
}