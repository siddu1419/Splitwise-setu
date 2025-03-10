# Expense Sharing Application

A Spring Boot application that allows users to create groups, add members, and split expenses among group members. The application supports different types of expense splits (Equal, Unequal, and Percentage-based).

## Features

- User registration and authentication
- Group creation and management
- Expense creation with multiple split types
- Expense settlement tracking
- Comprehensive test coverage
- Secure API endpoints with JWT authentication

## Technology Stack

- Spring Boot 3.x
- Spring Security with JWT
- Spring Data JPA
- PostgreSQL Database
- JUnit 5 for testing
- Mockito for mocking dependencies

## Authentication & Authorization

### JWT Authentication Flow

1. **Registration**: Users register with email and password
   - Password is encrypted using BCrypt
   - Email uniqueness is validated

2. **Login**: Users authenticate with email and password
   - JWT token is generated upon successful authentication
   - Token contains user ID and email
   - Token is valid for 24 hours

3. **Security Filters**:
   - `JwtAuthenticationFilter`: Intercepts requests to validate JWT tokens
   - `UsernamePasswordAuthenticationFilter`: Handles login requests
   - `ExceptionHandlerFilter`: Handles authentication/authorization exceptions

4. **Authorization**:
   - Role-based access control (USER role)
   - Group membership validation for expense operations
   - User-specific data access restrictions

## Application Flow

### 1. User Management

#### Registration
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "name": "John Doe"
  }'
```

#### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

### 2. Group Management

#### Create Group
```bash
curl -X POST http://localhost:8080/api/groups \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {jwt_token}" \
  -d '{
    "name": "Vacation Group",
    "description": "Group for vacation expenses"
  }'
```

#### Add Member to Group
```bash
curl -X POST http://localhost:8080/api/groups/{groupId}/members \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {jwt_token}" \
  -d '{
    "userId": 2
  }'
```

#### Get User's Groups
```bash
curl -X GET http://localhost:8080/api/groups \
  -H "Authorization: Bearer {jwt_token}"
```

### 3. Expense Management

#### Create Expense (Equal Split)
```bash
curl -X POST http://localhost:8080/api/expenses/groups/{groupId} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {jwt_token}" \
  -d '{
    "description": "Dinner",
    "amount": 100.00,
    "splitType": "EQUAL",
    "shares": [
      {"userId": 1},
      {"userId": 2}
    ]
  }'
```


#### Get Group Expenses
```bash
curl -X GET http://localhost:8080/api/expenses/groups/{groupId} \
  -H "Authorization: Bearer {jwt_token}"
```

#### Get User's Expenses
```bash
curl -X GET http://localhost:8080/api/expenses/me \
  -H "Authorization: Bearer {jwt_token}"
```

#### Settle Expense Share
```bash
curl -X POST http://localhost:8080/api/expenses/shares/{shareId}/settle \
  -H "Authorization: Bearer {jwt_token}"
```

## Testing

The application includes comprehensive test coverage:

### Unit Tests
- `UserServiceTest`: Tests user registration, authentication, and management
- `GroupServiceTest`: Tests group creation and member management
- `ExpenseServiceTest`: Tests expense creation and management with different split types

### Test Coverage
- Service layer: 90%+ coverage
- Controller layer: 85%+ coverage
- Repository layer: 100% coverage

### Test Categories
1. **Happy Path Tests**
   - Successful user registration
   - Successful group creation
   - Successful expense creation with different split types
   - Successful expense settlement

2. **Validation Tests**
   - Invalid email format
   - Duplicate email registration
   - Invalid expense amounts
   - Invalid split percentages
   - Non-member group access

3. **Error Handling Tests**
   - Resource not found scenarios
   - Invalid input scenarios
   - Authorization failure scenarios

## Database Schema

### Users
- id (PK)
- email (unique)
- password (encrypted)
- name
- created_at

### Groups
- id (PK)
- name
- description
- created_by (FK to Users)
- created_at

### Group Members
- group_id (FK to Groups)
- user_id (FK to Users)

### Expenses
- id (PK)
- description
- amount
- group_id (FK to Groups)
- paid_by_id (FK to Users)
- split_type (EQUAL, UNEQUAL, PERCENTAGE)
- date
- created_at

### Expense Shares
- id (PK)
- expense_id (FK to Expenses)
- user_id (FK to Users)
- share_amount
- percentage
- settled
- settled_at

## Why PostgreSQL?

We chose PostgreSQL for this application for several key reasons:

1. **ACID Compliance**
   - Full ACID (Atomicity, Consistency, Isolation, Durability) compliance
   - Critical for financial transactions and expense tracking
   - Ensures data integrity in concurrent operations

2. **Complex Queries**
   - Superior handling of complex JOIN operations
   - Efficient for expense calculations and group summaries
   - Better performance with relationship-heavy schemas

3. **JSON Support**
   - Native JSON and JSONB data types
   - Useful for storing flexible expense metadata
   - Better performance than MySQL for JSON operations

4. **Scalability**
   - Excellent performance with large datasets
   - Better read/write performance for our use case
   - Efficient handling of concurrent users

5. **Extensions**
   - Rich ecosystem of extensions
   - Built-in full-text search capabilities
   - Advanced indexing options

6. **Open Source**
   - Active community support
   - No licensing costs
   - Regular security updates

## Getting Started

### Prerequisites
- JDK 17 or higher
- PostgreSQL 12 or higher
- Gradle 7.x
- Git

### Local Setup

1. **Clone the Repository**
   ```bash
   git clone https://github.com/yourusername/expense-sharing-app.git
   cd expense-sharing-app
   ```

2. **Configure PostgreSQL**
   ```bash
   # Login to PostgreSQL
   psql -U postgres

   # Create database
   CREATE DATABASE expense_sharing;

   # Create user
   CREATE USER expense_user WITH PASSWORD 'your_password';

   # Grant privileges
   GRANT ALL PRIVILEGES ON DATABASE expense_sharing TO expense_user;
   ```

3. **Configure Application**
   
   Create `src/main/resources/application.properties`:
   ```properties
   # Database Configuration
   spring.datasource.url=jdbc:postgresql://localhost:5432/expense_sharing
   spring.datasource.username=expense_user
   spring.datasource.password=your_password
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

   # JWT Configuration
   jwt.secret=your_jwt_secret_key
   jwt.expiration=86400000

   # Server Configuration
   server.port=8080
   ```

4. **Initialize Database with Sample Data (Optional)**
   
   Run the seed script:
   ```bash
   psql -U expense_user -d expense_sharing -f src/main/resources/db/seed.sql
   ```

   Sample `seed.sql`:
   ```sql
   -- Create test users
   INSERT INTO users (email, password, name, created_at) 
   VALUES 
   ('test1@example.com', '$2a$10$yourhashedpassword', 'Test User 1', NOW()),
   ('test2@example.com', '$2a$10$yourhashedpassword', 'Test User 2', NOW());

   -- Create test group
   INSERT INTO groups (name, description, created_by, created_at)
   VALUES ('Test Group', 'Group for testing', 1, NOW());

   -- Add users to group
   INSERT INTO group_members (group_id, user_id)
   VALUES (1, 1), (1, 2);
   ```

5. **Build the Application**
   ```bash
   ./gradlew clean build
   ```

6. **Run the Application**
   ```bash
   ./gradlew bootRun
   ```

7. **Verify Installation**
   ```bash
   curl http://localhost:8080/api/health
   ```

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.example.demo.service.ExpenseServiceTest"

# Generate test coverage report
./gradlew jacocoTestReport
```

### Development Setup

1. **IDE Configuration**
   - Import as Gradle project
   - Enable annotation processing
   - Install Lombok plugin

2. **Recommended Tools**
   - Postman for API testing
   - pgAdmin for database management
   - JaCoCo for test coverage analysis

3. **Code Style**
   - Follow Google Java Style Guide
   - Use provided checkstyle configuration
   ```bash
   ./gradlew checkstyleMain
   ```

### Troubleshooting

1. **Database Connection Issues**
   ```bash
   # Check PostgreSQL status
   sudo service postgresql status

   # Verify connection
   psql -U expense_user -d expense_sharing -h localhost
   ```

2. **Application Startup Issues**
   - Check logs in `logs/application.log`
   - Verify database credentials
   - Ensure correct Java version

3. **Common Errors**
   - Port 8080 already in use: Change port in application.properties
   - Database migration failed: Check schema version
   - Authentication failed: Verify JWT secret key

## Security Considerations

1. **Password Security**
   - Passwords are hashed using BCrypt
   - Minimum password length enforced
   - Password complexity requirements

2. **JWT Security**
   - Tokens expire after 24 hours
   - Refresh token mechanism
   - Token blacklisting for logout

3. **API Security**
   - All endpoints except registration and login require authentication
   - CORS configuration
   - Rate limiting
   - Input validation and sanitization

4. **Data Security**
   - Users can only access their own data
   - Group members can only access group data
   - SQL injection prevention
   - XSS prevention

