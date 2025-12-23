package com.stockexchange.service;

import com.stockexchange.model.StockPrice;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Stock Price Publisher
 * <p>
 * Generates and broadcasts stock prices via STOMP. With RabbitMQ broker, messages are distributed
 * across ALL instances.
 */
@Component
@Slf4j
public class StockPricePublisher {

  private final StockPriceService stockPriceService;
  private final SimpMessagingTemplate messagingTemplate;

  public StockPricePublisher(
      StockPriceService stockPriceService,
      SimpMessagingTemplate messagingTemplate) {
    this.stockPriceService = stockPriceService;
    this.messagingTemplate = messagingTemplate;
  }

  @Scheduled(fixedRateString = "${stock-exchange.update-interval:1000}", initialDelay = 2000)
  public void publishStockPrices() {
    List<String> symbols = stockPriceService.getAvailableSymbols();

    for (String symbol : symbols) {
      try {
        StockPrice price = stockPriceService.generateAndSavePrice(symbol);

        // Use exchange destination format for RabbitMQ
        // Format: /exchange/<exchange-name>/<routing-key>
        String destination = "/exchange/amq.topic/stock." + symbol;

        messagingTemplate.convertAndSend(destination, price);

        log.trace("Published {} to {} price: ${}",
            symbol, destination, String.format("%.2f", price.getPrice()));

      } catch (Exception e) {
        log.error("Error publishing price for {}: {}", symbol, e.getMessage());
      }
    }
  }
}