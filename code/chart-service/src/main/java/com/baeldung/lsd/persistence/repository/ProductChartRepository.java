package com.baeldung.lsd.persistence.repository;

import com.baeldung.lsd.persistence.model.ProductChart;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

public interface ProductChartRepository extends CrudRepository<ProductChart, Long> {
    Optional<ProductChart> findByCode(String code);
}
