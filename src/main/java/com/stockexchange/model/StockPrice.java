package com.stockexchange.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_prices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockPrice implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 10)
  private String symbol;

  @Column(nullable = false)
  private Double price;

  @Column(nullable = false)
  private Instant timestamp;

  @Column(nullable = false)
  private Double change;

  @Column(nullable = false)
  private Double changePercent;

  @Column
  private Long volume;

  @Column
  private Double dayHigh;

  @Column
  private Double dayLow;

  @Column
  private Double openPrice;

  @Column
  private Double previousClose;

  public StockPrice(String symbol, Double price, Instant timestamp,
      Double change, Double changePercent) {
    this.symbol = symbol;
    this.price = price;
    this.timestamp = timestamp;
    this.change = change;
    this.changePercent = changePercent;
  }
}
