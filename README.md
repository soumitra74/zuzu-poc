# Review System Microservice

This microservice retrieves Agoda.com / Booking.com / Expedia reviews from an AWS S3 bucket, processes the data, and stores it in a relational database (e.g., PostgreSQL).

## Features
- Two separate Jobs: 
  - to pull review files from S3 and store each line of JSONL file as a record
  - to process reach record and build a review system (in RDBMS)

## Architecture
The system is a single microservice with the following key components:

### Data Flow
1. **S3 File Processing Job**
   - Scans configured S3 bucket for JSONL files containing hotel reviews
   - Downloads and processes files line by line
   - Stores raw records in database for further processing
   - Tracks job and file processing status
   - Avoids duplicate processing using file name and updated timestamp

2. **Record Processing Job** 
   - Retrieves unprocessed records from database
   - Parses JSON data into domain objects
   - Validates and transforms data
   - Stores normalized data in relational tables
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
  - Idempotent processing to handle duplicates

- **Monitoring & Logging**
  - Job status tracking
  - Error logging and reporting
  - Processing metrics recorded

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
└── pom.xml
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

Used simple API key based authentication. Refer to the API_KEY_Authentication.md file.

It supports both CLI and API based access. Refer to CLI_Readme and REST_API_Readme for usage instructions.

Logging is not implemented yet (WIP). For now it uses System.out and System.err for debug information.

## How to run

### Set up DB

Create hotel_reviews db in postgres with user zuzu as owner.
Run the following commands from db/init folder:

```
psql -U zuzu -d hotel_reviews < 01_reviews_schema.sql
psql -U zuzu -d hotel_reviews < 02__job_and_file_tracking.sql
psql -U zuzu -d hotel_reviews < 04__create_api_keys_table.sql
```

Upload test file(s) in localstack:

```
pipenv shell
pip install awscli awscli-local

awslocal s3 mb s3://hotel-reviews
awslocal s3 cp /d/soumitra/Downloads/agoda_com_2025-04-10.jl s3://hotel-reviews/agoda_com_2025-04-10.jl
```

### Compilation and running

Clone this repository. Install dependent components (like java, maven etc.).

Run the following commands for testing:

```
mvn compile package
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar run-job --bucket=hotel-reviews
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar process-records 
```

> Note that I have used Mockito, but also created a local MockS3Client class for ease of implementation.


## Job Scheduling

These jobs can be configured with a scheduler using REST endpoint. Note that I have assumed that S3 job (aka run-job) will trigger process-records job. Initially I was planning to have just one job, but it made sense to have two idependent ones. process-records job can be fanned out for parallel processing.  

## Known Issues and Enhancements

### Enhancements

1. DB schema can be further normalised. E.g. country can be a separate entity etc.
2. Authentication should be integrated with Cognito.
3. S3 bucket should have three folders: inbox, processing, processed.

### Issue list

High priority
1. Create folder structure in S3. Move processed files to a different folder in S3
	a. alternatively, rename the file to .processing and .processed in S3
	b. also check in db whether the file is already processed -- DONE
	c. use a date filter -- DONE
2. Create auth layer: Simple Role-Based API Keys -- DONE
3. Fix unit tests (mocking of S3Client) -- DONE
4. Fix response related fields like responder_name, text, date text

Medium priority
1. remove processed_at from s3_files entity -- DONE
2. rename record.downloaded_at to created_at -- SKIP
3. remove new from s3_files entity -- SKIP
4. Add a logging framework
5. refactor review. create a review_response entity
6. versioning of data like hotel, review, provider name etc.
7. remove started_at from job_runs table -- DONE
8. refactor country
9. ReviewProviderLogo is missing
10. ReviewProviderText == provider name?
11. review summary table needs to have a FK relationship with review -- DONE
12. remove checksum in s3 files -- DONE

Low priority
1. rename s3_files to source_files -- SKIPPED
2. rating_category may need a provider_id -- SKIPPED
3. rename flag_code to flag_name -- SKIPPED
4. reviewerReviewedCount: is it same as reviews_written?
5. ratingRaw to be renamed to rating -- DONE
6. isResponseShown should be changed to isShowReviewResponse?
