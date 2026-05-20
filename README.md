# DWSC Backend

Minimal Spring Boot web app (no database). Run it without PostgreSQL.

## Run

```bash
mvn spring-boot:run
```

- `http://localhost:8080/` → JSON `{"message":"Hello World"}`
- `http://localhost:8080/health` → plain text `ok`

When you add PostgreSQL/JPA later, add the starters and datasource config again.
