# Review System CLI

A command-line interface for the Review System using PicoCLI framework.

## Building the Application

```bash
mvn clean package
```

## Running the Application

### CLI Mode (with arguments)
```bash
# Using the JAR file
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar [command] [options]

# Using Maven
mvn spring-boot:run -- [command] [options]
mvn spring-boot:run -Dspring-boot.run.arguments="run-job --bucket hotel-reviews"
```

### Web Mode (without arguments)
```bash
# Start as web application
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar

# Or using Maven
mvn spring-boot:run
```

The application automatically detects whether to run in CLI mode (when arguments are provided) or web mode (when no arguments are provided).

## Available Commands

### 1. Run Job
Process reviews from an S3 bucket.

```bash
# Basic usage
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar run-job --bucket hotel-reviews

# With custom options
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar run-job \
  --bucket hotel-reviews \
  --page-size 20 \
  --trigger-type SCHEDULED \
  --notes "Daily processing job"
```

**Options:**
- `-b, --bucket`: S3 bucket name (required)
- `-p, --page-size`: Page size for processing (default: 10)
- `-t, --trigger-type`: Trigger type (default: MANUAL)
- `-n, --notes`: Job notes (default: "CLI triggered job")

### 2. Process Records
Process review records from the database.

```bash
# Basic usage
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar process-records

# With custom options
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar process-records \
  --batch-size 20 \
  --trigger-type SCHEDULED \
  --notes "Daily record processing job"
```

**Options:**
- `-b, --batch-size`: Batch size for processing (default: 10)
- `-t, --trigger-type`: Trigger type (default: MANUAL)
- `-n, --notes`: Job notes (default: "CLI triggered record processing job")

### 3. List Jobs
Display recent job runs.

```bash
# List recent jobs
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar list-jobs

# List with limit
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar list-jobs --limit 5

# Filter by status
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar list-jobs --status SUCCESS
```

**Options:**
- `-l, --limit`: Number of jobs to list (default: 10)
- `-s, --status`: Filter by status (e.g., SUCCESS, FAILED, RUNNING)

### 4. List Files
List files in an S3 bucket.

```bash
# List all files
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar list-files hotel-reviews

# List files with prefix
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar list-files hotel-reviews --prefix agoda
```

**Parameters:**
- `bucket`: S3 bucket name (required)

**Options:**
- `-p, --prefix`: File prefix filter

## Examples

### Process reviews from LocalStack
```bash
# Make sure LocalStack is running on localhost:4566
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar run-job --bucket hotel-reviews
```

### Process records from database
```bash
# Process records in batches of 20
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar process-records --batch-size 20
```

### Check job status
```bash
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar list-jobs --limit 5
```

### Browse S3 files
```bash
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar list-files hotel-reviews
```

### Start as web application
```bash
# Start the web server (no CLI arguments)
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar
```

## Help

Get help for any command:

```bash
# General help
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar --help

# Command-specific help
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar run-job --help
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar list-jobs --help
java -jar target/zuzu-poc-1.0-SNAPSHOT.jar list-files --help
```

## Configuration

The CLI uses the same configuration as the main application:

- Database settings from `application.yml`
- AWS/S3 settings from environment variables or `application.yml`
- For testing, use `application-test.yml` profile

## Environment Variables

Set these environment variables for AWS/LocalStack configuration:

```bash
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_REGION=us-east-1
export AWS_S3_ENDPOINT=http://localhost:4566
export AWS_S3_BUCKET=hotel-reviews
``` 