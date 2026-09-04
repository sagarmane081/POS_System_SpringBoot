# POS System — Spring Boot Backend

A REST API backend for a Point-of-Sale (POS) system, built with **Spring Boot 3 / Java 21**.
It provides secure, JWT-authenticated APIs with integrated online payments, database
migrations, and containerised deployment.

> **Status:** ~90% complete and actively developed. Backend is deployed to AWS EC2 via Docker.
> Role-based access control and the React frontend are in progress (see [Roadmap](#roadmap)).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 3.3.5 (Web, Security, Data JPA, Validation, Mail) |
| Auth | Spring Security + JWT (jjwt) |
| Persistence | Hibernate / JPA on **MySQL** |
| Migrations | Flyway |
| Mapping | MapStruct (entity ↔ DTO) |
| Payments | Stripe, Razorpay |
| API Docs | springdoc OpenAPI / Swagger UI |
| Build | Maven (wrapper included) |
| Containerisation | Docker + Docker Compose |
| CI | GitHub Actions |
| Code Quality | Qodana |

---

## Features

- **JWT authentication** with Spring Security.
- **Online payment integration** via Stripe and Razorpay.
- **REST APIs** documented with OpenAPI/Swagger and verified in Postman.
- **Versioned database schema** managed with Flyway migrations.
- **Clean DTO mapping** using MapStruct.
- **Dockerised** for reproducible builds and deployment.

> _Core modules: list your actual domain modules here (e.g., Products, Orders, Payments, Categories) so readers know the scope at a glance._

---

## Getting Started

### Prerequisites
- Java 21
- MySQL 8 (running locally, or via Docker)
- Docker & Docker Compose (optional, for containerised run)

### 1. Clone
```bash
git clone https://github.com/sagarmane081/POS_System_SpringBoot.git
cd POS_System_SpringBoot
```

### 2. Configure
Set the following in `src/main/resources/application.properties` (or as environment
variables). **Do not commit real secrets** — keep them out of version control.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/pos_db
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD

# JWT
app.jwt.secret=YOUR_JWT_SECRET
app.jwt.expiration=3600000

# Payments
stripe.api.key=YOUR_STRIPE_KEY
razorpay.key.id=YOUR_RAZORPAY_KEY_ID
razorpay.key.secret=YOUR_RAZORPAY_KEY_SECRET
```
> Property names above are typical — adjust them to match the keys actually used in your code.

### 3. Run locally
```bash
./mvnw spring-boot:run
```

### 4. Run with Docker
```bash
docker compose up --build
```

The API will be available at `http://localhost:8080`.

---

## API Documentation

Once running, interactive API docs are available at:

- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI spec:** `http://localhost:8080/v3/api-docs`

<!-- Add a screenshot of the Swagger UI here — it makes the repo land instantly for reviewers.
     Save the image under /docs and reference it like below: -->
<!-- ![Swagger UI](docs/swagger-ui.png) -->

---

## Testing

```bash
./mvnw test
```

---

## Deployment

The application is containerised with Docker and deployed to **AWS EC2**.
Build the image and run the container, or deploy the `docker-compose.yml` stack on the host.

---

## Roadmap

- [ ] Role-based access control (Admin / Cashier)
- [ ] React frontend (in development)
- [ ] Additional test coverage

---

## Author

**Sagar Mane** — [LinkedIn](https://linkedin.com/in/sagar-mane-502966140) · [GitHub](https://github.com/sagarmane081)
