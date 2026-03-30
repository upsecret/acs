# ACS (Access Control System)

Spring Boot 4.0.5 / Spring Cloud 2025.1.1 (Oakwood) / Java 21 기반 API Gateway + Config Server

```
Client → [Swarm Ingress L4] → Gateway(x2) → [Swarm DNS] → Services
                                    ↕
                            Config Server (Oracle JDBC)
                            Redis (rate limit, blacklist)
                            Kafka (bus refresh, events)
                            OTel Collector → Elasticsearch
```

## Modules

| Module | Description |
|--------|-------------|
| `common-lib` | JWT 유틸, 공유 DTO, 상수 |
| `config-server` | Spring Cloud Config Server (Oracle JDBC backend) |
| `gateway-service` | Spring Cloud Gateway (WebFlux, Bucket4j rate limiter) |

## Quick Start

```bash
# Build
./gradlew clean build

# Local infra only (IDE에서 앱 직접 실행)
docker compose up -d

# Full stack (인프라 + 앱 전부)
docker compose -f docker-compose.full.yml up -d
```

## Environment Configuration

단일 `docker/swarm-stack.yml`을 `.env` 파일만 바꿔서 모든 환경에 배포:

```bash
./deploy.sh local   # 로컬 Swarm
./deploy.sh dev     # 개발 환경
./deploy.sh qa      # QA 환경
./deploy.sh prod    # 운영 환경
```

| Item | local | dev | qa | prod |
|------|-------|-----|-----|------|
| Gateway replicas | 1 | 1 | 2 | 2 |
| Config replicas | 1 | 1 | 2 | 2 |
| Oracle | local container | dev server | qa server | prod server |
| Redis | local container | dev shared | qa shared | prod shared |
| Rate limit | 1000/min | 500/min | 100/min | 100/min |
| JWT expiration | 365 days (all envs, changeable via Config Server) ||||
| Log level | DEBUG | DEBUG | INFO | WARN |
| OTel sampling | 100% | 100% | 50% | 10% |
| trusted-proxies | `.*` | internal | internal | specific IP |

## Key Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/auth/token` | POST | JWT 토큰 발급 |
| `/auth/logout` | POST | 토큰 블랙리스트 (로그아웃) |
| `/api/hr/**` | * | HR 서비스 프록시 (인증 + rate limit) |
| `/api/attendance/**` | * | 근태 서비스 프록시 (인증 + rate limit) |
| `/actuator/health` | GET | 헬스체크 |
| `/actuator/busrefresh` | POST | Config 변경 전파 |

## Config Refresh Flow

```
Oracle PROPERTIES 변경
  → POST /actuator/busrefresh (any instance)
  → Kafka → all Gateway instances receive RefreshEvent
  → @RefreshScope beans recreated
```

## Token Blacklist Flow

```
POST /auth/logout (Bearer token)
  → Redis blacklist:{jti} (TTL = remaining token lifetime)
  → Caffeine local cache (TTL 30s)
  → Kafka Bus → all Gateway instances sync local cache
```

## Observability

Spring Boot 4 + OpenTelemetry (OTLP) → OTel Collector → Elasticsearch/Kibana

- Local Kibana: http://localhost:5601
- Traces: APM → Services → gateway-service / config-server
- Metrics: rate_limit_denied_total, gateway.auth spans
- Logs: trace_id/span_id auto-correlated via logback appender

QA/Prod: `.env` 파일의 `OTEL_EXPORTER_OTLP_*` endpoint를 외부팀 Collector로 변경

## Operations

```bash
# Rolling update
docker service update --image acs/gateway-service:v2 acs_gateway-service

# Scale
docker service scale acs_gateway-service=3

# Logs
docker service logs -f acs_gateway-service
```

## Tech Stack

- Java 21, Spring Boot 4.0.5, Spring Cloud 2025.1.1
- Jackson 3 (Boot 4 default), jjwt 0.13.0
- Spring Cloud Gateway Server WebFlux 5.0.x
- Bucket4j + Redis Lettuce (distributed rate limiting)
- Caffeine (local blacklist cache)
- Spring Cloud Bus + Kafka (config refresh, events)
- OpenTelemetry + OTLP → Elastic Observability
- Docker Swarm (deployment, Swarm DNS routing — no `lb://`)
