# Test Scenarios for `[Feature Name]`

## 1. `[Controller Name]` Test Scenarios
### Create `[ControllerTestClassName]` class
1. Create `[ControllerTestClassName]` class
2. Use the project's controller test pattern, such as `@WebMvcTest` and `MockMvc` for Spring MVC
3. Mock the service layer
4. Generate test scenarios for controller request validation, response mapping, status codes, and error handling

#### `[testMethodName]`
- Description: `[Test description]`
- Input: `[Input description]`
- Expected Output: `[Expected output description]`
- Verification Points:
  - `[Verification point 1]`
  - `[Verification point 2]`

## 2. `[Service Name]` Test Scenarios
### Create `[ServiceTestClassName]` class
1. Create `[ServiceTestClassName]` class
2. Mock repository, adapter, factory, or collaborator dependencies
3. Generate test scenarios for business rules, error paths, edge cases, and collaborator calls

#### `[testMethodName]`
- Description: `[Test description]`
- Input: `[Input description]`
- Expected Output: `[Expected output description]`
- Verification Points:
  - `[Verification point 1]`
  - `[Verification point 2]`

## 3. `[Repository Name]` Test Scenarios
### Create `[RepositoryTestClassName]` class
1. Create `[RepositoryTestClassName]` class
2. Use the repository or adapter testing pattern already used by the project
3. Generate test scenarios for persistence mapping, queries, empty results, and errors

#### `[testMethodName]`
- Description: `[Test description]`
- Input: `[Input description]`
- Expected Output: `[Expected output description]`
- Verification Points:
  - `[Verification point 1]`
  - `[Verification point 2]`

## 4. `[DAO Name]` Test Scenarios
### Create `[DAOTestClassName]` class
1. Create `[DAOTestClassName]` class when the project has DAO classes
2. Use the DAO testing pattern already used by the project
3. Generate test scenarios for DAO-specific queries and persistence behavior

#### `[testMethodName]`
- Description: `[Test description]`
- Input: `[Input description]`
- Expected Output: `[Expected output description]`
- Verification Points:
  - `[Verification point 1]`
  - `[Verification point 2]`

## 5. Model Class Test Scenarios
### Create `[ModelTestClassName]` class
1. Create `[ModelTestClassName]` class
2. Generate test scenarios for constructors, factory methods, calculations, invariants, and DTO/domain mapping

#### `[testMethodName]`
- Description: `[Test description]`
- Input: `[Input description]`
- Expected Output: `[Expected output description]`
- Verification Points:
  - `[Verification point 1]`
  - `[Verification point 2]`

## 6. Integration Test Scenarios
### Create `[ControllerIntegrationTestClassName]` class
1. Create `[ControllerIntegrationTestClassName]` class
2. Use the project's integration test pattern, such as `@SpringBootTest` and `@AutoConfigureMockMvc`
3. Generate end-to-end scenarios for the feature's acceptance criteria

#### `[testMethodName]`
- Description: `[Test description]`
- Input: `[Input description]`
- Expected Output: `[Expected output description]`
- Verification Points:
  - `[Verification point 1]`
  - `[Verification point 2]`

## 7. Constraints
- Test name should follow the format: `should_return_[expected_output]_when_[action]_given_[input]`
- Prefer deterministic test data.
- Keep tests focused on behavior described by the feature prompt.
- Avoid duplicate scenarios already covered by existing tests.
