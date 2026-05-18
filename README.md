# Payment Gateway Challenge

This is a Spring Boot payment gateway service. It accepts merchant payment requests, validates the request, forwards valid payments to a bank simulator for authorization, and stores the payment result in an in-memory repository so it can be retrieved later by payment ID.

## Tech Stack

- Java 17
- Spring Boot 3.1.5
- Gradle
- Spring Web / RestTemplate
- springdoc-openapi
- JUnit 5 / Spring Boot Test
- Docker / Docker Compose
- Mountebank bank simulator

## Project Structure

```text
src/main/java/com/checkout/payment/gateway
├── client          # Bank service client
├── configuration   # Spring bean configuration
├── controller      # HTTP API controller
├── enums           # Currency and payment status enums
├── exception       # Global exception handling
├── model           # Request and response models
├── repository      # In-memory payment repository
├── service         # Payment processing business logic
└── util            # Card utility methods

src/test/java       # API integration tests
imposters           # Mountebank bank simulator configuration
```

## Features

- Create a payment with `POST /payment`
- Retrieve a payment with `GET /payment/{id}`
- Basic payment request validation
- Bank simulator integration
- In-memory payment result storage
- Returns only the last four card digits, never the full card number or CVV
- OpenAPI/Swagger UI documentation

## Requirements

- JDK 17
- Docker and Docker Compose

## Running Locally

Start the bank simulator first:

```bash
docker compose up bank_simulator
```

Then start the payment gateway:

```bash
./gradlew bootRun
```

The payment gateway runs on:

```text
http://localhost:8090
```

The bank simulator runs on:

```text
http://localhost:8080
```

## Running With Docker

Start both the payment gateway and the bank simulator:

```bash
docker compose up --build
```

The Docker Compose setup configures the payment gateway with:

```text
BANK_PAYMENTS_URL=http://bank_simulator:8080/payments
```

## Configuration

Default configuration is defined in `src/main/resources/application.properties`:

```properties
server.port=8090
springdoc.swagger-ui.enabled=true
springdoc.api-docs.enabled=true
bank.payments-url=http://localhost:8080/payments
```

Spring Boot properties can be overridden with environment variables. Example:

```bash
BANK_PAYMENTS_URL=http://localhost:8080/payments ./gradlew bootRun
```

## API Documentation

After starting the service, Swagger UI is available at:

```text
http://localhost:8090/swagger-ui/index.html
```

OpenAPI JSON is available at:

```text
http://localhost:8090/v3/api-docs
```

## Create Payment

```http
POST /payment
Content-Type: application/json
```

Request example:

```json
{
  "card_number": "2222405343248877",
  "expiry_month": 12,
  "expiry_year": 2030,
  "currency": "GBP",
  "amount": 100,
  "cvv": "123"
}
```

Successful response:

```http
HTTP/1.1 201 Created
```

```json
{
  "id": "f16cb520-62d9-4f4c-86ab-d603eca935c0",
  "status": "Authorized",
  "cardNumberLastFour": "8877",
  "expiryMonth": 12,
  "expiryYear": 2030,
  "currency": "GBP",
  "amount": 100
}
```

### Request Fields

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `card_number` | string | Yes | Card number with 14 to 19 digits |
| `expiry_month` | number | Yes | Expiry month, from 1 to 12 |
| `expiry_year` | number | Yes | Expiry year |
| `currency` | string | Yes | Supported values: `GBP`, `USD`, `EUR` |
| `amount` | number | Yes | Payment amount in the minor unit of the currency, must be greater than 0. For example, `100` means GBP 1.00. |
| `cvv` | string | Yes | 3 to 4 digits |

### Payment Statuses

| Status | Meaning |
| --- | --- |
| `Authorized` | The bank authorized the payment |
| `Declined` | The bank declined the payment |
| `Rejected` | The gateway rejected the request, or the bank call failed |

## Retrieve Payment

```http
GET /payment/{id}
```

Response example:

```json
{
  "id": "f16cb520-62d9-4f4c-86ab-d603eca935c0",
  "status": "Authorized",
  "cardNumberLastFour": "8877",
  "expiryMonth": 12,
  "expiryYear": 2030,
  "currency": "GBP",
  "amount": 100
}
```

If the payment ID does not exist:

```http
HTTP/1.1 404 Not Found
```

```json
{
  "message": "Page not found"
}
```

If the payment ID is not a valid UUID:

```http
HTTP/1.1 400 Bad Request
```

```json
{
  "message": "Invalid request parameter"
}
```

## Bank Simulator Behavior

The bank simulator configuration is located in `imposters/bank_simulator.ejs`.

| Card number ending | Simulator behavior | Gateway status |
| --- | --- | --- |
| `1`, `3`, `5`, `7`, `9` | Returns `authorized: true` | `Authorized` |
| `2`, `4`, `6`, `8` | Returns `authorized: false` | `Declined` |
| `0` | Returns `503` | `Rejected` |

## Error Handling

| Scenario | HTTP status | Response message |
| --- | --- | --- |
| Payment ID does not exist | `404` | `Page not found` |
| Path parameter is not a UUID | `400` | `Invalid request parameter` |
| Malformed JSON request body | `400` | `Invalid request body` |
| Unexpected error | `500` | `Internal server error` |

When a payment request contains invalid fields, the API still returns `201 Created`, but the saved payment status is `Rejected`.

Amounts are represented as integer minor currency units throughout the gateway and bank simulator integration. This avoids floating-point precision issues when handling money.

## Testing

Run the test suite:

```bash
./gradlew test
```

The current tests cover:

- Retrieving an existing payment by ID
- Error responses for unknown or invalid IDs
- Malformed request bodies
- Invalid payment requests being rejected
- Mapping authorized and declined bank responses
- Mapping bank client failures to rejected payments
- Preserving leading zeros in card last-four responses
- Retrieving a payment after it has been created

## Key Design Considerations and Assumptions

- Amounts are accepted and returned in the minor unit of the currency. The gateway does not convert from major units such as `10.50` to `1050`; callers are expected to send `1050` for USD 10.50.
- `amount` is represented as a `long` to avoid floating-point precision issues and to support values larger than the `int` range.
- Full card numbers and CVVs are not returned in API responses. The gateway only returns `cardNumberLastFour`.
- `cardNumberLastFour` is represented as a string so leading zeros are preserved, for example `"0042"`.
- Payment records are stored in an in-memory `ConcurrentHashMap`. This keeps the take-home implementation simple, but records are lost when the application restarts.
- Invalid payment details, such as unsupported currency, invalid card number, expired card, or non-positive amount, create a payment record with `Rejected` status. Malformed JSON and invalid path parameters are treated as request errors and return `400`.
- Bank simulator failures are mapped to `Rejected`. This includes unavailable bank responses or client exceptions when calling the bank.
- The payment gateway generates a UUID for each payment. Idempotency keys are not implemented, so repeated submissions create separate payment records.
- Supported currencies are limited to `GBP`, `USD`, and `EUR`. The service assumes the request amount is already in the correct minor unit for the supplied currency.
- Authentication, authorization, PCI-grade tokenization, persistent storage, observability, and production deployment concerns are outside the scope of this offline exercise.

## Design Notes

Payment processing flow:

1. `PaymentGatewayController` receives the HTTP request.
2. `PaymentGatewayService` creates a payment response object and validates the request.
3. If validation fails, the payment is saved with `Rejected` status.
4. If validation succeeds, `BankClient` forwards the request to the bank simulator.
5. A successful bank authorization is mapped to `Authorized`; a bank decline is mapped to `Declined`; a bank call failure is mapped to `Rejected`.
6. The payment result is saved in the in-memory repository and can be retrieved by UUID.

Note: `PaymentsRepository` stores payments in an in-memory `ConcurrentHashMap`. Payment records are lost when the application restarts. A production implementation should replace this with persistent storage.
