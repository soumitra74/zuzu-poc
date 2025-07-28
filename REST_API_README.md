# Review System REST API

This document describes the REST API endpoints for the Review System application.

## Base URL
```
http://localhost:8080/api/jobs
```

## Endpoints

### 1. Contoller health
**GET** `/api/jobs/health`

Returns the status of the job controller.

**Response:**
```json
{
  "status": "ready",
  "availableJobs": ["job-runner", "record-processor"],
  "message": "Job controller is ready to process requests"
}
```

### 2. List All Jobs
**GET** `/api/jobs`

Returns a list of all job runs with optional filtering and pagination.

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `status` (optional): Filter by job status (e.g., "running", "success", "failed")

**Examples:**
```bash
# Get all jobs (first page)
curl -X GET http://localhost:8080/api/jobs

# Get jobs with pagination
curl -X GET "http://localhost:8080/api/jobs?page=0&size=10"

# Filter by status
curl -X GET "http://localhost:8080/api/jobs?status=success"
```

**Response:**
```json
{
  "success": true,
  "jobs": [
    {
      "id": 1,
      "scheduledAt": "2025-07-28T10:00:00",
      "finishedAt": "2025-07-28T10:05:30",
      "status": "success",
      "notes": "Processing S3 files"
    }
  ],
  "totalJobs": 1,
  "page": 0,
  "size": 20
}
```

### 3. Get Job by ID
**GET** `/api/jobs/{jobId}`

Returns a specific job run by its ID.

**Path Parameters:**
- `jobId`: The ID of the job to retrieve

**Example:**
```bash
curl -X GET http://localhost:8080/api/jobs/1
```

**Success Response:**
```json
{
  "success": true,
  "job": {
    "id": 1,
    "scheduledAt": "2025-07-28T10:00:00",
    "finishedAt": "2025-07-28T10:05:30",
    "status": "success",
    "notes": "Processing S3 files"
  }
}
```

**Error Response (404):**
```json
{
  "success": false,
  "error": "Job not found with ID: 999"
}
```

### 4. Run JobRunner Job
**POST** `/api/jobs/run-s3-ingest`

Runs the JobRunner job to process S3 files and extract records.

**Request Body:**
```json
{
  "s3Uri": "s3://bucket-name/",
  "batchSize": 10
}
```

**Parameters:**
- `s3Uri` (optional): The S3 URI to process (e.g., "s3://hotel-reviews")
- `batchSize` (optional): Number of records to process in each batch (default: 10)

**Success Response:**
```json
{
  "success": true,
  "message": "JobRunner job completed successfully",
  "s3Uri": "s3://bucket-name/path/to/files",
  "batchSize": 10
}
```

**Error Response:**
```json
{
  "success": false,
  "error": "S3 URI is required",
  "exception": "IllegalArgumentException"
}
```

### 5. Run RecordProcessor Job
**POST** `/api/jobs/run-record-processor`

Runs the RecordProcessorJob to process extracted records and create reviews, hotels, providers, and reviewers.

**Request Body:**
```json
{
  "pageSize": 10
}
```

**Parameters:**
- `pageSize` (optional): Number of records to process in each page (default: 10)

**Success Response:**
```json
{
  "success": true,
  "message": "RecordProcessorJob completed successfully",
  "pageSize": 10
}
```

**Error Response:**
```json
{
  "success": false,
  "error": "Failed to run RecordProcessorJob: Database connection failed",
  "exception": "SQLException"
}
```

### 6. List All S3 Files
**GET** `/api/jobs/s3-files`

Returns a list of all S3 files with optional filtering and pagination.

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `status` (optional): Filter by file status (e.g., "processing", "success", "failed")
- `jobRunId` (optional): Filter by job run ID

**Examples:**
```bash
# Get all S3 files
curl -X GET http://localhost:8080/api/jobs/s3-files

# Filter by status
curl -X GET "http://localhost:8080/api/jobs/s3-files?status=success"

# Filter by job run ID
curl -X GET "http://localhost:8080/api/jobs/s3-files?jobRunId=1"
```

**Response:**
```json
{
  "success": true,
  "files": [
    {
      "id": 1,
      "s3Key": "hotel-reviews.jsonl",
      "startedAt": "2025-07-28T10:00:00",
      "finishedAt": "2025-07-28T10:05:30",
      "status": "success",
      "recordCount": 1500,
      "errorMessage": null
    }
  ],
  "totalFiles": 1,
  "page": 0,
  "size": 20
}
```

### 7. Get S3 File by ID
**GET** `/api/jobs/s3-files/{fileId}`

Returns a specific S3 file by its ID.

**Path Parameters:**
- `fileId`: The ID of the S3 file to retrieve

**Example:**
```bash
curl -X GET http://localhost:8080/api/jobs/s3-files/1
```

**Success Response:**
```json
{
  "success": true,
  "file": {
    "id": 1,
    "s3Key": "hotel-reviews.jsonl",
    "startedAt": "2025-07-28T10:00:00",
    "finishedAt": "2025-07-28T10:05:30",
    "status": "success",
    "recordCount": 1500,
    "errorMessage": null
  }
}
```

### 8. List All Records
**GET** `/api/jobs/records`

Returns a list of all records with optional filtering and pagination.

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `status` (optional): Filter by record status (e.g., "new", "processing", "success", "failed")
- `jobRunId` (optional): Filter by job run ID
- `s3FileId` (optional): Filter by S3 file ID

**Examples:**
```bash
# Get all records
curl -X GET http://localhost:8080/api/jobs/records

# Filter by status
curl -X GET "http://localhost:8080/api/jobs/records?status=success"

# Filter by job run ID
curl -X GET "http://localhost:8080/api/jobs/records?jobRunId=1"

# Filter by S3 file ID
curl -X GET "http://localhost:8080/api/jobs/records?s3FileId=1"
```

**Response:**
```json
{
  "success": true,
  "records": [
    {
      "id": 1,
      "rawData": "{...}",
      "status": "success",
      "processedAt": "2025-07-28T10:01:00",
      "errorFlag": false
    }
  ],
  "totalRecords": 1,
  "page": 0,
  "size": 20
}
```

### 9. Get Record by ID
**GET** `/api/jobs/records/{recordId}`

Returns a specific record by its ID.

**Path Parameters:**
- `recordId`: The ID of the record to retrieve

**Example:**
```bash
curl -X GET http://localhost:8080/api/jobs/records/1
```

**Success Response:**
```json
{
  "success": true,
  "record": {
    "id": 1,
    "rawData": "{...}",
    "status": "success",
    "processedAt": "2025-07-28T10:01:00",
    "errorFlag": false
  }
}
```

### 10. List All Record Errors
**GET** `/api/jobs/record-errors`

Returns a list of all record errors with optional filtering and pagination.

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `errorType` (optional): Filter by error type (e.g., "PROCESSING_ERROR")
- `recordId` (optional): Filter by record ID

**Examples:**
```bash
# Get all record errors
curl -X GET http://localhost:8080/api/jobs/record-errors

# Filter by error type
curl -X GET "http://localhost:8080/api/jobs/record-errors?errorType=PROCESSING_ERROR"

# Filter by record ID
curl -X GET "http://localhost:8080/api/jobs/record-errors?recordId=1"
```

**Response:**
```json
{
  "success": true,
  "errors": [
    {
      "recordId": 1,
      "errorType": "PROCESSING_ERROR",
      "errorMessage": "Invalid JSON format",
      "traceback": "java.lang.Exception: ..."
    }
  ],
  "totalErrors": 1,
  "page": 0,
  "size": 20
}
```

### 11. Get Record Error by Record ID
**GET** `/api/jobs/record-errors/{recordId}`

Returns a specific record error by record ID.

**Path Parameters:**
- `recordId`: The ID of the record to get error for

**Example:**
```bash
curl -X GET http://localhost:8080/api/jobs/record-errors/1
```

**Success Response:**
```json
{
  "success": true,
  "error": {
    "recordId": 1,
    "errorType": "PROCESSING_ERROR",
    "errorMessage": "Invalid JSON format",
    "traceback": "java.lang.Exception: ..."
  }
}
```

## Usage Examples

### Using curl

1. **Check job status:**
```bash
curl -X GET http://localhost:8080/api/jobs/health
```

2. **List all jobs:**
```bash
curl -X GET http://localhost:8080/api/jobs
```

3. **Get specific job:**
```bash
curl -X GET http://localhost:8080/api/jobs/1
```

4. **Filter jobs by status:**
```bash
curl -X GET "http://localhost:8080/api/jobs?status=success"
```

5. **List all S3 files:**
```bash
curl -X GET http://localhost:8080/api/jobs/s3-files
```

6. **Get specific S3 file:**
```bash
curl -X GET http://localhost:8080/api/jobs/s3-files/1
```

7. **Filter S3 files by status:**
```bash
curl -X GET "http://localhost:8080/api/jobs/s3-files?status=success"
```

8. **List all records:**
```bash
curl -X GET http://localhost:8080/api/jobs/records
```

9. **Get specific record:**
```bash
curl -X GET http://localhost:8080/api/jobs/records/1
```

10. **Filter records by status:**
```bash
curl -X GET "http://localhost:8080/api/jobs/records?status=success"
```

11. **List all record errors:**
```bash
curl -X GET http://localhost:8080/api/jobs/record-errors
```

12. **Get record error by record ID:**
```bash
curl -X GET http://localhost:8080/api/jobs/record-errors/1
```

13. **Run JobRunner job:**
```bash
curl -X POST http://localhost:8080/api/jobs/run-s3-ingest \
  -H "Content-Type: application/json" \
  -d '{
    "s3Uri": "s3://my-bucket/reviews/",
    "batchSize": 20
  }'
```

14. **Run RecordProcessor job:**
```bash
curl -X POST http://localhost:8080/api/jobs/run-record-processor \
  -H "Content-Type: application/json" \
  -d '{
    "pageSize": 15
  }'
```

## Job Workflow

1. **JobRunner Job**: 
   - Processes S3 files containing JSONL data
   - Extracts raw records and stores them in the database
   - Tracks file processing status and job runs

2. **RecordProcessor Job**:
   - Processes the raw records extracted by JobRunner
   - Parses JSON data and creates structured entities
   - Creates/updates hotels, providers, reviewers, and reviews
   - Handles errors and logs them appropriately

## Notes

- Both jobs are synchronous and will block until completion
- Jobs create their own job run records in the database
- Error handling includes detailed error messages and stack traces
- The API supports CORS for cross-origin requests
- All endpoints return JSON responses with consistent structure
- Job listing supports pagination and status filtering 