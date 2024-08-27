package com.baeldung.lsd.worker;

import com.baeldung.lsd.persistence.model.ProductPurchase;
import com.baeldung.lsd.persistence.repository.ProductPurchaseRepository;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.beans.factory.annotation.Value;


import java.util.Optional;

public class InsertProductPurchaseWorker implements Worker {

    private final String taskDefName;
    private final ProductPurchaseRepository productPurchaseRepository;

    public InsertProductPurchaseWorker(@Value("taskDefName") String taskDefName, ProductPurchaseRepository productPurchaseRepository) {
        System.out.println("TaskDefName: " + taskDefName);
        this.taskDefName = taskDefName;
        this.productPurchaseRepository = productPurchaseRepository;
    }

    @Override
    public String getTaskDefName() {
        return taskDefName;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        String id = (String) task.getInputData().get("id");

        Optional<ProductPurchase> productPurchase = productPurchaseRepository.findById(Long.parseLong(id));

        result.addOutputData("info", productPurchase);

        System.out.println("Info: " + productPurchase);
        System.out.println("Insert product purchase");

        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }

}
