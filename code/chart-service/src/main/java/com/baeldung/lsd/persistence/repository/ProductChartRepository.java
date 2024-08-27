package com.baeldung.lsd.persistence.repository;

import com.baeldung.lsd.persistence.model.ProductChart;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

public interface ProductChartRepository extends CrudRepository<ProductChart, Long> {
}
