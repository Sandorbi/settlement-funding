# Settlement Funding Selector

A Spring Boot service that selects settlement instructions to maximize total expected fee without exceeding an available settlement balance.

Instructions are funded in full or not selected. Each successful request stores the funding run and all its candidate instructions, including which were selected.

## Technology

- Java 21
- Spring Boot 4.1.1
- Maven Wrapper
- PostgreSQL 17
- Spring Data JPA
- Flyway
- JUnit, MockMvc, and Testcontainers

## Prerequisites

- JDK 21
- Docker with Docker Compose
- An available local port `5432` for PostgreSQL and `8080` for the application

Run the following commands from the project root.

## Database setup

Start PostgreSQL:

```bash
docker compose up -d
```

The default connection settings are:

| Setting | Value |
|---|---|
| Host | localhost |
| Port | 5432 |
| Database | settlement_funding |
| Username | settlement |
| Password | settlement |

Database data is stored in the `postgres_data` Docker volume and survives container restarts.

Stop the database with:

```bash
docker compose down
```

Flyway automatically applies migrations from `src/main/resources/db/migration` when the application starts. Hibernate validates the resulting schema; it does not create or update tables.

To connect to another database, set these environment variables:

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/settlement_funding` |
| `DB_USERNAME` | `settlement` |
| `DB_PASSWORD` | `settlement` |

## Build and run

### Windows PowerShell

Build the executable JAR and run the tests:

```powershell
.\mvnw.cmd clean verify
```

Start the application:

```powershell
java -jar target/settlement-funding-0.0.1-SNAPSHOT.jar
```

Alternatively, run directly through Maven:

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux and macOS

Build:

```bash
./mvnw clean verify
```

Start the application:

```bash
java -jar target/settlement-funding-0.0.1-SNAPSHOT.jar
```

Alternatively:

```bash
./mvnw spring-boot:run
```

The API is available at:

```text
http://localhost:8080
```

PostgreSQL must be running before starting the application.

## Tests

Run tests independently:

```powershell
.\mvnw.cmd test
```

On Linux or macOS:

```bash
./mvnw test
```

Integration tests use temporary PostgreSQL containers with the actual Flyway migrations.

## API

Request IDs are shown as `<requestId>` placeholders, and timestamps are illustrative. Replace `<requestId>` with the actual UUID returned by POST for subsequent requests.

### 1. Create a funding run

```http
POST /api/v1/settlement/fund
```

```bash
curl -i -X POST "http://localhost:8080/api/v1/settlement/fund" \
  -H "Content-Type: application/json" \
  --data '{
    "availableSettlementBalance": 20000,
    "candidateInstructions": [
      {
        "instructionReference": "INS-2001",
        "instructionAmount": 7000,
        "expectedFee": 150
      },
      {
        "instructionReference": "INS-2002",
        "instructionAmount": 9000,
        "expectedFee": 210
      },
      {
        "instructionReference": "INS-2003",
        "instructionAmount": 4000,
        "expectedFee": 90
      },
      {
        "instructionReference": "INS-2004",
        "instructionAmount": 6000,
        "expectedFee": 130
      }
    ]
  }'
```

Response: **201 Created**

```json
{
  "requestId": "<requestId>",
  "selectedInstructions": [
    {
      "instructionReference": "INS-2001",
      "instructionAmount": 7000,
      "expectedFee": 150
    },
    {
      "instructionReference": "INS-2002",
      "instructionAmount": 9000,
      "expectedFee": 210
    },
    {
      "instructionReference": "INS-2003",
      "instructionAmount": 4000,
      "expectedFee": 90
    }
  ],
  "totalSettlementConsumed": 20000,
  "totalExpectedFee": 450,
  "createdAt": "2026-09-29T11:13:31.980448Z"
}
```

If no instruction fits, the run is still persisted, but the endpoint returns **200 OK** with an empty selection and zero totals. An empty candidate list produces the same behavior.

For example:

```bash
curl -i -X POST "http://localhost:8080/api/v1/settlement/fund" \
  -H "Content-Type: application/json" \
  --data '{
    "availableSettlementBalance": 5,
    "candidateInstructions": [
      {
        "instructionReference": "INS-A",
        "instructionAmount": 6,
        "expectedFee": 2
      }
    ]
  }'
```

Response: **200 OK**

```json
{
  "requestId": "<requestId>",
  "selectedInstructions": [],
  "totalSettlementConsumed": 0,
  "totalExpectedFee": 0,
  "createdAt": "2026-09-29T11:15:24.468188Z"
}
```

### 2. Get a funding run

```http
GET /api/v1/settlement/{requestId}
```

Replace `<requestId>` below with the ID returned by POST:

```bash
curl -i "http://localhost:8080/api/v1/settlement/<requestId>"
```

Response: **200 OK**

```json
{
  "requestId": "<requestId>",
  "selectedInstructions": [
    {
      "instructionReference": "INS-2001",
      "instructionAmount": 7000.0000,
      "expectedFee": 150.0000
    },
    {
      "instructionReference": "INS-2002",
      "instructionAmount": 9000.0000,
      "expectedFee": 210.0000
    },
    {
      "instructionReference": "INS-2003",
      "instructionAmount": 4000.0000,
      "expectedFee": 90.0000
    }
  ],
  "totalSettlementConsumed": 20000.0000,
  "totalExpectedFee": 450.0000,
  "createdAt": "2026-09-29T11:13:31.980448Z"
}
```

An unknown UUID returns **404 Not Found**. An invalid UUID format returns **400 Bad Request**.

### 3. Get funding history

```http
GET /api/v1/settlement?page=0&size=20
```

```bash
curl -i "http://localhost:8080/api/v1/settlement?page=0&size=1"
```

Assuming the two example POST requests above were submitted to an empty database, the response is **200 OK**:

```json
{
  "content": [
    {
      "requestId": "<requestId>",
      "selectedInstructions": [],
      "totalSettlementConsumed": 0.0000,
      "totalExpectedFee": 0.0000,
      "createdAt": "2026-09-29T11:15:24.468188Z"
    }
  ],
  "page": 0,
  "size": 1,
  "totalElements": 2,
  "totalPages": 2
}
```

Each entry contains the full funding result, including selected instructions.

| Parameter | Default | Rules |
|---|---|---|
| `page` | 0 | Zero-based, non-negative integer |
| `size` | 20 | Integer between 1 and 100 |

Runs are ordered by `createdAt DESC`, then `id DESC` to make ordering deterministic when timestamps match.

A page beyond the available results returns an empty `content` list with **200 OK**. Pagination combinations whose offset (`page × size`) exceeds `2147483647` return **400 Bad Request**.

## Input validation

- `availableSettlementBalance` is required and must be non-negative.
- `candidateInstructions` is required and may be empty; its elements cannot be null.
- `instructionReference` must be non-blank and at most 50 characters.
- `instructionAmount` is required and must be greater than zero.
- `expectedFee` is required and must be non-negative.
- Monetary inputs support up to 16 integer digits and 4 decimal places.
- The selected total fee must not exceed `9999999999999999.9999`.

Invalid input returns **400 Bad Request** with a descriptive problem response. For example:

```json
{
  "detail": "One or more request fields are invalid.",
  "instance": "/api/v1/settlement/fund",
  "status": 400,
  "title": "Invalid funding request",
  "errors": [
    {
      "field": "availableSettlementBalance",
      "message": "must be greater than or equal to 0"
    }
  ]
}
```

## Selection algorithm

The application uses sparse dynamic programming to solve the 0/1 knapsack problem.

It tracks combinations by consumed balance and keeps the highest fee for each amount. After processing an instruction, it removes combinations that consume at least as much balance as another combination without earning a higher fee.

Each instruction is considered once per combination. `BigDecimal` provides exact decimal arithmetic. When multiple combinations earn the same maximum fee, the algorithm prefers lower total balance consumption.

The algorithm finds an exact optimum, but the number of retained combinations can grow exponentially in the worst case. The current implementation has no candidate-count or state-count limit, so processing time and memory usage depend on the input values as well as the number of candidates.

## Database schema

### `settlement_runs`

Stores one funding decision:

- `id`: UUID primary key and public request ID.
- `available_settlement_balance`: submitted balance.
- `total_settlement_consumed`: selected instructions' total amount.
- `total_expected_fee`: selected instructions' total fee.
- `created_at`: creation timestamp, stored as `TIMESTAMPTZ`.

### `settlement_instructions`

Stores all candidates submitted for a run:

- `id`: generated BIGINT primary key.
- `run_id`: foreign key referencing `settlement_runs`.
- `instruction_reference`: submitted human-readable label.
- `instruction_amount`: balance consumed by the instruction.
- `expected_fee`: fee earned if selected.
- `selected`: whether the instruction was selected for that run.

The relationship is one run to many instructions. Each run owns its candidate snapshots, allowing past decisions to be audited independently. References are not required to be unique.

### Indexes

- Primary keys automatically provide indexes for lookup by ID.
- `(created_at DESC, id DESC)` supports the history endpoint's ordering. The ID provides a deterministic tie-breaker for equal timestamps.
- `settlement_instructions(run_id)` supports fetching instructions belonging to one run or a page of runs.

History retrieves selected instructions for the page in a bulk query, avoiding a separate instruction query for every run.
