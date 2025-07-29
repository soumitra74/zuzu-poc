# Review System Microservice

This microservice retrieves Agoda.com / Booking.com / Expedia reviews from an AWS S3 bucket, processes the data, and stores it in a relational database (e.g., PostgreSQL).

## Features
- Two separate Jobs: 
-- to pull review files from S3 and store each line of JSONL file as a record Parses and processes 
-- to process reach record and build a review system (in RDBMS)

## Architecture
The system follows a microservices architecture with the following key components:

### Data Flow
1. **S3 File Processing Job**
   - Scans configured S3 bucket for JSONL files containing hotel reviews
   - Downloads and processes files line by line
   - Stores raw records in database for further processing
   - Tracks job and file processing status
   - Avoid duplicate processing using file name and updated timestamp

2. **Record Processing Job** 
   - Retrieves unprocessed records from database
   - Parses JSON data into domain objects
   - Validates and transforms data
   - Stores normalized data in relational tables
   - Handles errors and retries
   - Tracks each record's processing status, processing time
   - Errors are stored in record_errors table for further analysis
   - Tracks source system IDs in external_id columns (wherever applicable)

### Key Components
- **AWS S3 Integration**
  - Uses AWS SDK to interact with S3 bucket
  - Supports local testing via LocalStack

- **Database Layer**
  - PostgreSQL for persistent storage
  - Separate schemas for raw data and processed reviews
  - JPA/Hibernate for ORM

- **Processing Engine**
  - Multi-threaded processing for improved throughput
  - Error handling and retry mechanisms
  - Idempotent processing to handle duplicates

- **Monitoring & Logging**
  - Job status tracking
  - Error logging and reporting
  - Processing metrics and statistics

### Technology Stack
- Spring Boot for application framework
- AWS SDK for S3 integration
- PostgreSQL for data storage
- JPA/Hibernate for persistence
- Log4j2 for logging

## Project Structure

```
zuzu-poc/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/
│   │   │       └── soumitra/
│   │   │           └── reviewsystem/
│   │   │               ├── ReviewSystemApplication.java
│   │   │               ├── config/
│   │   │               ├── controller/
│   │   │               ├── service/
│   │   │               ├── dao/
│   │   │               ├── model/
│   │   │               ├── dto/
│   │   │               └── util/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── logback-spring.xml
│   └── test/
│       └── java/
│           └── org/
│               └── soumitra/
│                   └── reviewsystem/
│
├── scripts/
│
├── docker/
│   └── docker-compose.yml
│
├── .gitignore
├── README.md
├── pom.xml
```

## Database configuration

(I have used postgres for this implementation.)

Create an user zuzu and a db named hotel_reviews
Update .properties file for db configuration.

## Other configuration

Used localstack for S3

```
$ awslocal s3api list-objects-v2 --bucket hotel-reviews
{
    "Contents": [
        {
            "Key": "agoda_com_2025-04-10.jl",
            "LastModified": "2025-07-25T17:31:55.000Z",
            "ETag": "\"57c9b10433248d0c3d4aeef7610900e6\"",
            "ChecksumAlgorithm": [
                "CRC32"
            ],
            "ChecksumType": "FULL_OBJECT",
            "Size": 90918,
            "StorageClass": "STANDARD"
        }
    ],
    "RequestCharged": null,
    "Prefix": ""
}
```