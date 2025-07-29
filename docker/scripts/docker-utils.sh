#!/bin/bash

# Docker utility functions for the hotel reviews microservice

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
    print_success "Docker is running"
}

# Function to build the application
build_app() {
    print_status "Building the application..."
    docker-compose build app
    print_success "Application built successfully"
}

# Function to start all services
start_services() {
    print_status "Starting all services..."
    docker-compose up -d
    print_success "Services started successfully"
}

# Function to stop all services
stop_services() {
    print_status "Stopping all services..."
    docker-compose down
    print_success "Services stopped successfully"
}

# Function to restart all services
restart_services() {
    print_status "Restarting all services..."
    docker-compose down
    docker-compose up -d
    print_success "Services restarted successfully"
}

# Function to view logs
view_logs() {
    local service=${1:-app}
    print_status "Viewing logs for service: $service"
    docker-compose logs -f $service
}

# Function to check service health
check_health() {
    print_status "Checking service health..."
    
    # Check if services are running
    if docker-compose ps | grep -q "Up"; then
        print_success "All services are running"
    else
        print_error "Some services are not running"
        docker-compose ps
        exit 1
    fi
    
    # Check database connection
    if docker-compose exec -T db pg_isready -U postgres -d hotel_reviews > /dev/null 2>&1; then
        print_success "Database is healthy"
    else
        print_error "Database is not healthy"
        exit 1
    fi
    
    # Check application health
    if curl -f http://localhost:8080/api/jobs/health > /dev/null 2>&1; then
        print_success "Application is healthy"
    else
        print_warning "Application health check failed (may still be starting)"
    fi
}

# Function to initialize LocalStack
init_localstack() {
    print_status "Initializing LocalStack..."
    docker-compose exec localstack /bin/bash -c "
        # Wait for LocalStack to be ready
        until curl -s http://localhost:4566/_localstack/health > /dev/null; do
            echo 'Waiting for LocalStack...'
            sleep 2
        done
        
        # Create S3 bucket
        aws --endpoint-url=http://localhost:4566 s3 mb s3://hotel-reviews --region us-east-1
        
        # Create test data
        mkdir -p /tmp/test-data
        
        cat > /tmp/test-data/sample1.jsonl << 'EOF'
{\"comment\":{\"hotelReviewId\":947130812,\"reviewerInfo\":{\"roomTypeId\":1,\"roomTypeName\":\"Standard Room\",\"reviewGroupId\":1,\"reviewGroupName\":\"Business\",\"lengthOfStay\":2}},\"hotel\":{\"hotelId\":16402071,\"hotelName\":\"Test Hotel 1\"},\"provider\":{\"externalId\":332,\"providerName\":\"Agoda\"},\"reviewer\":{\"displayName\":\"Test Reviewer 1\",\"countryName\":\"Test Country\"},\"review\":{\"reviewExternalId\":947130812,\"ratingRaw\":8.8}}
EOF
        
        # Upload test file
        aws --endpoint-url=http://localhost:4566 s3 cp /tmp/test-data/sample1.jsonl s3://hotel-reviews/test-prefix/sample1.jsonl --region us-east-1
        
        echo 'LocalStack initialized successfully'
    "
    print_success "LocalStack initialized successfully"
}

# Function to run tests
run_tests() {
    print_status "Running tests..."
    docker-compose exec app mvn test
    print_success "Tests completed"
}

# Function to clean up
cleanup() {
    print_status "Cleaning up Docker resources..."
    docker-compose down -v --remove-orphans
    docker system prune -f
    print_success "Cleanup completed"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  build       - Build the application"
    echo "  start       - Start all services"
    echo "  stop        - Stop all services"
    echo "  restart     - Restart all services"
    echo "  logs [SVC]  - View logs (default: app)"
    echo "  health      - Check service health"
    echo "  init-s3     - Initialize LocalStack with S3 bucket"
    echo "  test        - Run tests"
    echo "  cleanup     - Clean up Docker resources"
    echo "  help        - Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 start"
    echo "  $0 logs db"
    echo "  $0 health"
}

# Main script logic
case "${1:-help}" in
    build)
        check_docker
        build_app
        ;;
    start)
        check_docker
        start_services
        ;;
    stop)
        check_docker
        stop_services
        ;;
    restart)
        check_docker
        restart_services
        ;;
    logs)
        check_docker
        view_logs "$2"
        ;;
    health)
        check_docker
        check_health
        ;;
    init-s3)
        check_docker
        init_localstack
        ;;
    test)
        check_docker
        run_tests
        ;;
    cleanup)
        check_docker
        cleanup
        ;;
    help|--help|-h)
        show_usage
        ;;
    *)
        print_error "Unknown command: $1"
        show_usage
        exit 1
        ;;
esac 