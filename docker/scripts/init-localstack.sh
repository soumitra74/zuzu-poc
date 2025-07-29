#!/bin/bash

# Initialize LocalStack with S3 bucket and test data
echo "Initializing LocalStack..."

# Wait for LocalStack to be ready
echo "Waiting for LocalStack to be ready..."
until curl -s http://localhost:4566/_localstack/health > /dev/null; do
    echo "Waiting for LocalStack..."
    sleep 2
done

echo "LocalStack is ready!"

# Create S3 bucket
echo "Creating S3 bucket: hotel-reviews"
aws --endpoint-url=http://localhost:4566 s3 mb s3://hotel-reviews --region us-east-1

# Create test data directory
mkdir -p /tmp/test-data

# Create sample JSONL files for testing
cat > /tmp/test-data/sample1.jsonl << EOF
{"comment":{"hotelReviewId":947130812,"reviewerInfo":{"roomTypeId":1,"roomTypeName":"Standard Room","reviewGroupId":1,"reviewGroupName":"Business","lengthOfStay":2}},"hotel":{"hotelId":16402071,"hotelName":"Test Hotel 1"},"provider":{"externalId":332,"providerName":"Agoda"},"reviewer":{"displayName":"Test Reviewer 1","countryName":"Test Country"},"review":{"reviewExternalId":947130812,"ratingRaw":8.8}}
{"comment":{"hotelReviewId":947130813,"reviewerInfo":{"roomTypeId":2,"roomTypeName":"Deluxe Room","reviewGroupId":2,"reviewGroupName":"Leisure","lengthOfStay":3}},"hotel":{"hotelId":16402072,"hotelName":"Test Hotel 2"},"provider":{"externalId":332,"providerName":"Agoda"},"reviewer":{"displayName":"Test Reviewer 2","countryName":"Test Country"},"review":{"reviewExternalId":947130813,"ratingRaw":9.0}}
EOF

cat > /tmp/test-data/sample2.jsonl << EOF
{"comment":{"hotelReviewId":947130814,"reviewerInfo":{"roomTypeId":3,"roomTypeName":"Suite","reviewGroupId":3,"reviewGroupName":"Luxury","lengthOfStay":4}},"hotel":{"hotelId":16402073,"hotelName":"Test Hotel 3"},"provider":{"externalId":332,"providerName":"Agoda"},"reviewer":{"displayName":"Test Reviewer 3","countryName":"Test Country"},"review":{"reviewExternalId":947130814,"ratingRaw":9.5}}
EOF

# Upload test files to S3
echo "Uploading test files to S3..."
aws --endpoint-url=http://localhost:4566 s3 cp /tmp/test-data/sample1.jsonl s3://hotel-reviews/test-prefix/sample1.jsonl --region us-east-1
aws --endpoint-url=http://localhost:4566 s3 cp /tmp/test-data/sample2.jsonl s3://hotel-reviews/test-prefix/sample2.jsonl --region us-east-1

# List files in bucket
echo "Files in S3 bucket:"
aws --endpoint-url=http://localhost:4566 s3 ls s3://hotel-reviews/test-prefix/ --recursive --region us-east-1

echo "LocalStack initialization complete!" 