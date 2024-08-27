package com.baeldung.lsd.worker;

import com.baeldung.lsd.persistence.repository.ProductChartRepository;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.hibernate.annotations.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class ChartWorker implements Worker {

    private final String taskDefName;

    @Autowired
    private ProductChartRepository productChartRepository;

    public ChartWorker(@Value("taskDefName") String taskDefName) {
        this.taskDefName = taskDefName;
    }

    @Override
    public String getTaskDefName() {
        return taskDefName;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        String id = (String) task.getInputData().get("id");

        result.addOutputData("info", productChartRepository.findById(Long.parseLong(id)));

        System.out.println("Chiamata chart service");

        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }

}
