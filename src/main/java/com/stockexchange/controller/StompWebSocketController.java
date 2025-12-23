package com.stockexchange.controller;

import com.stockexchange.model.StockPrice;
import com.stockexchange.repository.StockPriceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

/**
 * STOMP WebSocket Controller
 *
 * Handles client subscriptions and provides initial data
 */
@Controller
@Slf4j
public class StompWebSocketController {

  private final StockPriceRepository repository;

  public StompWebSocketController(StockPriceRepository repository) {
    this.repository = repository;
  }

  /**
   * Called when client subscribes to /exchange/amq.topic/stock.{symbol}
   * Returns current price immediately to subscribing client
   */
  @SubscribeMapping("/exchange/amq.topic/stock.{symbol}")
  public StockPrice onSubscribe(@DestinationVariable String symbol) {
    log.info("Client subscribed to stock: {}", symbol);
    return repository.findLatestBySymbol(symbol.toUpperCase()).orElse(null);
  }
}