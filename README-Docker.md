# Docker Configuration for Hotel Reviews Microservice

This document provides comprehensive instructions for running the hotel reviews microservice using Docker.

## Overview

The Docker configuration includes:
- **Spring Boot Application** - The main microservice
- **PostgreSQL Database** - For data persistence
- **LocalStack** - For S3 emulation in development
- **Redis** (optional) - For caching

## Prerequisites

- Docker and Docker Compose installed
- At least 4GB of available RAM
- Ports 8080, 5432, and 4566 available

## Quick Start

### 1. Build and Start Services

```bash
# Build the application
docker-compose build

# Start all services
docker-compose up -d

# Check service status
docker-compose ps
```

### 2. Initialize LocalStack (Development)

```bash
# Initialize S3 bucket and test data
./docker/scripts/docker-utils.sh init-s3
```

### 3. Verify Services

```bash
# Check service health
./docker/scripts/docker-utils.sh health

# View application logs
docker-compose logs -f app
```

## Configuration

### Environment Variables

The application uses the following environment variables:

#### Database Configuration
- `SPRING_DATASOURCE_URL` - Database connection URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password
- `SPRING_JPA_HIBERNATE_DDL_AUTO` - Hibernate DDL mode

#### AWS S3 Configuration
- `AWS_S3_ENDPOINT` - S3 endpoint URL
- `AWS_S3_BUCKET` - S3 bucket name
- `AWS_REGION` - AWS region
- `AWS_ACCESS_KEY_ID` - AWS access key
- `AWS_SECRET_ACCESS_KEY` - AWS secret key

#### Application Configuration
- `SERVER_PORT` - Application port (default: 8080)
- `SPRING_PROFILES_ACTIVE` - Spring profile (dev/prod)
- `JAVA_OPTS` - JVM options

### Profiles

#### Development Profile
- Uses LocalStack for S3 emulation
- Debug logging enabled
- SQL queries logged
- Hot reload support

#### Production Profile
- Uses real AWS S3
- Optimized JVM settings
- Minimal logging
- Resource limits applied

## Docker Compose Files

### Main Configuration (`docker-compose.yml`)
- Base configuration for all environments
- Includes PostgreSQL, LocalStack, and application

### Development Override (`docker-compose.override.yml`)
- Development-specific settings
- Debug ports exposed
- Enhanced logging
- Source code mounting (optional)

### Production Configuration (`docker-compose.prod.yml`)
- Production-optimized settings
- Resource limits
- Security hardening
- Health checks

## Usage Examples

### Development Environment

```bash
# Start development environment
docker-compose up -d

# View logs
docker-compose logs -f app

# Run tests
docker-compose exec app mvn test

# Access database
docker-compose exec db psql -U postgres -d hotel_reviews

# Access LocalStack
aws --endpoint-url=http://localhost:4566 s3 ls s3://hotel-reviews/
```

### Production Environment

```bash
# Start production environment
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Check health
curl http://localhost:8080/api/jobs/health

# View logs
docker-compose logs -f app
```

### Using Utility Scripts

```bash
# Make scripts executable
chmod +x docker/scripts/*.sh

# Start services
./docker/scripts/docker-utils.sh start

# Check health
./docker/scripts/docker-utils.sh health

# View logs
./docker/scripts/docker-utils.sh logs app

# Stop services
./docker/scripts/docker-utils.sh stop

# Clean up
./docker/scripts/docker-utils.sh cleanup
```

## Service Endpoints

### Application
- **Health Check**: http://localhost:8080/api/jobs/health
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Metrics**: http://localhost:8080/api/jobs/metrics

### Database
- **Host**: localhost
- **Port**: 5432
- **Database**: hotel_reviews
- **Username**: postgres
- **Password**: postgres

### LocalStack (Development)
- **S3 Endpoint**: http://localhost:4566
- **Region**: us-east-1
- **Access Key**: test
- **Secret Key**: test

## Troubleshooting

### Common Issues

#### 1. Port Conflicts
```bash
# Check what's using the ports
netstat -tulpn | grep :8080
netstat -tulpn | grep :5432
netstat -tulpn | grep :4566
```

#### 2. Database Connection Issues
```bash
# Check database logs
docker-compose logs db

# Test database connection
docker-compose exec db pg_isready -U postgres -d hotel_reviews
```

#### 3. Application Startup Issues
```bash
# Check application logs
docker-compose logs app

# Check application health
curl http://localhost:8080/api/jobs/health
```

#### 4. S3/LocalStack Issues
```bash
# Check LocalStack logs
docker-compose logs localstack

# Test S3 connection
aws --endpoint-url=http://localhost:4566 s3 ls
```

### Debug Mode

To run the application in debug mode:

```bash
# Start with debug port
docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d

# Connect debugger to localhost:5005
```

### Resource Monitoring

```bash
# Check resource usage
docker stats

# Check disk usage
docker system df
```

## Security Considerations

### Development
- Database credentials are hardcoded for convenience
- LocalStack uses test credentials
- Debug ports are exposed

### Production
- Use environment variables for sensitive data
- Database credentials should be externalized
- Debug ports should be disabled
- Resource limits should be applied
- Health checks should be configured

## Performance Optimization

### JVM Settings
- `-XX:+UseContainerSupport` - Optimize for containers
- `-XX:MaxRAMPercentage=75.0` - Use 75% of available memory
- `-XX:+UseG1GC` - Use G1 garbage collector
- `-XX:+UseStringDeduplication` - Reduce memory usage

### Database Optimization
- Connection pooling configured
- Prepared statements enabled
- Query optimization enabled

### Container Optimization
- Multi-stage build reduces image size
- Non-root user for security
- Health checks for reliability
- Resource limits for stability

## Monitoring and Logging

### Application Logs
```bash
# View application logs
docker-compose logs -f app

# View specific log levels
docker-compose logs app | grep ERROR
```

### Database Logs
```bash
# View database logs
docker-compose logs -f db
```

### System Metrics
```bash
# Check container resource usage
docker stats

# Check disk usage
docker system df
```

## Backup and Recovery

### Database Backup
```bash
# Create backup
docker-compose exec db pg_dump -U postgres hotel_reviews > backup.sql

# Restore backup
docker-compose exec -T db psql -U postgres hotel_reviews < backup.sql
```

### Volume Backup
```bash
# Backup volumes
docker run --rm -v hotel-reviews_postgres_data:/data -v $(pwd):/backup alpine tar czf /backup/postgres_backup.tar.gz -C /data .

# Restore volumes
docker run --rm -v hotel-reviews_postgres_data:/data -v $(pwd):/backup alpine tar xzf /backup/postgres_backup.tar.gz -C /data
```

## Cleanup

### Remove All Resources
```bash
# Stop and remove containers, networks, and volumes
docker-compose down -v --remove-orphans

# Remove unused Docker resources
docker system prune -f
```

### Remove Specific Resources
```bash
# Remove only containers
docker-compose down

# Remove containers and networks
docker-compose down --remove-orphans

# Remove volumes
docker volume rm hotel-reviews_postgres_data hotel-reviews_localstack_data
```

## Support

For issues related to:
- **Docker Configuration**: Check this document
- **Application Logic**: Check the main README
- **Database Issues**: Check PostgreSQL documentation
- **S3 Issues**: Check LocalStack documentation

## Contributing

When modifying Docker configuration:
1. Test changes in development environment
2. Update documentation
3. Test in production-like environment
4. Update version tags if needed 