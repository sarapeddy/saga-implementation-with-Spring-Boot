# SAGA Pattern - implementation with Orkes Conductor

In this notebook, we discuss the implementation of the SAGA pattern using Spring Boot. As mentioned [here](/saga_theory.md), there are two approaches to implementing the pattern: **choreography** or **orchestration**. Considering the characteristics of both, implementing SAGA Pattern using an orchestrator is definitely a better choice, at least in our context. In the following sections, each step required to achieve the proposed result in the [project](../code/) is explained in detail.


## Project structure

A typical example of a microservices architecture is e-commerce platforms where purchases can be made. For this reason, the implemented project features a system composed of three microservices implemented with Spring Boot, each with a specific function, which together create a simplified version of an e-commerce site:

- `warehouse-service`, which handle the products in the warehouse;

- `cart-service`, which handle the products in the chart;

- `purchase-service`, which handle the purchased products.

As orchestrator, we will use **Orkes Conductor** because it is easily deployable via a Docker container and has an intuitive graphical interface. This interface allows us to define workflows, manage invocations, and configure the tasks. We will explore these aspects in more detail as we proceed.


## The workflow to implement

To ensure that the orchestrator can manage transactions across each microservice's database and keep the data consistent, workflows must be defined. This allows the orchestrator to know how to handle the various situations that may arise.


<div style="text-align: center">
    <img src="/slides/images/workflow.png" alt="workflow " width="300" height="300">
</div>

We previously mentioned that our project aims to simulate an e-commerce platform, so every product (before the sale) must do some checks. For instance it is important to verify if a number of credit card is valid. The workflow shown at the beginning represents the steps a product must go through before being sold. Essentially, a workflow in Orkes is an implementation of an activity diagram, consisting of a series of tasks to be executed in sequence. Specifically, our workflow want to represent the product sale process. It includes:

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

- **conductor**: This container runs the Orkes Conductor image, which is used for the orchestration task and to run the workflows. It is important to remember that it is the only container which expose 2 ports: `5000` to access at the UI and `8080` to allow the services to register their tasks. 

- **cart**: This container hosts the `cart-service`.

- **warehouse**: This container hosts the `warehouse-service`.

- **purchase**: This container hosts the `purchase-service`.

## Workflow definition with Conductor

Once the theoretical aspects of the workflow are defined, including its tasks, input and output parameters, and so on, it's time to move on to the implementation using Orkes Conductor. Orkes Conductor uses a JSON file to define a workflow. It becomes a one-to-one translation of what was previously done with some more information treated as metadata. These ones are very important because they specify task lifecycle, how many times a task must be retry in case of fall and so on. The structure of the JSON file becomes as follows:

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


It outlines the sequence of tasks and their configuration. The best approach you can follow to develop it is reported as follows:

- **Define the Workflow Structure**: In the JSON file, specify the overall structure of the workflow, including its name and any metadata. This serves as a blueprint for the tasks to be executed.

- **Specify Tasks**: Each task within the workflow is defined with its own JSON object. Tasks include information such as the task type, the service it interacts with, and any input parameters it requires.

- **Configure Task Parameters**: For each task, define the input parameters that it requires and the output it produces. This ensures that data flows correctly between tasks and that each task has the necessary information to execute properly.

- **Handle Task Transitions**: Define how tasks transition from one to another. This includes specifying conditions under which tasks should be executed, how to handle retries, and what actions to take in case of failures.

- **Error Handling and Compensation**: Implement error handling and compensation logic to manage any issues that arise during task execution. This might involve specifying compensating transactions or fallback actions to ensure the workflow completes successfully or is properly rolled back.



## Task definition with Conductor


## How to execute the workflow



## References

[Saga Pattern with Orkes](https://www.baeldung.com/orkes-conductor-saga-pattern-spring-boot)

[Conductor Documentation](https://orkes.io/content/)

[Operators in Orkes Conductor](https://orkes.io/content/category/reference-docs/operators)

[Basic Example of how to use Orkes Conductor](https://github.com/crisandolindesmanrumahorbo/conductor-netflix-demo)

[Java JPA Documentation](https://docs.spring.io/spring-data/jpa/reference/jpa.html)