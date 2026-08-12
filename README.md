# Payment Gateway Simulator

A production-inspired payment gateway backend implementing core fintech
engineering patterns: idempotency, state machine, webhook reliability,
and fraud detection.

**Live API:** https://payment-gateway-simulator.onrender.com/api/v1/health

---

## Why I built this

Payment systems are the hardest backend engineering problem in fintech.
Every company in this space — Razorpay, Juspay, PhonePe — solves the
same core challenges: how do you prevent double charges? How do you
guarantee a merchant gets notified even if their server is down?
How do you detect fraud without blocking legitimate users?

This project implements those exact solutions from scratch.

---

## Architecture decisions and why

**Amounts stored in paise, not rupees**  
All amounts are integers (paise). ₹500 = 50000. Floating point math
is unreliable for money — 0.1 + 0.2 = 0.30000000000000004 in Java.
Integer arithmetic is always exact. This is the industry standard —
Razorpay, Stripe, and PayPal all use minor currency units.

**Two-layer idempotency**  
Idempotency is implemented at two layers: Redis (fast path, O(1) lookup)
and a database unique constraint on idempotency_key (safe fallback if
Redis is unavailable). If a merchant's server crashes after sending a
payment request and they retry, they get back the identical response
without creating a duplicate charge. Same pattern used by Razorpay.

**Append-only payment events table**  
Every state transition is recorded in payment_events — never updated,
only inserted. This creates an immutable audit trail (RBI mandates this
for payment companies). If something goes wrong, you can reconstruct
exactly what happened and when.

**Payment state machine with validated transitions**  
PaymentStatus enum contains canTransitionTo() logic. A SUCCESS payment
cannot be moved to FAILED. A CREATED payment cannot jump to SUCCESS.
Invalid transitions throw exceptions. This prevents data corruption
from bugs or race conditions.

**Async webhook delivery with exponential backoff**  
Webhooks run in a separate thread (@Async) so the payment API response
is never blocked by the merchant's server latency. Failed deliveries
retry at 1 min → 5 min → 30 min → 2 hours. A scheduler runs every
60 seconds picking up pending retries.

**Redis-based fraud detection**  
If the same UPI ID fails 3 payments within 60 seconds, it's blocked
for 15 minutes. Uses Redis atomic INCR with TTL — safe for concurrent
requests across multiple threads.

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 4.1.0  |
| Database | PostgreSQL 15 |
| Cache / Locks | Redis 7 |
| ORM | Spring Data JPA + Hibernate |
| Auth | API Key (header-based) |
| Deployment | Docker + Render |

---

## How to run locally

**Prerequisites:** Java 17, Docker Desktop

```bash
# Clone the repo
git clone https://github.com/Vipinkushuk/payment-gateway-simulator.git
cd payment-gateway-simulator

# Start PostgreSQL and Redis
docker-compose up -d

# Run the app
./mvnw spring-boot:run
```

App starts at `http://localhost:8080`

---

## API endpoints

All endpoints except `/register` and `/health` require:

### Authentication

All protected endpoints require:

`X-Api-Key: your_api_key`

Payment endpoints additionally require:

`X-Idempotency-Key: unique-uuid-per-request`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/merchants/register | Register merchant, get API key |
| GET | /api/v1/health | Health check |
| POST | /api/v1/orders | Create an order |
| GET | /api/v1/orders/{id} | Get order by ID |
| POST | /api/v1/payments | Initiate payment |
| GET | /api/v1/payments/{id} | Get payment status |
| GET | /api/v1/payments/{id}/webhooks | Get webhook delivery history |

**[Postman Collection →](https://paradise-kings-team.postman.co/workspace/chat-app~f586ff85-c332-48ec-a313-12ec5edbd789/collection/41613639-e836fa9e-0208-4c8f-8f77-bd1eb78fa31c?action=share&source=copy-link&creator=41613639)**

---

## Key flows to test

**1. Register a merchant**
```bash
POST /api/v1/merchants/register
{
  "name": "Test Merchant",
  "email": "test@merchant.com",
  "webhookUrl": "https://webhook.site/your-id"
}
# Save the apiKey from response
```

**2. Create an order**
```bash
POST /api/v1/orders
Header: X-Api-Key: your_key
{
  "amount": 50000,
  "receipt": "order_001"
}
# Save the orderId
```

**3. Pay the order (idempotency test)**
```bash
POST /api/v1/payments
Header: X-Api-Key: your_key
Header: X-Idempotency-Key: test-key-001
{
  "orderId": "paste_order_id",
  "method": "UPI",
  "upiId": "test@paytm"
}
# Send same request again with same key → identical response returned
```

**4. Fraud detection test**
```bash
# Send 3 failed payments with same upiId (new order + new key each time)
# 4th attempt returns 429 Too Many Requests
```

---

## Database Schema

- `merchants` → API key authentication
- `orders` → order lifecycle (CREATED → PAID → EXPIRED)
- `payments` → payment attempts with idempotency key
- `payment_events` → immutable audit trail of every state change
- `webhook_deliveries` → delivery attempts with retry tracking

---

## What's different from a real payment gateway

A production gateway connects to NPCI/banks via ISO 8583 messaging,
requires RBI PA license, PCI-DSS compliance, and HSM for card encryption.
This project simulates the bank with a mock service (70% success,
20% fail, 10% timeout) and implements all the surrounding engineering
patterns identically.