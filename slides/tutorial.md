# SAGA Pattern - implementation with Conductor

In this notebook, we discuss the implementation of the SAGA pattern using Spring Boot. As mentioned [here](/saga_theory.md), there are two approaches to implementing the pattern: **choreography** or **orchestration**. Considering the characteristics of both, implementing SAGA Pattern using an orchestrator is definitely a better choice, at least in our context. In the following sections, each step required to achieve the proposed result in the [project](../code/) is explained in detail.


## Project structure

The final goal we want to reach in this project is the implementation of SAGA Pattern with Spring Boot. To do this, we need an example project composed of some microservices.  A typical example of a microservices architecture is e-commerce platforms where purchases can be made. Each microservice takes care of a particular task, and overall the system works cooperatively to keep data up-to-date and consistent. For this reason, the implemented project features a system composed of three microservices implemented with Spring Boot, each with a specific function, which together create a simplified version of an e-commerce site. The three microservices involved are:

- `warehouse-service`, which handle the products in the warehouse;

- `cart-service`, which handle the products in the chart;

- `purchase-service`, which handle the purchased products.

As previously said, SAGA Pattern needs an orchestrator. To simulate it, we will use **Conductor** because it is an orchestrator easily deployable via a Docker container and has an intuitive graphical interface. This interface allows us to define workflows, manage invocations, and configure the tasks. We will explore these aspects in more detail as we proceed.

In our implementation, each microservice has its own database able to store Products. Each product is stored with four attributes:
- *id*: the auto generated primary key;
- *code*: an unique attribute used as an alternative key;
- *name*: the product name;
- *description*: the product description.

The interaction with the database (*Postgres*) is handles by Java JPA to avoid the SQL writing.

Overall, we have an architecture consisting of three microservices, each with its associated database that stores product information, along with an orchestrator that will manage the interactions between them.

## Conductor Overview

**Conductor** is a workflow orchestration framework, developed by Netflix, that allows for managing complex processes within microservices architectures. Conductor enables the definition of workflows as a sequence of independent tasks, each executed by a different microservice, ensuring scalability, resilience, and control. With support for various programming languages and integration with numerous technologies, Conductor is ideal for automating and orchestrating distributed processes efficiently. 

In our context it is necessary to add a dependencies in `pom.xml` to enable every interested microservices to interact with it:

```html
<dependency>
    <groupId>com.netflix.conductor</groupId>
    <artifactId>conductor-client</artifactId>
    <version>3.8.1</version>
</dependency>

<dependency>
    <groupId>com.netflix.conductor</groupId>
    <artifactId>conductor-common</artifactId>
    <version>3.8.1</version>
</dependency>
```

The `conductor-client` dependency provides the client API for interacting with Netflix Conductor, a microservices orchestration framework. This library allows developers to create, manage, and execute workflows and tasks within Conductor from their Java applications. The `conductor-common` dependency contains shared components and utilities that are used across various Conductor modules. This includes common data models, utility classes, and configurations that are essential for building and integrating with Conductor. It is typically used in conjunction with the conductor-client to provide a consistent and reusable set of tools for working with Conductor workflows.


## The workflow to implement

To ensure that the orchestrator can manage transactions across each microservice's database and keep the data consistent, workflows must be defined. This allows the orchestrator to know how to handle the various situations that may arise.


<div style="text-align: center">
    <img src="/slides/images/workflow.png" alt="workflow " width="250" height="600">
</div>

We previously mentioned that our project aims to simulate an e-commerce platform, so every product (before the sale) must do some checks. For instance it is important to verify if a number of credit card is valid. The workflow previously shown represents the steps a product must go through before being sold. Essentially, a workflow in Conductor environment is an implementation of an activity diagram, consisting of a series of tasks to be executed in sequence. Specifically, our workflow want to represent the product sale process. It includes:

1. Removing the product from the warehouse.

2. Adding the product to the cart.

3. Verifying the credit card number to decide whether to proceed with removing the item from the chart and completing the sale, or to simply keep the item in the cart if payment cannot be processed.

Furthermore, it takes two parameters as input:

- *productCode*: the primary key of the product;

- *creditCard*: the credit card number that we want to use to pay.

In this way, by executing the workflow, the databases remain consistently synchronized. For instance, if a product is to be purchased and the provided credit card number is valid, the final result is that the product is moved from the warehouse database to the purchases database. 

## Docker configuration

Before diving into the implementation details, it's helpful to review the Docker container configuration to get a clearer picture of the architecture. In our example, there are five containers:

- **postgres**: This container manages the databases for the microservices. Although each of them should ideally have its own separate database, we simulate this by creating separate tables within a single Postgres container to simplify setup and reduce resource usage.

- **conductor**: This container runs the Conductor image, which is used for the orchestration task and to run the workflows. It is important to remember that it is the only container which expose 2 ports: `5000` to access at the UI and `8080` to allow the services to register their tasks. 

- **cart**: This container hosts the `cart-service`.

- **warehouse**: This container hosts the `warehouse-service`.

- **purchase**: This container hosts the `purchase-service`.

## Workflow definition with Conductor

Once the theoretical aspects of the workflow are defined, including its tasks, input and output parameters, and so on, it's time to move on to the implementation using Conductor. Conductor uses a JSON file to define a workflow. It becomes a one-to-one translation of what was previously done with some more information treated as metadata. These ones are very important because they specify task lifecycle, how many times a task must be retry in case of fall and so on. The basic structure of the JSON file becomes as follows:

```json
{
  "name": "worflow_name",
  "description": "Workflow for handling product purchases",
  "version": 1,
  "tasks": [
    {
      "name": "task1_name",
      "type": "SIMPLE",
      "inputParameters": {
        "id": "...",
        ...
      },
      "outputParameters": {
        ...
      }
    },
    {
      "name": "task2_name",
      "type": "SIMPLE",
      "inputParameters": {
        ...
      },
      "outputParameters": {
        ...
      }
    },

    ....
  ]
}
```

Of course, there may be cases, as in our example, where more complex constructs are needed, involving the use of specific operators such as SWITCH, DO-WHILE, and others. For the specific syntax, you can refer to the [reference](https://conductor-oss.github.io/conductor/documentation/configuration/workflowdef/operators/index.html).

The complete implementation of our workflow is provided in the file `buy_product_workflow.json`.


## Task definition with Conductor

Each workflow is composed of a set of task. Each of them must be defined. To do this, there are two main steps to follow:

1. Define the task characteristics using a JSON file.

2. Implement the tasks in Java within the various microservices by implementing the [Worker](https://conductor-oss.github.io/conductor/devguide/how-tos/Workers/build-a-java-task-worker.html) interface from Conductor OSS.

### 1. Task Definition with JSON File

For each task, just like for each workflow, we need to define not only the name but also any associated **metadata**. These typically follow a standard configuration that doesn’t require many modification. Below it is reported an example that corresponds to the `check_credit_card` task in our workflow:

```json
{
  "createdBy": "",
  "updatedBy": "",
  "name": "check_credit_card",
  "description": "Check number of credit card",
  "retryCount": 3,
  "timeoutSeconds": 1200,
  "inputKeys": [
    "integer"
  ],
  "outputKeys": [
    "sum"
  ],
  "timeoutPolicy": "TIME_OUT_WF",
  "retryLogic": "FIXED",
  "retryDelaySeconds": 60,
  "responseTimeoutSeconds": 600,
  "inputTemplate": {},
  "rateLimitPerFrequency": 0,
  "rateLimitFrequencyInSeconds": 1,
  "ownerEmail": "yes.in.a.jiffy@gmail.com",
  "backoffScaleFactor": 1
}
```

The JSON files for each task present in the considered microservice can be found in the `/resources/task` directory.

### 2. Java implementation
When it comes to defining the task in Java, we simply need to implement a class following this structure:

```java
package com.baeldung.lsd.worker;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.beans.factory.annotation.Value;

public class CreditCardWorker implements Worker {

    private final String taskDefName;

    public CreditCardWorker(@Value("taskDefName") String taskDefName) {
        this.taskDefName = taskDefName;
    }

    ...

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        String creditCard = (String) task.getInputData().get("creditCard");

        System.out.println("Credit card: " + creditCard);

        if (creditCard != null && creditCard.matches("^\\d{16}$")) {
            result.addOutputData("status", "valid");
        } else {
            result.addOutputData("status", "Invalid credit card");
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("Controllo numero di carta di credito");
        return result;
    }

}

```
Essentially, each Worker is associated with a Task, and the task's logic is implemented within the `execute()` method. The example provided is the implementation of the `check_credit_card` task from our workflow.

Typically, it is considered a best practice to place the implementation of Workers in a dedicated package named `worker`.

It is also necessary to instantiate the Worker when the service starts. So, every *main* class must be as follows:

```java
@SpringBootApplication
public class CartSetupApp implements ApplicationRunner {
    ....

    public static void main(final String... args) {
        SpringApplication.run(CartSetupApp.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        TaskClient taskClient = new TaskClient();
        taskClient.setRootURI("http://conductor:8080/api/"); // Point this to the server API

        int threadCount = 1; // number of threads used to execute workers.  To avoid starvation, should be
        Worker worker = new CreditCardWorker("check_credit_card");
        ...

        Collection workerArrayList = new ArrayList<Worker>();
        workerArrayList.add(worker);
        ...

        TaskRunnerConfigurer configurer =
                new TaskRunnerConfigurer.Builder(taskClient, workerArrayList)
                        .withThreadCount(threadCount)
                        .build();
        // Start the polling and execution of tasks
        configurer.init();
    }
}
```


## How to execute the workflow with Docker

Now that we have defined the workflow (both theoretically and practically), as well as the tasks and written the code to manage the JPA `@Entity` classes, we can move on to see how to actually execute the workflow (the database implementation isn't treated in this notebook because it is already known - see JPA reference or the project code).

For first, it is necessary to generate the .jar files to start Docker:
```sh
$ mvn clean package
$ docker compose build
$ docker compose up
```
At this point navigate to 
[http://localhost:1234/](http://localhost:1234/): here there is the **Conductor UI** as shown in the image below.

<div style="text-align: center">
    <img src="/slides/images/conductor_ui.png" alt="Conductor UI">
</div>

At the firt access, we need to save the workflow and tasks definitions. To do this click on *Definitions* and then on *New Workflow Definition* and *New Task Defintion*. The only things to do is copy and paste the JSON files defined before and click *Save*. See the image below:

<div style="text-align: center">
    <img src="/slides/images/def_workflow.png" alt="Workflow Definition">
</div>

To run the workflow, there is a section called ***Workbech***. 

<div style="text-align: center">
    <img src="/slides/images/workbench.png" alt="workbench">
</div>

Here, you can select which workflow execute and the set the input parameter. Once the configuration is ready click *Play*.

In our case, there are three main cases we can run to verify how workflow works:

1. Insert a correct *productCode* and *creditCard*:
    ```json
    {
      "productCode": "P7",
      "creditCard": "1234567891234567"
    }
    ```
   
2. Insert a correct *productCode* and incorrect *creditCard*:
   ```json
    {
      "productCode": "P7",
      "creditCard": "123456789"
    }
    ```
   
4. Insert a *pruductCode* that does not exist:
   ```json
    {
      "productCode": "False Code",
      "creditCard": "1234567891234567"
    }
    ```

**Note**: In this simplified implementation, the credit card number is considered invalid if it is not 16 digits long.

For the first case, we will see that the workflow successed moving the product "P7" from the warehouse product tablet to the purchase product table. For the second case, the product "P7" remains in cart product table because the credit card is invalid. In the end, in the third case the workflow failed.

## How can to see the execution results

In Conductor UI there is also another section called ***Executions***. Inside it is possible to see the results of every workflow executed and analyze the input/output parameters. It is very useful for debugging stuff. 

<div style="text-align: center">
    <img src="/slides/images/executions.png" alt="executions tab">
</div>

## How to check the elements in the database

Since the SAGA Pattern allows for managing elements in the database, it's important to note that the project dependencies include the H2 database:

```html
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
</dependency>
```

This provides a user-friendly graphical interface to query the implemented Postgres database. From the desired service, simply navigate to the URI `/h2-console` and enter the desired SQL in the appropriate field.

## Run Services without Docker

In this case, Conductor runs on Docker, but the services run locally. There is only one thing to remember: we have to change the Conductor url. So, the only line of code to change is the following one:

```java
taskClient.setRootURI("http://conductor:8080/api/");
```
and must be rewrite with:
```java
taskClient.setRootURI("http://localhost:8080/api/");
```
The rest of execution remains unchanged.

## Execute a workflow with ***curl***

In Netflix Conductor, you can trigger the execution of a workflow not only through the Conductor UI but also via an HTTP request. This capability allows you to programmatically start workflows, making it easier to integrate Conductor into automated processes or other applications.

Here's an example of how to trigger a workflow using an HTTP request with `curl`:

```bash
curl -X POST http://localhost:8080/api/workflow/<example-workflow> \
     -H "Content-Type: application/json" \
     -d '{
           "name": "<example-workflow>",
           "version": 1,
           "input": {
             "productCode": "P7",
             "creditCard": "1234567891234567"
           }
         }'
```

Replace `<example-workflow` with the name of the workflow you want to execute. This command sends a `POST` request to start the specified workflow, including any necessary input parameters in the JSON payload.

## References

[Saga Pattern with Conductor - Baeldung](https://www.baeldung.com/orkes-conductor-saga-pattern-spring-boot)

[Conductor OSS site](https://conductor-oss.github.io/conductor/devguide/concepts/index.html)

[Conductor OSS Documentation]()

[Operators in Conductor](https://conductor-oss.github.io/conductor/documentation/configuration/workflowdef/operators/index.html)

[Basic Example of how to use Conductor](https://github.com/crisandolindesmanrumahorbo/conductor-netflix-demo)

[Java JPA Documentation](https://docs.spring.io/spring-data/jpa/reference/jpa.html)
