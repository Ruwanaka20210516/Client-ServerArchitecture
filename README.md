# Smart Campus Sensor & Room Management API

A JAX-RS (Jersey + Grizzly) RESTful service that manages **Rooms**, **Sensors** and their historical **SensorReadings** for the University of Westminster "Smart Campus" initiative.

- Module: `5COSC022W` – Client-Server Architectures
- Base URI: `http://localhost:8080/api/v1`
- Storage: in-memory `ConcurrentHashMap` only (no database).
- Java 11, Maven 3.8+, Jersey 2.35, Grizzly 2 embedded HTTP server.

---

## 1. API Overview

The API follows a resource-oriented design that reflects the physical structure of the campus:

```
/api/v1                                    -> Discovery document (HATEOAS links)
/api/v1/rooms                              -> Collection of rooms (GET, POST)
/api/v1/rooms/{roomId}                     -> Individual room (GET, DELETE)
/api/v1/sensors                            -> Sensor collection with ?type= filter (GET, POST)
/api/v1/sensors/{sensorId}                 -> Individual sensor (GET)
/api/v1/sensors/{sensorId}/readings        -> Sub-resource: reading history (GET, POST)
```

Key behaviours required by the brief:

- Rooms cannot be deleted while they still own sensors (`409 Conflict`).
- Creating a sensor with a non-existent `roomId` returns `422 Unprocessable Entity`.
- Adding a reading to a `MAINTENANCE` or `OFFLINE` sensor returns `403 Forbidden`.
- Every successful `POST` to a sensor reading updates the parent sensor's `currentValue`.
- All uncaught errors are captured by a generic `ExceptionMapper<Throwable>` and returned as a safe `500` JSON body — stack traces never leak to clients.
- Every request and response is logged by a `ContainerRequestFilter` + `ContainerResponseFilter` pair.

### Project layout

```
src/main/java/com/smartcampus/
  app/         Main (Grizzly bootstrap) + RestApplication (@ApplicationPath)
  model/       Room, Sensor, SensorReading POJOs
  store/       DataStore – thread-safe in-memory persistence (singleton)
  resource/    DiscoveryResource, SensorRoom, SensorResource, SensorReadingResource
  exception/   RoomNotEmptyException, LinkedResourceNotFoundException, SensorUnavailableException
  mapper/      ExceptionMappers (409, 422, 403, 404, 500) + ErrorPayload helper
  filter/      LoggingFilter (request + response logging)
postman/
  SmartCampus.postman_collection.json   Importable Postman collection (16 requests)
```

---

## 2. Build & Run

### Prerequisites
- JDK **11 or later** on `PATH`
- Maven **3.8+**

### Build
```bash
mvn clean package
```

This produces a shaded, runnable JAR at `target/smart-campus-api.jar`.

### Run the server
```bash
java -jar target/smart-campus-api.jar
```

You should see:

```
INFO: Smart Campus API started at http://localhost:8080/api/v1 - press CTRL+C to stop.
```

(Alternative: `mvn exec:java -Dexec.mainClass=com.smartcampus.app.Main`.)

### Stop the server
`CTRL+C` in the terminal — a shutdown hook cleanly stops Grizzly.

---

## 3. Sample `curl` Commands

The server ships with seed data (2 rooms, 3 sensors). Use these examples to exercise every part of the API.

### 3.1 Discovery document
```bash
curl -i http://localhost:8080/api/v1
```

### 3.2 List rooms
```bash
curl -i http://localhost:8080/api/v1/rooms
```

### 3.3 Create a new room
```bash
curl -i -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"SCI-210","name":"Science Building Lab 210","capacity":25}'
```

### 3.4 Attempt to delete a non-empty room (expect `409 Conflict`)
```bash
curl -i -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 3.5 Register a new sensor attached to an existing room
```bash
curl -i -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type":"Temperature","status":"ACTIVE","currentValue":21.5,"roomId":"SCI-210"}'
```

### 3.6 Register a sensor against a missing room (expect `422 Unprocessable Entity`)
```bash
curl -i -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type":"CO2","status":"ACTIVE","currentValue":400,"roomId":"GHOST-999"}'
```

### 3.7 Filter sensors by type (query parameter)
```bash
curl -i "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 3.8 Append a reading — updates the sensor's `currentValue`
```bash
curl -i -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.1}'
```

### 3.9 Fetch reading history for a sensor
```bash
curl -i http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

### 3.10 Post a reading to a MAINTENANCE sensor (expect `403 Forbidden`)
```bash
curl -i -X POST http://localhost:8080/api/v1/sensors/OCC-002/readings \
  -H "Content-Type: application/json" \
  -d '{"value":1}'
```

---

## 4. Postman Collection

A ready-to-import Postman collection is included at:

```
postman/SmartCampus.postman_collection.json
```

It contains **16 requests** organised into five folders that mirror the five graded parts of the coursework. Import it by opening Postman → **Import** → drag the JSON file in. The base URL is parameterised via the `{{baseUrl}}` collection variable (default `http://localhost:8080/api/v1`).

Run the requests in numbered order — some requests depend on earlier state (e.g. `DELETE /rooms/SCI-210` only works if you ran the matching `POST /rooms` first).

| #  | Folder                            | Request                                                  | Expected status |
|----|-----------------------------------|----------------------------------------------------------|-----------------|
| 1  | Part 1 - Setup & Discovery        | `GET /` – Discovery (HATEOAS)                            | **200**         |
| 2  | Part 2 - Room Management          | `GET /rooms`                                             | **200**         |
| 3  | Part 2 - Room Management          | `POST /rooms` – create `SCI-210`                         | **201**         |
| 4  | Part 2 - Room Management          | `GET /rooms/SCI-210`                                     | **200**         |
| 5  | Part 2 - Room Management          | `DELETE /rooms/SCI-210` – empty room                     | **204**         |
| 6  | Part 2 - Room Management          | `DELETE /rooms/LIB-301` – non-empty room                 | **409**         |
| 7  | Part 3 - Sensors & Filtering      | `GET /sensors`                                           | **200**         |
| 8  | Part 3 - Sensors & Filtering      | `GET /sensors?type=CO2` – `@QueryParam` filter           | **200**         |
| 9  | Part 3 - Sensors & Filtering      | `POST /sensors` – valid `roomId`                         | **201**         |
| 10 | Part 3 - Sensors & Filtering      | `POST /sensors` – invalid `roomId`                       | **422**         |
| 11 | Part 3 - Sensors & Filtering      | `POST /sensors` – wrong Content-Type                     | **415**         |
| 12 | Part 4 - Sub-Resources (Readings) | `GET /sensors/TEMP-001/readings`                         | **200**         |
| 13 | Part 4 - Sub-Resources (Readings) | `POST /sensors/TEMP-001/readings`                        | **201**         |
| 14 | Part 4 - Sub-Resources (Readings) | `GET /sensors/TEMP-001` – verify `currentValue` updated  | **200**         |
| 15 | Part 5 - Error Handling           | `POST /sensors/OCC-002/readings` – MAINTENANCE sensor    | **403**         |
| 16 | Part 5 - Error Handling           | `GET /rooms/DOES-NOT-EXIST`                              | **404**         |

> **Tip — demoing the side-effect:** fire request #13, then immediately fire #14. The sensor's `currentValue` will have changed from `22.4` to the value just posted, proving the parent-sensor update required by Part 4.2.

> **Tip — running everything at once:** hover the collection name → `…` → **Run collection** to fire all 16 in order with a single click.

> **Reset trick:** because storage is in-memory, restarting the server (`Ctrl+C`, then `java -jar ...` again) instantly resets to the seed data. Useful between demo takes.

---

## 5. Conceptual Report (answers to the in-spec questions)

### Part 1.1 — Default lifecycle of a JAX-RS resource class

By default, the JAX-RS runtime treats resource classes as **per-request**: a new instance of the resource class is created for *every* incoming HTTP request and discarded when the response has been written. This is the opposite of Spring's default behaviour — there is no shared instance unless you force one (for example by annotating the class with `@Singleton` or `@ApplicationScoped`, or by registering an already-constructed instance from your `Application#getSingletons()` method).

The implication is that **instance fields on a resource cannot be used to store application state** that must survive across requests. If `SensorRoom` kept its room list in a plain `List<Room>` field, each request would see an empty list. State must therefore live **outside** the resource — in a shared singleton (here, `DataStore.getInstance()`) that every resource instance obtains a reference to.

Because that shared store is touched by many per-request resource instances simultaneously, it must be **thread-safe**. Plain `HashMap`/`ArrayList` would risk lost updates, `ConcurrentModificationException`, or broken iteration if two requests mutated them at the same time. The implementation uses `ConcurrentHashMap` for rooms, sensors and the readings-index map, and wraps each per-sensor reading list in `Collections.synchronizedList(...)` inside `DataStore.appendReading`. Atomic operations such as `computeIfAbsent` are used to avoid the classic "check-then-act" race that would otherwise arise between two sensors being posted concurrently.

### Part 1.2 — Why Hypermedia (HATEOAS) matters

HATEOAS — returning links and navigation hints inside responses — is considered a hallmark of advanced REST because it makes the API **self-descriptive**. Clients no longer need to hard-code every URL template from static documentation; they discover the next legal operations from the server's own response. This yields three concrete benefits for client developers:

1. **Loose coupling to URL structure.** The server can reshape its URI space (e.g. moving `/rooms` to `/facilities/rooms` or introducing versioning) without breaking clients, as long as the link relations (`rel` names) stay stable.
2. **State-driven workflows.** A response can advertise only the transitions that are currently legal for the resource ("this order has `cancel` and `ship` links, but not `refund`"), so the business rules live in one place — the server — instead of being duplicated across every client.
3. **Reduced documentation drift.** Static docs go stale the moment the team ships a change; hypermedia links are generated at runtime from live routing, so they are guaranteed to match the code the server is actually running.

The `DiscoveryResource` here is a small, entry-point HATEOAS document: it advertises `self`, `rooms`, `sensors` and `sensorReadings` links so a fresh client can bootstrap without any prior knowledge of the server's paths.

### Part 2.1 — Returning IDs vs. full Room objects in a list

Returning only IDs is cheaper on the wire and clearer in intent — the client receives a thin index and then fetches `/rooms/{id}` on demand. This shines when rooms are numerous and heavy, or when most clients only care about a few specific entries: payload size stays low and each subsequent fetch is independently cacheable (CDN, ETag, conditional GET).

Returning full objects eliminates the "**N+1 problem**" — one request for the list followed by N follow-up requests. It's the right default for dashboards and grid views that always render the whole row. The trade-offs are increased bandwidth, higher JSON parsing cost on the client, and the risk that a listing response grows unbounded as the dataset scales.

In practice the API returns full `Room` objects from `GET /rooms` because the resource is small (id, name, capacity, sensor ID list) and clients typically render all fields. For very large collections the correct answer is usually neither extreme but a **paginated, sparse projection** (`?page`, `?size`, `?fields=`) that the client can opt into.

### Part 2.2 — Is DELETE idempotent here?

**Yes.** Idempotence requires that the *server state* after N identical requests is identical to the state after 1 such request — it does **not** require every response to be 200 OK.

- First `DELETE /rooms/LIB-301`: the room exists and is empty → `204 No Content`, room removed from the map.
- Second, third, Nth identical `DELETE /rooms/LIB-301`: the room is already gone, so the `NotFoundException` mapper returns `404 Not Found`.

The server state after the first call is indistinguishable from the server state after the hundredth call — the room is absent in both cases. The status code differs (204 → 404), but idempotence is a property of state, not of status. The implementation also refuses to "partially succeed": if the room still has sensors attached, the very first `DELETE` is rejected with `409` via `RoomNotEmptyException` and no state mutation occurs, so retries on a failure are safe too.

### Part 3.1 — Consequences of `@Consumes(APPLICATION_JSON)` on a mismatched Content-Type

The `@Consumes` annotation is a contract: it tells the JAX-RS runtime which request Content-Types this method will accept. When a client posts `text/plain` or `application/xml` to `POST /sensors`, Jersey's request-matching algorithm notices that none of the method's consumed media types match the request's `Content-Type` header and **short-circuits before the method body is ever invoked**, returning HTTP `415 Unsupported Media Type`. No MessageBodyReader is searched, no POJO deserialisation runs, no validation code executes.

This is desirable for two reasons:
- **Security** — unparsed payloads never reach the application, shrinking the attack surface of hostile content (XML XXE, unexpected binary data).
- **Clarity** — clients get a precise, standard status code (`415`) and can remediate without guessing, rather than receiving a generic 400 with an opaque parser error.

In this API the runtime's default 415 response is normalised into a uniform JSON error body by `WebApplicationExceptionMapper`, so clients see the same error shape whatever the failure mode.

### Part 3.2 — Why `@QueryParam` beats a path segment for filtering

`GET /sensors?type=CO2` is preferred over `GET /sensors/type/CO2` because **filtering is a property of the query, not a new resource**. Concretely:

1. **One canonical resource.** The path `/sensors` always identifies *the collection of sensors*. Query parameters refine which view of it is returned. Encoding filters into the path creates competing identifiers for the same underlying data and makes caching and permission rules harder to reason about.
2. **Composable, orthogonal filters.** Queries combine trivially: `?type=CO2&status=ACTIVE&roomId=LIB-301` is natural. In the path-segment design you'd have to invent an explosion of routes (`/sensors/type/CO2/status/ACTIVE/...`) and either enforce ordering or duplicate handlers.
3. **Optional by nature.** `@QueryParam` is absent-friendly — omitting it just means "no filter". Path segments are mandatory by construction.
4. **REST conventions.** Servers, proxies, frameworks, and OpenAPI tooling all treat query strings as filters/pagination and path segments as identifiers. Conforming to that convention makes the API easier to document, cache, and monitor.

### Part 4.1 — Architectural benefits of the Sub-Resource Locator pattern

A sub-resource locator is a JAX-RS method that returns a plain Java object which itself handles all child paths. In `SensorResource.readings(...)`, the locator validates that the parent sensor exists and then hands control to a dedicated `SensorReadingResource` instance.

The benefits over lumping every nested path into one controller are:

1. **Separation of concerns.** Reading-history logic (list, append, enforce state constraints) lives in its own class. `SensorResource` stops being a god-object that also happens to manage readings.
2. **Context carried as constructor state.** The parent `sensorId` is captured once in the sub-resource's constructor, so every handler method inside it is free of boilerplate `@PathParam("sensorId")` parameters.
3. **Centralised preconditions.** The locator is the **single point** where "does this sensor exist?" is checked. If the sensor is missing, none of the sub-resource's methods ever run — a guarantee that's impossible to forget in one place versus repeating an `if` in every method.
4. **Polymorphic routing.** The locator can return *different* sub-resource classes based on request state (e.g. an archived-sensor variant with a read-only endpoint set), which is impossible with flat `@Path` annotations.
5. **Testability.** `SensorReadingResource` can be instantiated directly in a unit test with a sensor ID — no servlet container needed.

### Part 5.2 — Why 422 is more accurate than 404 for a missing reference in a valid payload

`404 Not Found` describes the **request URI itself** — "I don't know this resource". When a client POSTs `/sensors` with `{"roomId":"GHOST-999"}`, the URI `/sensors` absolutely exists; the problem lives *inside* the JSON body, not at the endpoint.

`422 Unprocessable Entity` (RFC 4918) is defined precisely for this case: the server understands the syntax (the JSON parsed cleanly), but cannot process the contained instructions because a semantic constraint is violated — in this case, referential integrity. Using 422 communicates two things to the client that 404 cannot:

- "Your endpoint is correct — don't retry at a different URL."
- "Fix the payload content and resend."

Returning 404 here would be actively misleading: a client seeing 404 might assume the `/sensors` endpoint is gone and give up. 400 Bad Request is an acceptable fallback, but 422 is more specific — 400 conventionally signals a syntactic problem (malformed JSON, missing required field), whereas 422 signals a semantic one (the JSON is well-formed but references something that does not exist).

### Part 5.4 — Cybersecurity risk of leaked stack traces

A raw Java stack trace is a reconnaissance goldmine for an attacker. From a single 500 response they can learn:

- **Framework and versions** — class names such as `org.glassfish.jersey.server.ServerRuntime$Responder.process` reveal Jersey; the exact version dictates which known CVEs apply.
- **Database and ORM hints** — `org.hibernate.*`, `org.postgresql.Driver`, JDBC URLs in exception messages disclose the storage technology and sometimes even credentials or internal hostnames.
- **Internal package structure** — proprietary class names (`com.smartcampus.billing.InvoiceService`) expose business domains that aren't visible from the public API surface, giving an attacker vocabulary for social engineering and guided fuzzing.
- **File paths and line numbers** — hint at the operating system, where the app is deployed, and which branch of a conditional was taken, which helps craft bypasses.
- **Exploit feedback loops** — a `java.sql.SQLSyntaxErrorException` in the response confirms SQL injection is reaching the database; a `java.io.FileNotFoundException: /etc/passwd` confirms path traversal, and so on. Hiding the stack trace forces the attacker to work blind.

The `GenericExceptionMapper` in this project therefore returns a bland JSON envelope (`"InternalServerError", "An unexpected error occurred..."`) while the real stack trace is written to the server-side log where operators — not attackers — can see it.

### Part 5.5 — Why filters beat per-method `Logger.info()` calls

Logging is a **cross-cutting concern**: it must happen for every request and every response, uniformly, forever. Implementing it with `Logger.info()` calls at the top and bottom of each resource method has predictable pathologies:

1. **Repetition.** Every new endpoint requires the developer to remember to add both a request log and a response log. Over dozens of handlers the odds of an omission approach 1.
2. **Inconsistency.** Different authors produce different log formats ("GET /sensors", "[REQ] sensors", "incoming:/sensors?type=CO2"), defeating log aggregation and dashboards.
3. **Tight coupling.** Business logic is polluted with observability code, hurting readability and making it harder to test the domain behaviour in isolation.
4. **Blind spots.** Manual logging inside methods cannot capture errors raised *before* the method is called (e.g. 415 Unsupported Media Type, 405 Method Not Allowed) or responses shaped by exception mappers. A `ContainerResponseFilter` sits outside that flow and sees **every** response.
5. **No code changes to toggle.** With a `@Provider` filter, switching logging levels or disabling logging altogether is a configuration change. With inline logging, it may require touching and redeploying every resource class.

`LoggingFilter` in this project implements both `ContainerRequestFilter` (logs method + URI on the way in) and `ContainerResponseFilter` (logs the final HTTP status on the way out), using `java.util.logging.Logger`. Adding new endpoints requires no logging code — the filter applies automatically.

---

## 6. Error Response Shape

Every error mapper returns the same envelope so clients only have to parse one shape:

```json
{
  "status": 409,
  "error": "RoomNotEmpty",
  "message": "Room 'LIB-301' cannot be deleted: still hosts 2 sensor(s).",
  "timestamp": "2026-04-18T13:30:00Z",
  "details": {
    "roomId": "LIB-301",
    "activeSensorCount": 2
  }
}
```

| Scenario                                                     | Exception                            | HTTP status |
|--------------------------------------------------------------|--------------------------------------|-------------|
| Delete a room that still has sensors                         | `RoomNotEmptyException`              | `409`       |
| POST a sensor whose `roomId` does not exist                  | `LinkedResourceNotFoundException`    | `422`       |
| POST a reading to a sensor in `MAINTENANCE` / `OFFLINE`      | `SensorUnavailableException`         | `403`       |
| GET / DELETE a non-existent room or sensor                   | `NotFoundException`                  | `404`       |
| Content-Type mismatch, method not allowed, etc.              | `WebApplicationException` subclasses | framework-determined |
| Anything else (`NullPointerException`, etc.)                 | `Throwable` (generic safety net)     | `500`       |

---

