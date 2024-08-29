# SAGA Design Pattern

## Introduction

The *SAGA design pattern* is a software architectural pattern used to manage complex, long-running transactions across multiple microservices or distributed systems. In a SAGA, a sequence of smaller, isolated transactions is executed step by step. Each transaction is managed by a separate microservice and, if one transaction fails, the pattern ensures the execution of compensating transactions to undo the effects of the preceding steps, thereby maintaining data consistency. This approach is particularly useful in microservices architectures where a single monolithic transaction is not feasible due to the distributed nature of the system. 

In a microservices architecture, traditional ACID (Atomicity, Consistency, Isolation, Durability) transactions become challenging to implement because they rely on a single, centralized database to ensure data consistency across multiple operations. However, in a microservices environment, data is typically distributed across multiple services, each with its own database. This distribution makes it difficult to maintain ACID properties across service boundaries, particularly **isolation**, which ensures that transactions do not interfere with each other.As a result, an application must use what are known as *countermeasures*, design techniques that prevent or reduce the impact of concurrency anomalies caused by the lack of
isolation. This topic and how to address it will be discussed in more detail later in the notebook.


## Anomalies that the lack of isolation can cause

The lack of isolation, as mentioned at the beginning, can cause the three following problems:

- **Lost Updates**: This anomaly occurs when one saga overwrites the changes made by another saga without first reading them. As a result, critical updates can be lost, leading to inconsistent or incorrect data in the system.

- **Dirty Reads**: This happens when a saga reads data that is currently being updated by another saga that hasn't yet completed its transaction. This can lead to decisions based on incomplete or incorrect data, potentially causing significant issues like exceeding credit limits or making unauthorized changes.

- **Fuzzy/nonrepeatable reads**: It happens when two different steps of a saga read the same data and
get different results because another saga has made updates.

## How to solve the lack of isolation problem

The saga transaction model adheres to ACD (Atomicity, Consistency, Durability) principles but lacks isolation, leading to potential anomalies that can cause issues in applications. Developers must implement strategies to either prevent these anomalies or reduce their impact on the business.  The key countermeasures usually used are:

- **Semantic lock**: involves setting a flag on a record during a compensatable transaction to indicate that the record is not yet finalized and could change. This flag can act as a lock, preventing other transactions from accessing the record, or as a warning, advising other transactions to proceed with caution. The flag is cleared when the saga completes successfully (via a retriable transaction) or when it rolls back (via a compensating transaction).

- **Commutative updates**: Design updates to be executable in any order. They must be commutative.

- **Pessimistic view**: Reorder saga steps to minimize business risk. It addresses the lack of isolation by reordering the steps of a saga to reduce business risks associated with dirty reads. By strategically adjusting the sequence of transactions, this approach ensures that more critical operations occur in a way that minimizes the potential impact of inconsistent data, thereby reducing the likelihood of errors caused by concurrent transactions.

- **Reread value**: Prevent dirty writes by verifying data before overwriting it. If the record has been modified, the saga aborts and may restart. This approach is a form of the Optimistic Offline Lock pattern. By checking for changes before committing an update, this countermeasure ensures that the saga operates on consistent data, reducing the risk of conflicts or overwriting updates made by other processes.

- **Version file**: Record updates to allow reordering. Th
is countermeasure addresses the issue of out-of-order operations by recording each operation performed on a record. This log allows the system to reorder operations to ensure they are applied in the correct sequence. By maintaining a record of operations, the system can handle concurrent requests more effectively. This approach effectively transforms noncommutative operations into commutative ones, ensuring consistency even when transactions are processed out of order.

- **By value**: Use the business risk of each request to dynamically select a concurrency mechanism. It involves selecting concurrency mechanisms based on the business risk associated with each request. For low-risk requests, such as those involving minor operations, sagas with appropriate countermeasures can be used. For high-risk requests, like those involving substantial financial transactions, distributed transactions are employed to ensure higher levels of consistency and reliability. This approach allows applications to balance business risk, availability, and scalability dynamically.


## Why cannot we use a distributed transaction?


Distributed transactions, managed through the X/Open Distributed Transaction Processing (DTP) Model and typically implemented with two-phase commit (2PC), ensure that all participants in a transaction either commit or roll back together. While this approach may seem straightforward, it has significant limitations.

One major issue is that many modern technologies, including NoSQL databases like MongoDB and Cassandra, as well as modern message brokers like RabbitMQ and Apache Kafka, do not support distributed transactions. Additionally, distributed transactions are synchronous, meaning that all participating services must be available for the transaction to complete. This requirement reduces the overall availability of the system because the availability of the entire transaction is the product of the availability of each service involved. According to the CAP theorem, systems can only achieve two out of three properties: consistency, availability, and partition tolerance. Modern architectures often prioritize availability over consistency.

Although distributed transactions offer a familiar programming model similar to local transactions, these challenges make them unsuitable for modern applications. Instead, to maintain data consistency in a microservices architecture, a different approach is needed one that leverages loosely coupled, asynchronous services. This is where the SAGA pattern comes into play.

![Distribution transaction](/slides/images/distri_trans.png)

In the image there is an example of what we have mentioned at the beginning of the pragraph. It illustrates `createOrder()` operation in a microservices architecture. This operation involves multiple services and must ensure data consistency across them. The diagram shows the `Order Service`, `Consumer Service`, `Kitchen Service`, and `Accounting Service`, each represented by hexagons. 

- The `Order controller` initiates the `createOrder()` process.
- The `Order Service` reads data from the `Consumer Service`, which manages consumer information.
- The `Order Service` then writes data to both the `Kitchen Service`, which handles ticket information, and the `Accounting Service`, which manages account details.

The dashed box around these services indicates the need for data consistency when performing these operations. The figure emphasizes that the `createOrder()` operation must update data across several services, requiring a mechanism to maintain consistency.

## How can we implement SAGA Pattern?

The alternative to the distributed transactions is SAGA PAttern. To implement it, two primary approaches can be utilized:

- **Choreography**: In this approach, each service involved in the saga performs its local transaction and then publishes an event to signal other services that the next step can proceed. This method is decentralized, with services reacting to events and executing their respective tasks. While choreography can simplify the design by eliminating the need for a central controller, it may lead to increased complexity as the number of interactions grows, making the overall process harder to manage and understand.

<div style="text-align: center">
    <img src="/slides/images/choreography.jpg" alt="choreography" width="300" height="300">
</div>


- **Orchestration**: This approach involves a centralized controller, known as the saga orchestrator, which coordinates the sequence of transactions. The orchestrator sends commands to each service to execute its part of the saga. If a step fails, the orchestrator triggers compensating transactions to roll back completed steps, thereby maintaining consistency. Orchestration offers better control and visibility over the saga's execution, though it introduces a potential single point of failure and can complicate the orchestrator's logic.

<div style="text-align: center">
    <img src="/slides/images/orchestration.jpg" alt="orchestration" width="300" height="300">
</div>

Both approaches are designed to maintain data consistency across distributed services without relying on traditional ACID transactions, making them essential in a microservices architecture. The choice between choreography and orchestration depends on the system's specific requirements and complexity.

## Why is orchestration more widespread?

### Limitations of Choreography

Coreography has some limitations:

- **Tight Coupling**: Services are directly connected, meaning changes in one service can impact others, complicating upgrades.
- **Distributed State**: Managing state across microservices complicates process tracking and may require additional infrastructure.
- **Troubleshooting Complexity**: Debugging is harder with dispersed service flows, requiring centralized logging and deep code knowledge.
- **Testing Challenges**: Interconnected microservices make testing more complex for developers.
- **Maintenance Difficulty**: As services evolve, adding new versions can reintroduce complexity, resembling a distributed monolith.

### Advantages of Orchestration

On the other hands, Orchestration has more advantages that makes this solution more feasible in many contexts:

- **Coordinated Transactions**: A central coordinator manages the execution of microservices, ensuring consistent transactions across the system.
- **Compensation**: Supports rollback through compensating transactions in case of failures, maintaining system consistency.
- **Asynchronous Processing**: Microservices operate independently, with the orchestrator managing communication and sequencing.
- **Scalability**: Easily scale by adding or modifying services without major impact on the overall application.
- **Visibility and Monitoring**: Centralized visibility enables quicker issue detection and resolution, improving system reliability.
- **Faster Time to Market**: Simplifies service integration and flow creation, speeding up adaptation and reducing the time to market.






## References

[Microservices Patterns - O'Reilly](https://www.oreilly.com/library/view/microservices-patterns/9781617294549/)


[Microservices Pattern - SAGA](https://microservices.io/patterns/data/saga.html)
