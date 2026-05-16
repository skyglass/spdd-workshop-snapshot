---
name: spdd-tests
description: Generate SPDD unit and integration tests from a feature structured prompt file. Use when the user invokes $spdd-tests or asks to create unit tests, integration tests, or a [Test] prompt from a [Feat] REASONS Canvas / SPDD prompt.
---

# SPDD Tests

Generate the unit and integration test artifacts for an SPDD feature in one workflow.
The input is a feature implementation prompt, usually:

```text
$spdd-tests @./spdd/prompt/GGQPA-XXX-{timestamp}-[Feat]-api-token-usage-billing.md
```

## Workflow

1. Validate input context.
   - Require one `@` reference to a feature structured prompt, acceptance-criteria document, or API implementation context.
   - If no input is provided, ask for the feature prompt path.
   - If a referenced path contains a timestamp placeholder such as `{timestamp}`, resolve it before reading:
     - Treat `{timestamp}` as a single filename wildcard (`*`) and search for matching files from the current project root.
     - If exactly one file matches, use that file as the referenced input.
     - If no files match, stop and report the unresolved pattern.
     - If multiple files match, stop and ask the user to choose the intended file.
   - Apply the same single-match resolution for a referenced file pattern that contains shell-style wildcards such as `*`, when the user passes a pattern instead of a concrete filename.
   - Read every referenced file completely.

2. Ensure the standard test scenario template exists.
   - Target path: `./spdd/template/TEST-SCENARIOS-TEMPLATE.md`.
   - Create parent directories when needed.
   - If the file already exists, reuse it unless it is clearly incompatible.
   - If absent, create it with sections for controller, service, repository, DAO, model class, and integration test scenarios.
   - The template must require test method names in this format:
     `should_return_[expected_output]_when_[action]_given_[input]`.

3. Generate the SPDD test prompt.
   - Derive the output path by replacing `[Feat]` with `[Test]` in the feature prompt filename.
   - If the input filename does not contain `[Feat]`, write the test prompt under `./spdd/prompt/` with a `[Test]` marker and the same feature slug.
   - Combine the implementation details prompt with the standard template.
   - Inspect existing production code and existing tests before writing scenarios.
   - When existing tests are present, avoid duplicate scenarios and keep only meaningful new coverage.
   - Save the generated test prompt file before generating test code.

4. Generate unit and integration test code from the test prompt.
   - Read the generated `[Test]` prompt completely.
   - Use the project's existing test stack and conventions.
   - For Java/Spring projects, prefer JUnit 5, Mockito, Spring MockMvc, and Spring Boot test patterns already present in the codebase.
   - Cover controller, service, repository/adapter, mapper/DTO/domain model, and integration scenarios when those layers exist.
   - Do not add production behavior to make tests pass. If production behavior appears wrong, report that as an SPDD prompt/code issue instead.
   - Add test-only support configuration only when needed, such as `src/test/resources/application.yml` for isolated test datasource setup.

5. Verify.
   - Run the narrowest useful test command when feasible, commonly `./gradlew test` for this workshop.
   - Fix generated test compilation issues.
   - If tests fail because production behavior disagrees with the structured prompt, stop and report the mismatch instead of silently changing production code.

## Standard Template Content

Use this structure when creating `./spdd/template/TEST-SCENARIOS-TEMPLATE.md`:

```markdown
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
```

## Output

After running the workflow, report:

- Template path created or reused
- Test prompt path created
- Test files created or updated
- Verification command and result
- Any production/prompt mismatch that blocked test generation
