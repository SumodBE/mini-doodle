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

* **Interactive UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI Spec (JSON):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

You can use the Swagger UI to test endpoints directly from your browser.

---

## 🏗 Architecture & Design Decisions

### 1. The Consumption Model (Hard Allocation)
We chose a design where booking a meeting **consumes** availability rather than overlaying it.
* **Why?** This optimizes "Search" performance. Determining if a user is free is a generic `COUNT` query on the `Availability` table, rather than a complex calculation of `(Availability - Meetings)`.
* **Trade-off:** Cancellation requires logic to "refund" time (implemented via the Merge Engine).

### 2. Smart Slot Management (The "Engine")
To support the Consumption Model, we implemented a robust domain engine in the Data Access layer:
* **Automatic Splitting:** When a meeting is booked in the middle of a slot (e.g., 09:00-12:00), the engine splits it into two fragments (09:00-10:00 and 11:00-12:00) and removes the booked portion.
* **Automatic Merging:** When a meeting is cancelled or availability is manually added, the engine detects adjacent or overlapping slots and merges them into a single continuous block. This prevents data fragmentation.

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

If you prefer using `curl` or Postman instead of Swagger:

### 1. Add Availability
**POST** `/api/availability`
```json
{
  "email": "alice@example.com",
  "start": "2026-02-01T09:00:00Z",
  "durationMinutes": 180
}