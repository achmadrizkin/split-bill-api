# Split Bill API

A Spring Boot REST API for tracking shared expenses in a group and working out who owes whom. Money handling follows banking-grade practices.

> The original challenge brief is archived in [`docs/CHALLENGE.md`](docs/CHALLENGE.md).

| | |
|---|---|
| **GitHub username** | `achmadrizkin` |
| **Unicode sum** | `a97 + c99 + h104 + m109 + a97 + d100 + r114 + i105 + z122 + k107 + i105 + n110 = 1269` |
| **`service_charge_pct`** | `1269 % 10 = ` **9** (9%) |

The percentage is computed at runtime by `ServiceChargePolicy` from the `app.github-username` setting (env `GITHUB_USERNAME`). It is not hardcoded anywhere.

---

## Build & run

**Docker (recommended; needs nothing but Docker):**

```bash
docker build -t split-bill-api .                           # also runs the full test suite
docker build --build-arg SKIP_TESTS=true -t split-bill-api .   # faster, skips tests
docker run --rm -p 4110:4110 split-bill-api
```

**Locally (JDK 21 + Maven 3.9):**

```bash
mvn test                    # unit + property-based + integration + architecture tests
mvn verify                  # + JaCoCo coverage report in target/site/jacoco/index.html
mvn spring-boot:run         # http://localhost:4110
```

- Swagger UI: http://localhost:4110/swagger-ui.html
- Health: http://localhost:4110/actuator/health
- Storage: in-memory H2 in PostgreSQL mode, with the schema managed by Flyway. Data is reset on restart.

---

## API

Base path `/api/v1`. All JSON is `snake_case`. **Every monetary amount is a decimal string** (`"1500000.00"`). Amounts in requests may be sent as a string or a number. Errors use [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) and carry a stable `error_code`.

| Method | Path | Purpose |
|---|---|---|
| POST | `/groups` | Create group with participants |
| GET | `/groups/{groupId}` | Get group |
| POST | `/groups/{groupId}/participants` | Add participant |
| POST | `/groups/{groupId}/expenses` | Add expense (supports `Idempotency-Key`) |
| GET | `/groups/{groupId}/expenses[?category=FOOD]` | List expenses |
| GET | `/groups/{groupId}/expenses/{expenseId}` | Get expense with its shares |
| POST | `/groups/{groupId}/expenses/{expenseId}/void` | Void an expense (never deleted) |
| POST | `/groups/{groupId}/payments` | Record a settle-up payment (supports `Idempotency-Key`) |
| GET | `/groups/{groupId}/payments` | List payments |
| GET | `/groups/{groupId}/balances` | Net balance per participant |
| GET | `/groups/{groupId}/settlement` | **Settlement summary** |
| GET | `/groups/{groupId}/audit-events` | Audit trail |

### Walkthrough with curl: "Bali Trip"

```bash
BASE=http://localhost:4110/api/v1

# 1. Create a group (currency defaults to IDR)
curl -s -X POST $BASE/groups -H 'Content-Type: application/json' -d '{
  "name": "Bali Trip", "currency": "IDR", "participants": ["Andi", "Budi", "Citra"]
}'
# -> copy "id" and the participant ids from the response
GROUP=<group id>; ANDI=<id>; BUDI=<id>; CITRA=<id>

# 2a. Equal split among everyone (split_type defaults to EQUAL, splits default to the whole group)
curl -s -X POST $BASE/groups/$GROUP/expenses -H 'Content-Type: application/json' \
  -H "Idempotency-Key: bali-hotel-001" -d "{
  \"description\": \"Hotel\", \"category\": \"ACCOMMODATION\", \"amount\": \"1500000\", \"paid_by\": \"$ANDI\"
}"

# 2b. Equal split, explicit
curl -s -X POST $BASE/groups/$GROUP/expenses -H 'Content-Type: application/json' -d "{
  \"description\": \"Seafood dinner\", \"category\": \"FOOD\", \"amount\": \"300000\", \"paid_by\": \"$BUDI\",
  \"split_type\": \"EQUAL\"
}"

# 2c. Exact amounts (must add up to the total)
curl -s -X POST $BASE/groups/$GROUP/expenses -H 'Content-Type: application/json' -d "{
  \"description\": \"Taxi\", \"category\": \"TRANSPORT\", \"amount\": \"100000\", \"paid_by\": \"$CITRA\",
  \"split_type\": \"EXACT\",
  \"splits\": [{\"participant_id\": \"$ANDI\", \"value\": \"50000\"}, {\"participant_id\": \"$BUDI\", \"value\": \"50000\"}]
}"

# 2d. Percentage (must add up to 100) or SHARES (whole-number weights such as 2:1:1)
curl -s -X POST $BASE/groups/$GROUP/expenses -H 'Content-Type: application/json' -d "{
  \"description\": \"Snorkeling\", \"category\": \"ENTERTAINMENT\", \"amount\": \"200000\", \"paid_by\": \"$ANDI\",
  \"split_type\": \"PERCENTAGE\",
  \"splits\": [{\"participant_id\": \"$ANDI\", \"value\": 50}, {\"participant_id\": \"$CITRA\", \"value\": 50}]
}"

# 3. Settlement summary
curl -s $BASE/groups/$GROUP/settlement

# 4. Record that Budi paid Andi back
curl -s -X POST $BASE/groups/$GROUP/payments -H 'Content-Type: application/json' -d "{
  \"from_participant_id\": \"$BUDI\", \"to_participant_id\": \"$ANDI\", \"amount\": \"350000\", \"note\": \"BCA transfer\"
}"

# Other endpoints
curl -s $BASE/groups/$GROUP
curl -s -X POST $BASE/groups/$GROUP/participants -H 'Content-Type: application/json' -d '{"name": "Dewi"}'
curl -s "$BASE/groups/$GROUP/expenses?category=FOOD"
curl -s $BASE/groups/$GROUP/expenses/<expense id>
curl -s -X POST $BASE/groups/$GROUP/expenses/<expense id>/void -H 'Content-Type: application/json' -d '{"reason": "Entered twice"}'
curl -s $BASE/groups/$GROUP/payments
curl -s $BASE/groups/$GROUP/balances
curl -s $BASE/groups/$GROUP/audit-events
```

Settlement for steps 1–2c (ids shortened):

```json
{
  "group_name": "Bali Trip",
  "currency": "IDR",
  "total_expenses": "1900000.00",
  "total_payments": "0.00",
  "service_charge_pct": 9,
  "service_charge_amount": "171000.00",
  "service_charge_breakdown": [
    {"name": "Andi", "amount": "58500.00"}, {"name": "Budi", "amount": "58500.00"}, {"name": "Citra", "amount": "54000.00"}
  ],
  "balances": [
    {"name": "Andi",  "paid": "1500000.00", "share": "650000.00", "net": "850000.00"},
    {"name": "Budi",  "paid": "300000.00",  "share": "650000.00", "net": "-350000.00"},
    {"name": "Citra", "paid": "100000.00",  "share": "600000.00", "net": "-500000.00"}
  ],
  "transfers": [
    {"from_name": "Citra", "to_name": "Andi", "amount": "500000.00"},
    {"from_name": "Budi",  "to_name": "Andi", "amount": "350000.00"}
  ],
  "transaction_count": 2,
  "settled": false,
  "category_summary": [
    {"category": "ACCOMMODATION", "total": "1500000.00", "expense_count": 1},
    {"category": "FOOD", "total": "300000.00", "expense_count": 1},
    {"category": "TRANSPORT", "total": "100000.00", "expense_count": 1}
  ]
}
```

Example error:

```json
{
  "type": "about:blank", "title": "Unprocessable Entity", "status": 422,
  "detail": "Exact split amounts add up to 60000.00 IDR but the expense total is 100000.00 IDR",
  "instance": "/api/v1/groups/…/expenses",
  "error_code": "SPLIT_SUM_MISMATCH"
}
```

---

## Design highlights

- **Money:** values are `BigDecimal` wrapped in a `Money` value object (amount + ISO 4217 currency) and scaled to the currency's minor unit.
  - Input with too many decimals (`10.005` IDR) is **rejected**, never silently rounded.
  - Derived values such as the service charge use banker's rounding (`HALF_EVEN`).
- **No lost cents:** `MoneyAllocator` uses the largest-remainder method. Splitting 100,000.00 three ways gives 33,333.34 + 33,333.33 + 33,333.33, and the shares always add back up to the total exactly. Property-based tests (jqwik) check this on hundreds of random inputs.
- **Split strategies:** `EQUAL`, `EXACT`, `PERCENTAGE` and `SHARES`, implemented as a `sealed interface` with one record per strategy (Strategy pattern).
- **Settlement optimization:** a greedy min-cash-flow algorithm (largest debtor pays largest creditor).
  - Needs at most n−1 transfers, runs in O(n log n), and is deterministic.
- **Ledger, not stored balances:** balances are always derived from expenses and payments, as in core banking.
  - Expenses are immutable and can only be **voided**.
  - Every change is written to an append-only **audit trail** in the same transaction.
- **Safe retries:** the `Idempotency-Key` header on POST expense/payment means a client retrying after a timeout never double-books money. Reusing a key with a different payload returns 409.
- **Concurrency:** writes take an `OPTIMISTIC_FORCE_INCREMENT` lock on the group. Two concurrent payments cannot both pass the "not more than owed" check; the loser gets a `409 CONCURRENT_MODIFICATION`.
- **Payments can only reduce debt:** the payer must owe at least the amount and the payee must be owed at least the amount. A typo (an extra zero) cannot silently flip who owes whom.
- **Layering:**
  - `domain` is pure Java, with no Spring or JPA.
  - `application` holds the use cases and transactions.
  - `infrastructure` holds JPA and config.
  - `api` holds REST, DTOs and errors.
  - These rules are enforced by ArchUnit tests.

### Tests

| Layer | What |
|---|---|
| Unit (domain) | `Money`, `MoneyAllocator`, each split strategy, `BalanceCalculator`, `SettlementOptimizer`, `ServiceChargePolicy` (incl. the README example `johndoe47 → 0`) |
| Property-based | Allocation never creates or loses money; balances always net to zero; settlement clears every balance in ≤ n−1 transfers |
| Integration | Full HTTP → DB flows: Bali Trip settlement, payments, idempotent retry, voiding, validation and error format, audit trail |
| Architecture | Domain is framework-free; API never touches persistence |

---

## Submission question

> **What was the hardest design decision you made while building this, and what trade-off did you accept?**

<!-- TODO(achmadrizkin): rewrite this answer in your own words before submitting. -->
The hardest decision was where rounding is allowed to happen. Splitting money almost never divides evenly, and every place that rounds is a place where a cent can quietly disappear. I chose to reject over-precise input outright and to store each participant's share as a fixed amount when the expense is recorded. The shares are allocated with the largest-remainder method, so they always add up to the total exactly. The trade-off is that some cents are assigned by position: the first person in the list may carry 0.01 more. I also store a fixed allocation rather than recomputing it, so changing the participant list does not re-split old expenses. A second trade-off is that settlement uses a greedy algorithm, not the true minimum number of transfers (which is NP-hard). I accepted being "near-optimal and explainable" over "optimal and exponential".

---

## Known limitations / next steps

- No authentication or authorization. Any caller can read or modify any group if they know its UUID.
- One currency per group, with no FX conversion.
- H2 in-memory storage. For production, switch the datasource to PostgreSQL; the schema is already compatible.
- Payments cannot be voided yet. This would mirror expense voiding.
- Idempotency records are never expired. Production would add a TTL (e.g. 24h) cleanup job.#   s p l i t - b i l l - a p i  
 