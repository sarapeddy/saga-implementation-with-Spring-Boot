package com.baeldung.lsd.worker;

import com.baeldung.lsd.persistence.model.ProductChart;
import com.baeldung.lsd.persistence.repository.ProductChartRepository;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.beans.factory.annotation.Value;


import java.util.Optional;

public class ChartInsertWorker implements Worker {

    private final String taskDefName;
    private final ProductChartRepository productChartRepository;

    public ChartInsertWorker(@Value("taskDefName") String taskDefName, ProductChartRepository productChartRepository) {
        System.out.println("TaskDefName: " + taskDefName);
        this.taskDefName = taskDefName;
        this.productChartRepository = productChartRepository;
    }

    @Override
    public String getTaskDefName() {
        return taskDefName;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        String code = (String) task.getInputData().get("productCode");
        String name = (String) task.getInputData().get("name");
        String description = (String) task.getInputData().get("description");

        System.out.println("Code: " + code);
        System.out.println("Name: " + name);
        System.out.println("Description: " + description);

        ProductChart product = new ProductChart(code, name, description);

        productChartRepository.save(product);

        System.out.println("Add product to chart db");

        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }

}
