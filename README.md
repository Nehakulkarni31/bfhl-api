# BFHL REST API — Bajaj Finserv Health Assessment

## Quick Start

```bash
mvn clean install
mvn spring-boot:run
```

API available at: `http://localhost:8080/bfhl`

## Endpoint

`POST /bfhl`

### Headers
```
X-Request-Id: REQ-1001
Content-Type: application/json
```

### Request
```json
{
  "data": ["A", "1", "22", "$", "B", "7"]
}
```

## Deploy to Render

1. Push this repo to GitHub
2. Go to https://render.com → New → Web Service
3. Connect your GitHub repo
4. Build command: `mvn clean package -DskipTests`
5. Start command: `java -jar target/bfhl-1.0.0.jar`
6. Environment: Java
7. Click Deploy

## Run Tests

```bash
mvn test
```
