# Weather Data Aggregator: Implementation & Testing Strategy

## Implementation Phases

### Phase 1: Basic Setup & Infrastructure

#### Aggregation Server

- [x] Basic `AggregationServer.java` with minimal functionality
- [ ] File I/O operations for JSON data
- [ ] Server startup on a specified port
- [ ] Handling HTTP status codes 201 and 200
- [ ] **Tests**: Unit tests for each functionality

#### GET Client

- [ ] Basic `GETClient.java` with minimal functionality
- [ ] Read command-line parameters for server and port
- [ ] Implement GET request functionality
- [ ] **Tests**: Unit tests for basic GET operations

#### Content Server

- [ ] Basic `ContentServer.java` with minimal functionality
- [ ] Read command-line parameters for server and file location
- [ ] File reading and text parsing
- [ ] **Tests**: Unit tests for individual features

### Phase 2: Communication & Basic Operations

#### Basic Communication

- [ ] Communication between Aggregation Server and GET Client
- [ ] Communication between Aggregation Server and Content Server
- [ ] **Tests**: Integration tests for communication

#### PUT Operations

- [ ] Implement PUT operations in Content Server
- [ ] PUT request handling in Aggregation Server
- [ ] **Tests**: Unit and Integration tests for PUT operations

#### GET Operations

- [ ] Advanced GET operations in GET Client
- [ ] **Tests**: Unit and Integration tests for GET operations

### Phase 3: Advanced Features & Error Handling

#### Lamport Clocks

- [ ] Implementation in all components
- [ ] **Tests**: Unit and Integration tests for synchronization

#### Error Handling

- [ ] HTTP status codes 400, 500, and 204 in Aggregation Server
- [ ] Retry mechanisms in both GET and Content Servers
- [ ] **Tests**: Unit tests for error codes and retry mechanisms

#### Data Expiry

- [ ] 30-second data expiry in Aggregation Server
- [ ] **Tests**: Unit and Integration tests for data expiry

### Phase 4: Optimisation & Cleanup

- [ ] Code quality and comments
- [ ] Comprehensive and integration testing
- [ ] Optional: Implement bonus features like custom JSON parsing

## Component Methods

### GET Client

- [ ] `getClient()`
- [ ] `ReadInputFile()`
- [x] `SendPUTRequest()`
- [ ] `SynchroniseLamportClock()`
- [ ] `ConfirmReceipt()`

### Aggregation Server

- [x] `startServer(int port)`
- [x] `handleGetRequest(Client client): Response`
- [x] `handlePutRequest(ContentServer contentServer)`
- [ ] `removeStaleData()`
- [ ] `SynchroniseLamportClock()`

### Content Servers

- [ ] `ReadData()`
- [ ] `ConvertToJSON()`
- [ ] `SendPUTRequest()`
- [ ] `SynchroniseLamportClock()`
- [ ] `ConfirmReceipt()`

## Code Quality Checklist (Appendix A)

### Do

- [ ] Write comments above methods
- [ ] Describe special cases
- [ ] Maintain modular code

### Don't

- [ ] Use magic numbers
- [ ] Use comments as structural elements
- [ ] Leave spelling errors or TODO blocks

## Functionality Checklist (Appendix B)

### Basic Functionality

- [ ] Text sending and receiving
- [ ] Startup and initial communication
- [ ] PUT and GET operations
- [ ] Data expiry within 30 seconds
- [ ] Error retry mechanism

### Full Functionality

- [ ] Lamport clock synchronization
- [ ] Implement all error codes
- [ ] Server replication for fault-tolerance

### Bonus Functionality

- [ ] Manual JSON parsing for extra points

## Testing

- [ ] Unit tests for individual components and methods
- [ ] Integration tests for interactions and parallel requests
- [ ] Tests for server crash and recovery, network failure, and incorrect JSON