# Mini-Doodle: Meeting Scheduling Service

A robust RESTful API for managing user availability and scheduling meetings with automatic conflict detection and slot management.

This service implements a **"Hard Allocation" (Consumption)** model where scheduling a meeting consumes the corresponding availability slot, splitting and merging time blocks automatically to maintain data integrity.

---

## 🚀 Quick Start (Zero Setup)

Prerequisites: **Docker** & **Docker Compose** (No local Java or Gradle installation required).

1.  **Clone the repository:**
    ```bash
    git clone <your-repo-link>
    cd mini-doodle
    ```

2.  **Start the application:**
    ```bash
    docker-compose up --build
    ```
    *Note: The first run may take a few minutes as Docker downloads dependencies and builds the application.*

3.  **Verify it's running:**
    The service will be available at [http://localhost:8080](http://localhost:8080).

---

## 📚 API Documentation

Once the application is running, full interactive documentation is available via **Swagger UI**.

* **Swager UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI Spec (JSON):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

You can use the Swagger UI to test endpoints directly from your browser.

---

## 🏗 Architecture & Design Decisions

### 1. The Consumption Model (Hard Allocation)
We chose a design where booking a meeting **consumes** availability rather than overlaying it.
* **Why?** This optimizes "Search" performance. Determining if a user is free becomes a generic `COUNT` query on the `Availability` table, rather than a computationally expensive calculation of `(Availability - Meetings)`.
* **Trade-off:** Cancellation requires logic to "refund" time back to the availability pool. This is handled automatically by the **Availability Engine**.
### 2. Smart Slot Management (The "Availability Engine")
To support the Consumption Model, we implemented a robust domain engine that handles the lifecycle of time slots.

* **Feature A: Automatic Splitting & Consumption**
    * *Scenario:* User is available **09:00 - 12:00**.
    * *Action:* A meeting is booked for **10:00 - 11:00**.
    * *Result:* The engine handles three variations:
        1. **Middle Split:** Creates two fragments (**09-10** and **11-12**).
        2. **Edge Trim:** Booking **09-10** leaves one fragment (**10-12**).
        3. **Full Consumption:** Booking **09-12** removes the slot entirely.

* **Feature B: Automatic Merging**
    * *Scenario:* User has availability **09:00 - 10:00**.
    * *Action:* User adds new availability (or cancels a meeting) for **10:00 - 11:00**.
    * *Result:* The engine detects that these slots touch (or overlap) and seamlessly merges them into a single continuous block: **09:00 - 11:00**.
### 3. Native Queries for JSONB
Participants are stored as a JSON List (`["a@b.com", "c@d.com"]`) for flexibility and lightweight storage.
* **Decision:** We used **PostgreSQL Native Queries** with `jsonb_array_elements_text` to efficiently query inside this JSON blob for conflict detection, which standard JPQL cannot handle efficiently.

---

## 🛠 Developer Tools

We have included additional tools in the Docker setup to aid in review and debugging.

### 🐞 Remote Debugging
The service exposes port `5005` for remote debugging.
* **How to use:** Attach your IDE (IntelliJ/Eclipse) Remote Debugger to `localhost:5005`.

### 🗄 Database GUI (pgAdmin)
A pgAdmin container is running for easy database inspection.
* **URL:** [http://localhost:5050](http://localhost:5050)
* **Email:** `admin@admin.com`
* **Password:** `admin`
* **Server Connection Details:**
    * **Host:** `mini-doodle-postgres` (Service name)
    * **Username:** `minidoodle_root_user`
    * **Password:** `secret123`

---

## 🧪 API Usage Guide (Manual)

If you prefer using `curl` instead pf Postman or Swagger UI:

### 1. Register User
```bash
curl -X POST 'http://localhost:8080/user/register' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "firstName": "Nathan",
  "lastName": "Gold",
  "email": "nathan.gold@hotmail.com",
  "timezone": "Asia/Kolkata"
}'
```

### 2. Add Availability
```bash
curl -X 'POST'
  'http://localhost:8080/availability/create' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "email": "alice@example.com",
  "start": "2026-02-01T09:00:00Z",
  "durationMinutes": 180
}'
```
### 3. Remove Availability
```bash
curl -X 'POST' \
  'http://localhost:8080/availability/remove' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "email": "user@example.com",
  "start": "2026-02-01T10:00Z",
  "durationMinutes": 30
}'
```

### 4. Schedule Meeting
```bash
curl -X 'POST' \
  'http://localhost:8080/availability/remove' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "email": "user@example.com",
  "start": "2026-02-01T10:00Z",
  "durationMinutes": 30
}'
```
### 5. Cancel Meeting
```bash
curl -X 'DELETE' \
  'http://localhost:8080/meetings/cancel/1573d3b9-b294-4f45-a435-7138db05efd7' \
  -H 'accept: application/json'
```


