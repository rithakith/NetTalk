# ChatSystem - Network Programming Assignment

## Project Title
**Scalable Real-Time Chat and Notification System**

A multi-client chat application demonstrating core Java Network Programming concepts including TCP sockets, multithreading, NIO, synchronization, and HTTP client integration.

---

## Group Members and Individual Contributions

### Member 1: TCP Connection Listener
**Files:** `ConnectionListener.java`

**Network Concept:** Blocking TCP Sockets with ServerSocket

**Implementation:**
- Created the core server component using `ServerSocket` to accept incoming client connections
- Implemented blocking I/O with `accept()` method to establish reliable TCP connections
- Demonstrates connection-oriented communication and TCP 3-way handshake
- Serves as the foundation for client-server architecture

**Key Classes/Methods:**
- `ServerSocket.accept()` - Blocking call to accept new connections
- Socket management and connection lifecycle

---

### Member 2: Concurrent Client Handler
**Files:** `ClientHandler.java`, `ChatServer.java` (ThreadPool integration)

**Network Concept:** Multithreading with ExecutorService and ThreadPool

**Implementation:**
- Implemented concurrent client handling using Java's `ExecutorService` and ThreadPool
- Each client connection is handled in a separate thread from a pool of reusable threads
- Prevents server overload by using bounded thread pools instead of unlimited thread creation
- Handles message processing, private messaging, and client disconnection

**Key Classes/Methods:**
- `Executors.newFixedThreadPool()` - Creates thread pool
- `ExecutorService.submit()` - Submits client handler tasks
- `Runnable` pattern for concurrent execution

---

### Member 3: NIO Message Broadcaster
**Files:** `MessageBroadcaster.java`

**Network Concept:** Java NIO (Non-blocking I/O with Channels, Buffers, and Selectors)

**Implementation:**
- Implemented high-performance message broadcasting using Java NIO
- Single thread efficiently monitors thousands of channels using `Selector`
- Non-blocking I/O operations with `SocketChannel` and `ByteBuffer`
- Demonstrates scalability: one thread handles many connections simultaneously

**Key Classes/Methods:**
- `Selector.select()` - Multiplexes multiple channels
- `SocketChannel` - Non-blocking channel operations
- `ByteBuffer` - Efficient data handling
- `SelectionKey` - Channel readiness state

**Note:** The NIO broadcaster is implemented but can be toggled in `ChatServer.java` for demonstration purposes.

---

### Member 4: Thread-Safe Resource Management
**Files:** `UserManager.java`

**Network Concept:** Synchronization and Concurrency Control

**Implementation:**
- Ensures thread-safe access to shared resources (user lists, chat history)
- Uses `synchronized` keyword for mutual exclusion
- Implements concurrent collections (`ConcurrentHashMap`, `CopyOnWriteArrayList`)
- Prevents race conditions and data inconsistency in multi-threaded environment
- Manages active user list and chat message history

**Key Classes/Methods:**
- `synchronized` blocks and methods
- `ConcurrentHashMap` - Thread-safe map operations
- `CopyOnWriteArrayList` - Thread-safe list operations
- Lock objects for complex operations

---

### Member 5: External API Integration
**Files:** `ExternalApiClient.java`

**Network Concept:** HTTP Client using URL and URLConnection

**Implementation:**
- Integrates external web services using Java's `URL` and `URLConnection`
- Makes HTTP GET requests to public APIs (weather, jokes, quotes)
- Demonstrates client-side HTTP communication
- Processes API responses and formats them for chat display

**Supported API Commands:**
- `/weather <city>` - Get weather using wttr.in API
- `/joke` - Get random joke from Official Joke API
- `/quote` - Get inspirational quote from Quotable API
- `/help` - Show available commands

**Key Classes/Methods:**
- `URL` - Represents web resource
- `HttpURLConnection` - HTTP-specific connection
- `URLConnection.openConnection()` - Opens connection to URL
- HTTP request/response handling

---

## System Overview

### Architecture
```
┌─────────────┐
│   Clients   │ (Multiple concurrent connections)
└──────┬──────┘
       │ TCP Sockets (Member 1)
       │
┌──────▼────────────────────────┐
│      ChatServer               │
│  ┌─────────────────────────┐ │
│  │ ConnectionListener      │ │ ← Member 1: TCP ServerSocket
│  │ (ServerSocket)          │ │
│  └────────┬────────────────┘ │
│           │                   │
│  ┌────────▼────────────────┐ │
│  │ ExecutorService         │ │ ← Member 2: ThreadPool
│  │ (ThreadPool)            │ │
│  └────────┬────────────────┘ │
│           │                   │
│     ┌─────▼─────┐            │
│     │ClientHandler│ (Multiple)│
│     │ threads   │            │
│     └─────┬─────┘            │
│           │                   │
│  ┌────────▼────────────────┐ │
│  │ UserManager             │ │ ← Member 4: Synchronization
│  │ (Thread-safe)           │ │
│  └─────────────────────────┘ │
│                               │
│  ┌─────────────────────────┐ │
│  │ MessageBroadcaster      │ │ ← Member 3: NIO
│  │ (NIO Selector)          │ │
│  └─────────────────────────┘ │
│                               │
│  ┌─────────────────────────┐ │
│  │ ExternalApiClient       │ │ ← Member 5: HTTP Client
│  │ (URL/URLConnection)     │ │
│  └─────────────────────────┘ │
└───────────────────────────────┘
```

### Features
1. **Multi-client Support** - Handles multiple concurrent users with thread pool
2. **Public Chat** - Broadcast messages to all connected users
3. **Private Messaging** - Send messages to specific users (@username format)
4. **User Management** - Thread-safe tracking of online users
5. **Chat History** - Maintains recent message history
6. **External API Integration** - Fetch weather, jokes, and quotes
7. **Graceful Shutdown** - Proper resource cleanup

---

## Network Programming Concepts Used

1. **TCP Sockets (Socket, ServerSocket)**
   - Connection-oriented, reliable communication
   - 3-way handshake for connection establishment
   - Ordered, error-checked data delivery

2. **Multithreading and Thread Pools (ExecutorService)**
   - Concurrent client handling
   - Thread reusability and resource management
   - Scalability with bounded thread pools

3. **Java NIO (Channels, Buffers, Selectors)**
   - Non-blocking I/O operations
   - Single thread manages multiple connections
   - High-performance, scalable architecture

4. **Synchronization and Concurrency Control**
   - Thread-safe data structures
   - synchronized keyword for mutual exclusion
   - Concurrent collections (ConcurrentHashMap, CopyOnWriteArrayList)
   - Race condition prevention

5. **HTTP Client (URL, URLConnection)**
   - HTTP GET requests to external APIs
   - Response parsing and handling
   - Integration with third-party services

---

## How to Run

### Prerequisites
- Java Development Kit (JDK) 8 or higher
- Terminal/Command Prompt

### Compile the Project
```bash
cd ChatSystem/src/main/java
javac com/chatapp/**/*.java
```

### Run the Server
```bash
java com.chatapp.server.ChatServer
```

The server will start on port 8888 and display:
```
============================================================
ChatSystem - Network Programming Assignment
Demonstrating 5 Network Programming Concepts
============================================================

[ConnectionListener] Server started on port 8888
[ConnectionListener] Waiting for client connections...
[ChatServer] Server started successfully!
```

### Run Clients (in separate terminals)
```bash
java com.chatapp.client.ChatClient
```

When prompted, enter a username. You can run multiple clients simultaneously.

### Using the Chat Client

**Regular Chat:**
```
Hello everyone!
```

**Private Message:**
```
@Alice Hey, how are you?
```

**API Commands:**
```
/weather London
/joke
/quote
/help
```

**Disconnect:**
```
/quit
```

### Frontend (Optional)
Open `frontend/index.html` in a web browser for a demonstration UI. 

**Note:** The HTML frontend is for demonstration only. To actually connect to the Java server, use the console-based `ChatClient.java` or implement a WebSocket bridge.

---

## Screenshots of Outputs

### Server Console
```
============================================================
ChatSystem - Network Programming Assignment
Demonstrating 5 Network Programming Concepts
============================================================

[ConnectionListener] Server started on port 8888
[UserManager] Thread-safe user manager initialized
[ConnectionListener] Waiting for client connections...
[ConnectionListener] New connection from: 127.0.0.1:54321
[ChatServer] Client handler submitted to thread pool
[ClientHandler-pool-1-thread-1] Handling client from: /127.0.0.1
[ClientHandler] User 'Alice' connected
[UserManager] User 'Alice' registered. Total users: 1
[ChatServer] Broadcasted: Server: Alice has joined the chat
[ConnectionListener] New connection from: 127.0.0.1:54322
[ChatServer] Client handler submitted to thread pool
[ClientHandler-pool-1-thread-2] Handling client from: /127.0.0.1
[ClientHandler] User 'Bob' connected
[UserManager] User 'Bob' registered. Total users: 2
[UserManager] Message added to history. Total messages: 1
[ChatServer] Broadcasted: Alice: Hello Bob!
[ExternalApiClient] Processing API request: /joke
[ExternalApiClient] Fetching random joke
[ExternalApiClient] HTTP Response Code: 200
```

### Client Console (Alice)
```
Enter your username: Alice
Connected to server as 'Alice'

Commands:
  Regular message: just type and press Enter
  Private message: @username message
  API commands: /weather <city>, /joke, /quote, /help
  Quit: /quit

[SYSTEM]: Welcome to the chat, Alice!
[SYSTEM]: Alice has joined the chat
[SYSTEM]: Bob has joined the chat
[Alice]: Hello Bob!
[Bob]: Hi Alice! How are you?
[Alice]: /joke

==================================================
😄 Random Joke:
Why do programmers prefer dark mode?
Because light attracts bugs!
==================================================
```

---

## Challenges Faced and Solutions

### Challenge 1: Thread Safety in Multi-threaded Environment
**Problem:** Multiple threads accessing shared user list and chat history caused race conditions and data corruption.

**Solution (Member 4):**
- Implemented `synchronized` blocks for critical sections
- Used concurrent collections (`ConcurrentHashMap`, `CopyOnWriteArrayList`)
- Created locks for complex operations requiring multiple steps
- Result: Thread-safe access to all shared resources

### Challenge 2: Server Scalability with Many Clients
**Problem:** Creating a new thread for each client doesn't scale well with hundreds or thousands of clients.

**Solution (Member 2):**
- Implemented thread pool using `ExecutorService.newFixedThreadPool()`
- Reuses threads instead of creating new ones for each client
- Bounded pool prevents resource exhaustion
- Result: Server can efficiently handle many concurrent clients

### Challenge 3: Blocking I/O Performance Bottleneck
**Problem:** Traditional blocking I/O operations slowed down message broadcasting to many clients.

**Solution (Member 3):**
- Implemented NIO-based broadcaster with `Selector` and non-blocking channels
- Single thread efficiently monitors multiple client channels
- Non-blocking operations prevent thread starvation
- Result: High-performance message distribution (though optional in final implementation)

### Challenge 4: External API Integration Reliability
**Problem:** External API calls could fail, timeout, or return unexpected data.

**Solution (Member 5):**
- Implemented proper timeout handling (`setConnectTimeout`, `setReadTimeout`)
- Added error handling and try-catch blocks
- Graceful degradation with error messages to users
- Simple JSON parsing to avoid dependency on external libraries
- Result: Robust API integration with proper error handling

### Challenge 5: Object Serialization Over Sockets
**Problem:** Need to send complex `Message` objects over TCP sockets efficiently.

**Solution:**
- Made `Message` class implement `Serializable`
- Used `ObjectInputStream` and `ObjectOutputStream` for easy object transmission
- Proper stream flushing to avoid buffering issues
- Result: Clean, type-safe message passing between client and server

---

## Conclusion

This project successfully demonstrates five core Java Network Programming concepts through a practical, real-world application. Each team member contributed a distinct networking component:

1. **TCP Connection Handling** - Foundation of reliable client-server communication
2. **Concurrent Processing** - Scalable multi-client support with thread pools
3. **High-Performance I/O** - NIO for efficient non-blocking operations
4. **Thread Safety** - Proper synchronization for data consistency
5. **HTTP Integration** - External API connectivity for extended functionality

### Learning Outcomes
- Deep understanding of TCP socket programming and client-server architecture
- Practical experience with Java concurrency and thread management
- Exposure to advanced I/O techniques with Java NIO
- Hands-on implementation of thread-safe data structures
- Integration of external services via HTTP protocols

### Future Enhancements
- WebSocket bridge for HTML frontend connectivity
- Persistent message storage (database)
- User authentication and authorization
- File transfer capability
- End-to-end encryption for secure communication
- Admin controls and moderation features
- Emoji and rich text support

### Technologies Used
- **Language:** Java 8+
- **Network APIs:** java.net.*, java.nio.*
- **Concurrency:** java.util.concurrent.*
- **Frontend:** HTML5, CSS3, JavaScript (demo)

---

## Project Structure
```
ChatSystem/
├── src/main/java/com/chatapp/
│   ├── server/
│   │   ├── ConnectionListener.java      (Member 1)
│   │   ├── ClientHandler.java           (Member 2)
│   │   ├── MessageBroadcaster.java      (Member 3)
│   │   ├── UserManager.java             (Member 4)
│   │   └── ChatServer.java              (Integration)
│   ├── client/
│   │   └── ChatClient.java              (Console client)
│   ├── common/
│   │   └── Message.java                 (Shared protocol)
│   └── api/
│       └── ExternalApiClient.java       (Member 5)
├── frontend/
│   ├── index.html
│   ├── style.css
│   └── app.js
└── README.md
```

---

## Contact and Support
For questions or issues, contact any group member.

**Assignment:** IN 3111 - Network Programming Assignment 2  
**Institution:** [Your University Name]  
**Submission Date:** 9-Nov-2025

---

## References
- Oracle Java Documentation: https://docs.oracle.com/en/java/
- Java Network Programming (O'Reilly)
- Java Concurrency in Practice
- wttr.in Weather API: https://wttr.in
- Official Joke API: https://official-joke-api.appspot.com
- Quotable API: https://github.com/lukePeavey/quotable
