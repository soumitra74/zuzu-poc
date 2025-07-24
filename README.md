# Review System Microservice

This microservice retrieves Agoda.com / Booking.com / Expedia reviews from an AWS S3 bucket, processes the data, and stores it in a relational database (e.g., PostgreSQL).

## Features
- Periodically pulls review files from S3
- Parses and processes JSON Lines (.jl) review data
- Stores processed reviews in a relational database

## Project Structure

```
review-system-microservice/
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
│   │   │               ├── repository/
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

