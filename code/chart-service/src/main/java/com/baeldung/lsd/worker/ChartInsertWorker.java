package com.baeldung.lsd.worker;

import com.baeldung.lsd.persistence.model.ProductChart;
import com.baeldung.lsd.persistence.repository.ProductChartRepository;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.beans.factory.annotation.Value;


import java.util.Optional;

public class ChartDeletionWorker implements Worker {

    private final String taskDefName;
    private final ProductChartRepository productChartRepository;

    public ChartDeletionWorker(@Value("taskDefName") String taskDefName, ProductChartRepository productChartRepository) {
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
        String id = (String) task.getInputData().get("id");

        Optional<ProductChart> productChart = productChartRepository.findById(Long.parseLong(id));

        result.addOutputData("info", productChart);

        System.out.println("Info: " + productChart);
        System.out.println("Chiamata chart service");

        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }

}
