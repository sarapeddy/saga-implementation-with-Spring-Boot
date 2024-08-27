package com.baeldung.lsd.worker;

import com.baeldung.lsd.persistence.model.ProductChart;
import com.baeldung.lsd.persistence.repository.ProductChartRepository;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

public class ChartDeleteWorker implements Worker {

    private final String taskDefName;
    private final ProductChartRepository productChartRepository;

    public ChartDeleteWorker(@Value("taskDefName") String taskDefName,  ProductChartRepository productWarehouseRepository) {
        System.out.println("TaskDefName: " + taskDefName);
        this.taskDefName = taskDefName;
        this.productChartRepository = productWarehouseRepository;
    }

    @Override
    public String getTaskDefName() {
        return taskDefName;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        String code = (String) task.getInputData().get("productCode");

        Optional<ProductChart> productWarehouse = productChartRepository.findByCode(code);

        if(productWarehouse.isPresent()) {
            ProductChart product = productWarehouse.get();

            result.addOutputData("name", product.getName());
            result.addOutputData("description", product.getDescription());

            productChartRepository.delete(product);

            System.out.println("Delete product from warehouse");

            result.setStatus(TaskResult.Status.COMPLETED);
        }
        else {
            result.setStatus(TaskResult.Status.FAILED);
        }

        return result;
    }

}
