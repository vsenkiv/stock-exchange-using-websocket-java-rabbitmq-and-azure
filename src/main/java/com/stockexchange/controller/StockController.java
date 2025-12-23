package com.stockexchange.controller;

import com.stockexchange.model.StockPrice;
import com.stockexchange.service.StockPriceService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
@CrossOrigin(origins = "*")
public class StockController {

  private final StockPriceService stockPriceService;

  public StockController(StockPriceService stockPriceService) {
    this.stockPriceService = stockPriceService;
  }

  @GetMapping("/symbols")
  public ResponseEntity<List<String>> getAvailableSymbols() {
    return ResponseEntity.ok(stockPriceService.getAvailableSymbols());
  }

  @GetMapping("/current")
  public ResponseEntity<List<StockPrice>> getAllCurrentPrices() {
    return ResponseEntity.ok(stockPriceService.getCurrentPrices());
  }

  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Stock Exchange API with RabbitMQ STOMP is running");
  }
}