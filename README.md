# Static Workflow Service

A Spring Boot 3.x service that runs a hard‑coded workflow (no n8n JSON at runtime).

## Build & Run

```bash
mvn clean package
java -jar target/workflow-service-0.0.1-SNAPSHOT.jar
```

## Execute

```bash
curl -X POST http://localhost:8080/workflows/execute \
     -H "Content-Type: application/json" \
     -d '{"query":{"email":"hmchiud@tsmc.com"}}'
```
