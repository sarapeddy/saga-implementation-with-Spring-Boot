package com.baeldung.lsd.persistence.repository;

import com.baeldung.lsd.persistence.model.ProductChart;
import org.springframework.data.repository.CrudRepository;

public interface ProductChartRepository extends CrudRepository<ProductChart, Long> {
}
