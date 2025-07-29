# API Key Authentication

This document explains how to use the Simple Role-Based API Key authentication system implemented for the JobController.

## Overview

The API uses a simple role-based API key authentication system where:

- API keys are stored in the database
- Each API key has a role (ADMIN, OPERATOR, VIEWER, EXECUTOR)
- Different roles have different permissions
- API keys can be active/inactive and have expiration dates

## Roles and Permissions

### ADMIN
- **Permissions**: READ, WRITE, DELETE, EXECUTE
- **Access**: Full access to all endpoints
- **Use Case**: System administrators

### OPERATOR
- **Permissions**: READ, WRITE, EXECUTE
- **Access**: Can read data, write data, and execute jobs
- **Use Case**: Operations team

### VIEWER
- **Permissions**: READ
- **Access**: Can only read data and view status
- **Use Case**: Monitoring and reporting

### EXECUTOR
- **Permissions**: EXECUTE
- **Access**: Can only execute jobs
- **Use Case**: Automated systems that need to trigger jobs

## Default API Keys

The system comes with these default API keys for testing:

| Key | Role | Description |
|-----|------|-------------|
| `admin-key-123456789` | ADMIN | Full access |
| `operator-key-987654321` | OPERATOR | Read, write, execute |
| `viewer-key-456789123` | VIEWER | Read only |
| `executor-key-789123456` | EXECUTOR | Execute only |

## Using API Keys

### HTTP Header
Include the API key in the `X-API-Key` header:

```bash
curl -H "X-API-Key: admin-key-123456789" \
     -H "Content-Type: application/json" \
     -X POST http://localhost:8080/api/jobs/run-s3-ingest \
     -d '{"batchSize": 10}'
```

### Example Requests

#### Execute a Job (requires EXECUTE permission)
```bash
curl -H "X-API-Key: executor-key-789123456" \
     -H "Content-Type: application/json" \
     -X POST http://localhost:8080/api/jobs/run-s3-ingest \
     -d '{"s3Uri": "s3://my-bucket/data/", "batchSize": 20}'
```

#### View Jobs (requires READ permission)
```bash
curl -H "X-API-Key: viewer-key-456789123" \
     -X GET http://localhost:8080/api/jobs
```

#### Health Check (no authentication required)
```bash
curl -X GET http://localhost:8080/api/jobs/health
```

## API Key Management

### Create New API Key
```bash
curl -H "X-API-Key: admin-key-123456789" \
     -H "Content-Type: application/json" \
     -X POST http://localhost:8080/api/admin/keys \
     -d '{
       "name": "My New API Key",
       "role": "OPERATOR",
       "permissions": "[\"READ\", \"WRITE\", \"EXECUTE\"]"
     }'
```

### List All API Keys
```bash
curl -H "X-API-Key: admin-key-123456789" \
     -X GET http://localhost:8080/api/admin/keys
```

### Deactivate API Key
```bash
curl -H "X-API-Key: admin-key-123456789" \
     -X PUT http://localhost:8080/api/admin/keys/1/deactivate
```

### Delete API Key
```bash
curl -H "X-API-Key: admin-key-123456789" \
     -X DELETE http://localhost:8080/api/admin/keys/1
```

## Endpoint Permissions

| Endpoint | Method | Required Permission |
|----------|--------|-------------------|
| `/api/jobs/health` | GET | None (public) |
| `/api/jobs/run-s3-ingest` | POST | EXECUTE |
| `/api/jobs/run-record-processor` | POST | EXECUTE |
| `/api/jobs` | GET | READ |
| `/api/jobs/{id}` | GET | READ |
| `/api/jobs/s3-files` | GET | READ |
| `/api/jobs/s3-files/{id}` | GET | READ |
| `/api/jobs/records` | GET | READ |
| `/api/jobs/records/{id}` | GET | READ |
| `/api/jobs/record-errors` | GET | READ |
| `/api/jobs/record-errors/{id}` | GET | READ |

## Security Features

1. **API Key Validation**: Keys are validated against the database
2. **Role-Based Access**: Different roles have different permissions
3. **Key Expiration**: API keys can have expiration dates
4. **Usage Tracking**: Last used timestamp is updated on each request
5. **Key Hiding**: Actual key values are hidden in responses for security

## Error Responses

### Missing API Key
```json
{
  "success": false,
  "error": "API key is required",
  "status": 401
}
```

### Invalid API Key
```json
{
  "success": false,
  "error": "Invalid or expired API key",
  "status": 401
}
```

### Insufficient Permissions
```json
{
  "success": false,
  "error": "Insufficient permissions for this operation",
  "status": 403
}
```

## Best Practices

1. **Secure Storage**: Store API keys securely and never expose them in logs
2. **Key Rotation**: Regularly rotate API keys
3. **Minimal Permissions**: Use the least privileged role necessary
4. **Monitoring**: Monitor API key usage for suspicious activity
5. **Expiration**: Set expiration dates for temporary access

## Database Schema

The API keys are stored in the `api_keys` table:

```sql
CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    permissions TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP
);
```