# Corporate Proxy CA Certificate

Place your corporate proxy CA certificate here as `proxy.crt`.

This file is `.gitignore`d and must be obtained from your IT department.

## Usage

```bash
docker compose -f docker-compose.full.yml -f docker-compose.proxy.yml up -d
```

This injects `proxy.crt` into Kibana via `NODE_EXTRA_CA_CERTS` so it can
download Fleet packages from `epr.elastic.co` through the corporate proxy.
