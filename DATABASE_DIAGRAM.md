# Hotel Review System Database Diagram

## Entity Relationship Diagram (ERD)

```mermaid
erDiagram
    %% API Authentication
    api_keys {
        bigint id PK
        varchar api_key UK
        varchar name
        varchar role
        text permissions
        boolean is_active
        timestamp created_at
        timestamp last_used_at
        timestamp expires_at
    }

    %% Job and File Tracking
    job_runs {
        serial id PK
        timestamp scheduled_at
        timestamp finished_at
        text status
        text notes
    }

    s3_files {
        serial id PK
        integer job_run_id FK
        text s3_key UK
        timestamp started_at
        timestamp finished_at
        integer record_count
        integer page_number
        text status
        text error_message
    }

    records {
        serial id PK
        integer s3_file_id FK
        integer job_run_id FK
        text raw_data
        text status
        timestamp downloaded_at
        timestamp started_at
        timestamp finished_at
        boolean error_flag
    }

    record_errors {
        integer record_id PK,FK
        text error_type
        text error_message
        text traceback
    }

    %% Reference/Lookup Tables
    provider {
        smallint provider_id PK
        smallint external_id UK
        text provider_name
    }

    rating_category {
        smallint category_id PK
        text category_name UK
    }

    %% Core Entity Tables
    hotel {
        integer hotel_id PK
        integer external_id
        smallint provider_id FK
        text hotel_name
    }

    reviewer {
        bigint reviewer_id PK
        text display_name
        smallint provider_id FK
        integer country_id
        varchar country_name
        char flag_code
        boolean is_expert
        integer reviews_written
    }

    %% Review-specific Tables
    review {
        bigint review_id PK
        bigint review_external_id
        integer hotel_id FK
        smallint provider_id FK
        bigint reviewer_id FK
        numeric rating
        text rating_text
        varchar rating_formatted
        text review_title
        text review_comment
        integer review_vote_positive
        integer review_vote_negative
        timestamp review_date
        varchar translate_source
        varchar translate_target
        boolean is_response_shown
        text responder_name
        text response_text
        text response_date_text
        text response_date_fmt
        varchar check_in_month_yr
    }

    stay_info {
        bigint review_id PK,FK
        integer room_type_id
        text room_type_name
        integer review_group_id
        varchar review_group_name
        smallint length_of_stay
    }

    %% Aggregated Metrics Tables
    provider_hotel_summary {
        integer hotel_id PK,FK
        smallint provider_id PK,FK
        bigint review_id PK,FK
        numeric overall_score
        integer review_count
    }

    provider_hotel_grade {
        integer hotel_id PK,FK
        smallint provider_id PK,FK
        smallint category_id PK,FK
        bigint review_id PK,FK
        numeric grade_value
    }

    %% Relationships
    job_runs ||--o{ s3_files : "has"
    job_runs ||--o{ records : "processes"
    s3_files ||--o{ records : "contains"
    records ||--o| record_errors : "has"
    
    provider ||--o{ hotel : "provides"
    provider ||--o{ reviewer : "has"
    provider ||--o{ review : "publishes"
    
    hotel ||--o{ review : "receives"
    reviewer ||--o{ review : "writes"
    
    review ||--o| stay_info : "includes"
    review ||--o{ provider_hotel_summary : "contributes_to"
    review ||--o{ provider_hotel_grade : "contributes_to"
    
    hotel ||--o{ provider_hotel_summary : "has_summary"
    hotel ||--o{ provider_hotel_grade : "has_grades"
    
    rating_category ||--o{ provider_hotel_grade : "categorizes"
```

## Database Schema Overview

### 1. **Authentication & Authorization**
- **`api_keys`**: Manages API key authentication with role-based access control
  - Supports different roles: ADMIN, OPERATOR, VIEWER, EXECUTOR
  - Tracks key usage and expiration

### 2. **Job & File Tracking System**
- **`job_runs`**: Tracks job execution lifecycle
  - Status: pending, running, success, failed
  - Timestamps for scheduling and completion

- **`s3_files`**: Manages S3 file processing
  - Links to job runs
  - Tracks processing status and record counts
  - Stores error messages for failed files

- **`records`**: Individual JSON records from S3 files
  - Links to both S3 files and job runs
  - Tracks processing status with timestamps
  - Stores raw JSON data

- **`record_errors`**: Error tracking for failed records
  - One-to-one relationship with records
  - Stores error type, message, and stack trace

### 3. **Core Business Entities**

#### **Reference Tables**
- **`provider`**: Hotel review providers (Agoda, Booking.com, etc.)
- **`rating_category`**: Rating categories (Cleanliness, Facilities, etc.)

#### **Core Entities**
- **`hotel`**: Hotel information with provider relationship
- **`reviewer`**: User information with provider relationship
- **`review`**: Individual hotel reviews
- **`stay_info`**: Additional stay details (room type, group, length)

#### **Aggregated Metrics**
- **`provider_hotel_summary`**: Overall hotel scores per provider
- **`provider_hotel_grade`**: Detailed category grades per hotel/provider

## Key Features

### **Data Integrity**
- Foreign key constraints ensure referential integrity
- Unique constraints prevent duplicate data
- Check constraints validate status values

### **Performance Optimization**
- Indexes on frequently queried columns
- Composite primary keys for aggregated tables
- Efficient relationship design

### **Scalability**
- Identity columns for auto-incrementing IDs
- Proper data types for different value ranges
- Flexible text fields for variable content

### **Audit Trail**
- Comprehensive timestamp tracking
- Error logging and debugging support
- Job execution history

## Usage Patterns

### **Data Ingestion Flow**
1. `job_runs` → `s3_files` → `records` → `record_errors` (if failures)
2. Records processed into: `hotel`, `reviewer`, `review`, `stay_info`
3. Aggregated into: `provider_hotel_summary`, `provider_hotel_grade`

### **Query Patterns**
- Hotel reviews by provider
- Reviewer activity tracking
- Error analysis and debugging
- Performance metrics aggregation
- API key authentication and authorization

This database design supports a comprehensive hotel review system with robust job processing, error handling, and role-based access control.