package com.baeldung.lsd;

import com.baeldung.lsd.persistence.repository.ProductChartRepository;
import com.baeldung.lsd.worker.ChartWorker;
import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.Collection;

@SpringBootApplication
public class ChartSetupApp implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ChartSetupApp.class);
    @Autowired
    private ProductChartRepository productChartRepository;


    public static void main(final String... args) {

        SpringApplication.run(ChartSetupApp.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        TaskClient taskClient = new TaskClient();
        taskClient.setRootURI("http://localhost:8080/api/"); // Point this to the server API

        int threadCount = 1; // number of threads used to execute workers.  To avoid starvation, should be


        Worker worker1 = new ChartWorker("get_chart_info", productChartRepository);


        Collection workerArrayList = new ArrayList<Worker>();
        workerArrayList.add(worker1);

        TaskRunnerConfigurer configurer =
                new TaskRunnerConfigurer.Builder(taskClient, workerArrayList)
                        .withThreadCount(threadCount)
                        .build();
        // Start the polling and execution of tasks
        configurer.init();

        LOG.info("Starting Spring Boot application...");
    }

}
