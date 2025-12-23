package com.stockexchange.repository;

import com.stockexchange.model.StockPrice;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {

  @Query("SELECT sp FROM StockPrice sp WHERE sp.symbol = :symbol " +
      "ORDER BY sp.timestamp DESC LIMIT 1")
  Optional<StockPrice> findLatestBySymbol(String symbol);
}