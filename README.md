# 🚀 Concurrent Stock Trading & Order Matching Engine

A backend trading system built with **Java and Spring Boot** that simulates stock trading with **BUY/SELL orders, price-time priority, portfolio management, transaction processing, concurrency control, JWT authentication, and an in-memory order book**.

The project started as a basic trading backend and was progressively optimized toward a **concurrent and performance-oriented order matching system**.

---

## 📌 Project Overview

The system allows users to:

* Register and authenticate securely
* View available stocks
* Place BUY and SELL orders
* Lock funds and shares for pending orders
* Automatically match compatible BUY and SELL orders
* Handle partial order execution
* Maintain user portfolios
* Record completed transactions
* Cancel pending orders
* Recover pending orders after application restart
* Process concurrent orders safely

The core component is a **price-level order matching engine** designed around the **Price-Time Priority** principle.

---

## 🏗️ Architecture

```text
                        ┌──────────────────────┐
                        │       Client         │
                        └──────────┬───────────┘
                                   │
                                   ▼
                        ┌──────────────────────┐
                        │    REST Controller   │
                        └──────────┬───────────┘
                                   │
                                   ▼
                        ┌──────────────────────┐
                        │ Spring Security/JWT  │
                        └──────────┬───────────┘
                                   │
                                   ▼
                        ┌──────────────────────┐
                        │     OrderService     │
                        └──────────┬───────────┘
                                   │
                         ┌─────────┴─────────┐
                         │                   │
                         ▼                   ▼
                  ┌─────────────┐     ┌─────────────┐
                  │ Stock Lock  │     │   Disruptor │
                  └──────┬──────┘     │  RingBuffer │
                         │             └──────┬──────┘
                         ▼                    │
                  ┌─────────────┐             ▼
                  │  OrderBook  │◄───── Event Handler
                  └──────┬──────┘
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
        ┌─────────────┐       ┌─────────────┐
        │ BUY Levels  │       │ SELL Levels │
        │   TreeMap   │       │   TreeMap   │
        └──────┬──────┘       └──────┬──────┘
               │                     │
               ▼                     ▼
        ┌─────────────┐       ┌─────────────┐
        │  ArrayDeque │       │  ArrayDeque │
        │ FIFO Orders │       │ FIFO Orders │
        └─────────────┘       └─────────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Matching Engine │
                └────────┬────────┘
                         │
             ┌───────────┼───────────┐
             ▼           ▼           ▼
        Portfolio    Transaction   Balance
         Updates       History     Updates
             │           │           │
             └───────────┼───────────┘
                         ▼
                  ┌─────────────┐
                  │   Database  │
                  └─────────────┘
```

---

# ✨ Key Features

## 🔐 Authentication & Authorization

* Spring Security integration
* JWT-based authentication
* Stateless authentication
* Password hashing using BCrypt
* Protected REST APIs
* Role-based authorization

---

## 📈 Stock Management

Stocks contain information such as:

* Stock ID
* Symbol
* Company name
* Current price
* Available quantity

Admin-level operations can be used to manage stocks.

---

## 💰 BUY & SELL Orders

Users can submit:

```text
BUY  → Stock + Quantity + Price
SELL → Stock + Quantity + Price
```

Before accepting an order, the system verifies whether the user has sufficient resources.

For BUY orders:

```text
Required Amount = Price × Quantity

Available Balance =
Total Balance - Locked Balance
```

For SELL orders:

```text
Available Shares =
Total Shares - Locked Shares
```

This prevents users from committing the same funds or shares to multiple pending orders.

---

# 📚 Order Matching Engine

The heart of the project is the **Order Matching Engine**.

The engine follows **Price-Time Priority**.

### BUY Orders

Higher price gets higher priority.

```text
BUY
100 → highest priority
 99
 98
```

### SELL Orders

Lower price gets higher priority.

```text
SELL
101 → highest priority
102
103
```

If two orders have the same price, the order that arrived first gets priority.

This provides FIFO behavior at every price level.

---

# 🧠 Order Book Design

Each stock maintains its own `OrderBook`.

```text
Stock
  │
  └── OrderBook
       │
       ├── BUY
       │    └── TreeMap<Double, ArrayDeque<Order>>
       │
       └── SELL
            └── TreeMap<Double, ArrayDeque<Order>>
```

### BUY

```java
TreeMap<Double, ArrayDeque<Order>>
```

with reverse ordering.

Therefore:

```text
Highest BUY price → first entry for matching
```

### SELL

```java
TreeMap<Double, ArrayDeque<Order>>
```

with natural ordering.

Therefore:

```text
Lowest SELL price → first entry for matching
```

---

# ⚡ Why ArrayDeque?

The initial implementation used queue-based structures.

The order book was later optimized to use:

```java
ArrayDeque<Order>
```

at every price level.

The matching engine primarily requires:

```java
offer()
peek()
poll()
```

which naturally represents FIFO order processing.

Therefore:

```text
Price Level
     ↓
ArrayDeque
     ↓
FIFO Order Matching
```

This reduces unnecessary queue abstraction and provides an efficient structure for same-price orders.

---

# 🔄 Partial Order Matching

The engine supports partial fills.

Example:

```text
BUY  → 100 shares @ ₹100
SELL → 40 shares  @ ₹100
```

The engine executes:

```text
40 shares
```

Remaining:

```text
BUY → 60 shares
SELL → COMPLETED
```

The BUY order becomes:

```text
PARTIALLY_FILLED
```

The engine continues matching the remaining quantity with subsequent compatible SELL orders.

---

# 🔒 Concurrency & Thread Safety

A major focus of the project is handling concurrent orders.

Multiple users may submit orders for the same stock simultaneously.

Without synchronization, this could result in:

* Race conditions
* Double matching
* Incorrect quantities
* Inconsistent balances
* Corrupted order-book state

To handle this, the system uses **stock-level locking**.

```text
Stock A → Lock A
Stock B → Lock B
Stock C → Lock C
```

Instead of using one global lock for the complete application, each stock has its own lock.

Matching is protected using:

```java
synchronized (getStockLock(stockId)) {
    // matching logic
}
```

This isolates concurrent operations at the stock level.

---

# 🧵 ConcurrentHashMap

The system uses:

```java
ConcurrentHashMap
```

for managing:

```text
Stock ID → OrderBook
Stock ID → Lock
```

`computeIfAbsent()` is used to create the required OrderBook or lock when needed.

Example:

```java
orderBooks.computeIfAbsent(
    stockId,
    id -> new OrderBook()
);
```

---

# 🚀 LMAX Disruptor

The project also explores **event-driven order processing using LMAX Disruptor**.

The processing flow is:

```text
Controller
    ↓
OrderEventProducer
    ↓
RingBuffer
    ↓
OrderEventHandler
    ↓
OrderService
    ↓
Order Matching Engine
```

The producer publishes an order event to the RingBuffer.

The event handler consumes the event and invokes the corresponding order-processing logic.

This architecture was introduced to explore:

* High-throughput event processing
* Low-latency communication
* Reduced contention
* RingBuffer-based processing
* Event-driven architecture

---

# 💾 Database Persistence

The database stores persistent information such as:

* Users
* Stocks
* Orders
* Portfolios
* Transactions

The application uses:

```text
Spring Data JPA
       ↓
Hibernate
       ↓
Relational Database
```

The database provides durability while the in-memory OrderBook provides fast access for matching.

---

# 🔄 Order Book Recovery

Since the OrderBook is maintained in memory, it would normally be lost when the application restarts.

To solve this, pending orders are restored from the database using `@PostConstruct`.

Startup flow:

```text
Application Starts
       ↓
Load PENDING /
PARTIALLY_FILLED Orders
       ↓
Identify Stock
       ↓
Find OrderBook
       ↓
Insert into BUY / SELL Price Level
       ↓
OrderBook Restored
```

This combines:

**Database → Persistence**

**Memory → Fast Matching**

---

# 💼 Portfolio Management

After a successful trade, the system updates both buyer and seller portfolios.

The system tracks:

* Quantity
* Average price
* Locked quantity
* User
* Stock

The portfolio is updated only after successful order matching.

---

# 💳 Balance & Fund Locking

For BUY orders:

```text
Required Amount
       ↓
Lock Balance
       ↓
Order Pending
       ↓
Trade Executed
       ↓
Deduct Balance
```

For SELL orders:

```text
Required Shares
       ↓
Lock Shares
       ↓
Order Pending
       ↓
Trade Executed
       ↓
Update Portfolio
```

This helps prevent double spending and double selling.

---

# 📊 Order Lifecycle

An order can move through the following states:

```text
           ┌───────────┐
           │  PENDING  │
           └─────┬─────┘
                 │
          Match Found
                 │
                 ▼
       ┌──────────────────┐
       │ PARTIALLY_FILLED │
       └────────┬─────────┘
                │
         Remaining Qty = 0
                │
                ▼
          ┌───────────┐
          │ COMPLETED │
          └───────────┘
```

A pending order can also be:

```text
PENDING → CANCELLED
```

---

# 🛠️ Technology Stack

| Technology        | Purpose                        |
| ----------------- | ------------------------------ |
| Java              | Core programming language      |
| Spring Boot       | Backend framework              |
| Spring Security   | Authentication & authorization |
| JWT               | Stateless authentication       |
| Spring Data JPA   | Database interaction           |
| Hibernate         | ORM                            |
| MySQL / H2        | Data persistence/testing       |
| Maven             | Dependency management          |
| ConcurrentHashMap | Thread-safe data management    |
| TreeMap           | Price-level ordering           |
| ArrayDeque        | FIFO order queues              |
| LMAX Disruptor    | Event-driven processing        |
| REST API          | Client-server communication    |
| IntelliJ IDEA     | Development environment        |
| Postman           | API testing                    |

---

# 📂 Project Structure

```text
src/main/java
│
└── com.example.InternProject
    │
    ├── Controller
    │
    ├── Service
    │
    ├── Model
    │
    ├── Repo
    │
    ├── Security
    │
    └── Config
```

Main components include:

```text
OrderController
OrderService
OrderBook
Order
Stock
Portfolio
Transaction
User
OrderEvent
OrderEventProducer
OrderEventHandler
DisruptorConfig
JWT Service
Security Configuration
```

---

# 🔌 Main API Operations

The application provides REST APIs for operations such as:

```text
GET     /stocks
POST    /stocks
PUT     /stocks/{id}
DELETE  /stocks/{id}

GET     /orders
GET     /orders/{id}

POST    /orders/buy
POST    /orders/sell

DELETE  /orders/{id}
```

Authentication endpoints are also available for user registration/login depending on the configured security flow.

---

# 🧪 Testing

The APIs can be tested using **Postman**.

Important scenarios tested include:

* User authentication
* BUY order placement
* SELL order placement
* Insufficient balance
* Insufficient shares
* Order cancellation
* Full matching
* Partial matching
* Multiple orders at the same price
* Multiple price levels
* Concurrent order processing
* Application restart and pending-order recovery

---

# 📈 Performance Optimization Roadmap

Performance optimization is being approached incrementally.

### Completed

* Price-level order book
* Separate BUY / SELL price levels
* FIFO order queues
* `ArrayDeque` optimization
* Stock-level locking
* ConcurrentHashMap
* Event-driven processing with LMAX Disruptor

### Current Focus

* Matching latency benchmarking
* Database bottleneck identification
* Reducing unnecessary database operations
* Object allocation analysis
* Garbage collection analysis
* Lock contention analysis
* Throughput testing

The goal is to optimize based on **measured bottlenecks rather than assumptions**.

---

# 🎯 Key Design Decisions

### Why TreeMap?

Because the matching engine needs the best price quickly.

```text
BUY  → Highest price
SELL → Lowest price
```

TreeMap keeps price levels ordered.

---

### Why ArrayDeque?

Orders at the same price must follow FIFO.

```text
offer() → Add order
peek()  → View oldest order
poll()  → Remove oldest order
```

---

### Why ConcurrentHashMap?

Order books and stock locks are shared across concurrent requests.

ConcurrentHashMap provides thread-safe access to these structures.

---

### Why Stock-Level Locking?

A global lock would unnecessarily block unrelated stocks.

With stock-level locking:

```text
Stock A → Lock A
Stock B → Lock B
```

Operations on different stocks can be isolated.

---

### Why In-Memory OrderBook + Database?

The database provides:

**Durability**

The in-memory OrderBook provides:

**Fast matching**

This creates a balance between persistence and performance.

---

# 📚 What I Learned

This project helped me understand practical backend and system-design concepts including:

* Spring Boot architecture
* REST API development
* Spring Security
* JWT authentication
* JPA/Hibernate
* Database transactions
* Order book architecture
* Price-Time Priority
* Partial order matching
* ConcurrentHashMap
* Thread synchronization
* Stock-level locking
* Event-driven architecture
* LMAX Disruptor
* RingBuffer
* Data structure optimization
* Performance bottleneck analysis

The project also helped me understand that building a system is not only about making it functional.

It is also about making it:

```text
Correct
   ↓
Thread-Safe
   ↓
Persistent
   ↓
Recoverable
   ↓
Scalable
   ↓
Performant
```

---

# 🔮 Future Enhancements

Planned improvements include:

* Load testing with concurrent users
* Matching-engine latency benchmarks
* Database query optimization
* Connection-pool tuning
* Metrics and monitoring
* Distributed order-book architecture
* Redis-based caching
* Kafka/event streaming integration
* Microservices architecture
* Docker containerization
* Cloud deployment
* Advanced performance profiling

---

# 👨‍💻 Project Goal

The long-term goal of this project is to evolve it from a basic stock trading backend into a **high-throughput, concurrent, and scalable trading simulation platform** while continuously measuring and improving its performance.

---

## ⭐ Key Highlight

> **Built a concurrent stock order matching engine using Java and Spring Boot with price-time priority, TreeMap-based price levels, ArrayDeque FIFO queues, stock-level locking, JWT security, persistent order-book recovery, and LMAX Disruptor-based event processing.**

---

## 📌 Status

🚧 **Active Development**

The core trading and matching functionality is implemented. Current development focuses on **performance benchmarking, database optimization, concurrency testing, and scalability improvements.**

