Android & Kotlin Multiplatform SDK Design - Copilot Instructions Template
Purpose: This file demonstrates Copilot instruction best practices while serving as a copy-ready template for teams building SDKs.

How to Use: Copy this file to .github/copilot-instructions.md in your repository. Customize for your specific project following the 3-layer structure (System Rules → Context → Specific Guidance).

LAYER 1: SYSTEM RULES (Critical, Non-Negotiable)
These rules apply universally and must not be overridden.

API Design: Naming Conventions (Critical)
All public methods and classes must follow industry standard naming patterns:

Method Naming Rule: [Verb][Object] Pattern

✅ Correct: createUser(), listUsers(), deleteUser(), updateEmail(), subscribeToChanges()

❌ Wrong: petCreate() (wrong order), getUsers() (for collections, use list), getUsersFiltered() (unclear)

Verb options: create, list, get, update, delete, fetch, subscribe, observe

Max length: 30 characters

Reason: Enables IDE autocomplete, predictable for developers, follows JetBrains standards

Class Naming Rule: Singular Nouns, Plain English

✅ Correct: User, Transaction, AuthenticationResponse, NetworkException

❌ Wrong: Users (plural), UserObj (suffix), UserData (generic), Impl suffix

Reason: Matches Kotlin conventions, used by Firebase, Retrofit, OkHttp

Package Organization: Domain-Based, Not Function-Based

✅ Correct: com.example.users, com.example.auth, com.example.analytics

❌ Wrong: com.example.models, com.example.responses, com.example.exceptions

Depth: Keep 2-3 levels max

Reason: Developers intuitively find what they need

Error Handling (Critical)
Rule 1: Use Sealed Classes for Result Types
All normal success/failure paths must use sealed classes, NOT exceptions:

kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val exception: Exception) : Result<T>()
    class Loading<T> : Result<T>()
}
Benefits: Compile-time type safety, no exception overhead for expected failures, Compose-friendly.

Rule 2: Specific Exception Types Only

✅ Correct: NetworkException, AuthenticationException, RateLimitException, ValidationException

❌ Wrong: Exception, RuntimeException, Error

Each exception must include: error message, context (URL, status code, retry info), cause

kotlin
class RateLimitException(
    message: String,
    val retryAfterMs: Long,
    cause: Throwable? = null
) : SdkException(message, cause)
Rule 3: All Public Methods Must Document Exceptions

kotlin
/**
 * @throws NetworkException If a network error occurs (retryable).
 * @throws AuthenticationException If the API key is invalid.
 * @throws NotFoundException If the resource doesn't exist.
 */
suspend fun getUser(userId: String): User
Modularity: Visibility Control (Critical)
Rule: Mark Everything Internal by Default

Implementation classes: internal class UserClientImpl

Data sources: internal interface RemoteDataSource

Mappers/converters: internal class UserMapper

Only export stable public interfaces and models

Gradle Dependency Scoping:

api(): Only for public interfaces users must depend on

implementation(): For internal dependencies not exposed to consumers

Example:

text
dependencies {
    api(project(":core"))                    // Public API
    implementation("com.squareup.okhttp:okhttp:4.11.0")  // Internal—users can't access
}
LAYER 2: PROJECT CONTEXT & TECH STACK
What We're Building
Android and Kotlin Multiplatform SDKs following production patterns from:

Retrofit (REST client, 40%+ of Android apps)

Firebase Android SDK (used by 3M+ apps)

OkHttp (HTTP foundation)

Square SDKs (Retrofit, OkHttp, Molecule)

JetBrains (Ktor, kotlinx)

Technology Stack
Core Stack:

Kotlin 1.9+ with coroutines + Flow

Jetpack Compose (primary UI framework when needed)

Retrofit + OkHttp (networking)

Kotlin Multiplatform (androidMain, iosMain, commonMain)

Testing:

Unit tests: Kotlin Test, JUnit

Integration: Mock HTTP server

Samples: basic/, advanced/, multiplatform/ folders

Documentation:

KDoc with examples

README (90-second quick start)

Migration guides per major version

Troubleshooting FAQ

Key References
Main Guide: sdk_design_copilot.md

Skills Framework: sdk_skills_expertise.md

Quick Reference: sdk_quick_reference.md

LAYER 3: SPECIFIC GUIDANCE BY TASK
Task 1: Designing a New API Method
When: Adding a new public API to your SDK

Do This:

Start with verb: create, list, get, update, delete, subscribe

Add specific object name

Use suspend (not callbacks)

Document exceptions

Provide example in KDoc

Example:

kotlin
/**
 * Fetches a user by ID.
 *
 * The result is cached for 5 minutes.
 *
 * @param userId The unique identifier of the user.
 * @return The requested user.
 * @throws NotFoundException If the user doesn't exist.
 * @throws AuthenticationException If the API key is invalid.
 *
 * @example
 * ```kotlin
 * val user = client.users.getUser("user_123")
 * ```
 */
suspend fun getUser(userId: String): User
Checklist:

 Verb + object naming

 Single responsibility (does one thing well)

 Suspend function (not callback)

 Public methods documented with KDoc

 Exceptions listed

 Example provided

 Under 30 characters

Task 2: Designing Error Handling
When: A method can fail (network, validation, auth, etc.)

Do This:

Create sealed Result class with Success/Error/Loading

Define specific exception types

Include context in exceptions

Document all exceptions

Example:

kotlin
// Error handling
sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error<T>(
        val message: String,
        val statusCode: Int? = null,
        val isRetryable: Boolean = false
    ) : ApiResponse<T>()
    class Loading<T> : ApiResponse<T>()
}

// Specific exceptions
class RateLimitException(
    message: String,
    val retryAfterMs: Long
) : SdkException(message)

// Usage in UI (Compose)
when (val result = apiClient.getUser(id)) {
    is ApiResponse.Success -> updateUI(result.data)
    is ApiResponse.Error -> showError(result.message, result.isRetryable)
    is ApiResponse.Loading -> showLoader()
}
Checklist:

 Sealed classes used for result types

 Specific exception types defined

 Context included (error code, retry info)

 All public methods document exceptions

 Error messages are actionable

Task 3: Configuring Complex SDKs
When: Your SDK has 3+ configuration parameters

Do This:

Create Builder class for configuration

Use fluent API (method chaining)

Provide sensible defaults

Only required params: critical ones (API key)

Make builder immutable

Example:

kotlin
class SdkClient private constructor(
    val baseUrl: String,
    val timeout: Duration,
    val interceptors: List<Interceptor>,
    val cache: Cache?
) {
    class Builder {
        private var baseUrl: String = "https://api.example.com"  // Default
        private var timeout: Duration = 30.seconds              // Default
        private var interceptors: MutableList<Interceptor> = mutableListOf()
        private var cache: Cache? = null                        // Optional

        fun baseUrl(url: String) = apply { this.baseUrl = url }
        fun timeout(duration: Duration) = apply { this.timeout = duration }
        fun addInterceptor(interceptor: Interceptor) = apply { 
            interceptors.add(interceptor) 
        }
        fun cache(cache: Cache) = apply { this.cache = cache }

        fun build(): SdkClient = SdkClient(
            baseUrl = baseUrl,
            timeout = timeout,
            interceptors = interceptors.toList(),
            cache = cache
        )
    }
}

// Usage
val client = SdkClient.Builder()
    .baseUrl("https://custom.com")
    .timeout(60.seconds)
    .addInterceptor(loggingInterceptor)
    .build()
Checklist:

 Builder pattern used for 3+ params

 Fluent API (method returns this)

 Sensible defaults provided

 Only critical params required

 Builder is immutable after build()

Task 4: Kotlin Multiplatform Implementation
When: Implementing SDK on both Android and iOS

Do This:

Common interface in commonMain

Platform-specific actual implementations

Use delegation pattern for native SDK wrapping

Keep public API identical across platforms

Example Structure:

text
commonMain/
  User.kt           # data class User
  UserManager.kt    # interface UserManager (expect)

androidMain/
  UserManager.kt    # actual implementation using Firebase Auth
  
iosMain/
  UserManager.kt    # actual implementation using Swift auth
Example Code:

kotlin
// commonMain
interface UserManager {
    suspend fun getUser(): User
}

// androidMain
actual class UserManager(private val firebaseAuth: FirebaseAuth) : UserManager {
    override suspend fun getUser(): User = suspendCancellableCoroutine { cont ->
        firebaseAuth.currentUser?.let { user ->
            cont.resume(user.toKmpUser())
        } ?: cont.resumeWithException(AuthenticationException("Not logged in"))
    }
}

// iosMain
actual class UserManager(private val swiftAuth: SwiftAuth) : UserManager {
    override suspend fun getUser(): User = suspendCancellableCoroutine { cont ->
        swiftAuth.getCurrentUser { swiftUser, error in
            if let error = error {
                cont.resumeWithException(error)
            } else {
                cont.resume(swiftUser.toKmpUser())
            }
        }
    }
}
NON Negotiable: think deeper not over the surface for every question asked everylin written 
Checklist:

 Common interface in commonMain

 Actual implementations per platform

 Public API identical across platforms

 Implementation details hidden in actual declarations

Task 5: Jetpack Compose Integration
When: SDK will be used in Compose apps

Do This:

Expose data as StateFlow, not callbacks

Model errors as immutable state, not exceptions

Provide mock implementations for previews

Use sealed classes for comprehensive type safety

Example:

kotlin
// Immutable state
data class UserUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

// StateFlow for Compose
class UserViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    fun getUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = UserUiState(isLoading = true)
            try {
                val user = userRepository.getUser(userId)
                _uiState.value = UserUiState(user = user)
            } catch (e: Exception) {
                _uiState.value = UserUiState(error = e.message)
            }
        }
    }
}

// Compose integration
@Composable
fun UserScreen(viewModel: UserViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        uiState.isLoading -> LoadingScreen()
        uiState.error != null -> ErrorScreen(uiState.error)
        uiState.user != null -> UserContent(uiState.user!!)
    }
}

// Mock for previews
object MockUserRepository : UserRepository {
    override suspend fun getUser(id: String): User = User(
        id = "1",
        name = "John Doe",
        email = "john@example.com"
    )
}

@Preview
@Composable
fun UserScreenPreview() {
    UserScreen(UserViewModel(MockUserRepository))
}
Checklist:

 Data exposed as StateFlow, not callbacks

 Errors modeled as state

 Mock implementations provided

 Uses collectAsStateWithLifecycle (not collectAsState)

 Immutable state classes

Task 6: Versioning & Breaking Changes
When: Releasing a major version or deprecating an API

Do This:

Follow Semantic Versioning: MAJOR.MINOR.PATCH

Deprecate before removing (12-month timeline)

Document breaking changes in CHANGELOG

Provide migration guides

Example:

kotlin
// v1.0.0 - Old API
fun getUserById(id: String): User

// v1.1.0 - Introduce new, deprecate old
@Deprecated(
    message = "Use getUser(id) instead. Will be removed in v3.0.0",
    replaceWith = ReplaceWith("getUser(id)"),
    level = DeprecationLevel.WARNING
)
fun getUserById(id: String): User = getUser(id)

fun getUser(id: String): User  // Recommended

// v2.0.0 - Error level
@Deprecated(
    message = "Use getUser(id) instead. Removal in v3.0.0",
    replaceWith = ReplaceWith("getUser(id)"),
    level = DeprecationLevel.ERROR
)
fun getUserById(id: String): User = getUser(id)

// v3.0.0 - Remove entirely
// getUserById is gone
CHANGELOG Example:

text
## [2.0.0] - 2024-02-01

### Breaking Changes
- Removed `getUserById(id: String)` - use `getUser(id: String)` instead
- Changed `UpdateUserRequest` constructor signature

### New Features
- Added `getUsers(filter: UserFilter)` for batch operations
- Added `subscribeToUserChanges(id: String): Flow<UserUpdate>`

### Migration Guide
See docs/migration/1-to-2.md
Checklist:

 Semantic versioning (MAJOR.MINOR.PATCH)

 12-month deprecation timeline

 @Deprecated annotations with replacement suggestions

 CHANGELOG documents breaking changes

 Migration guide provided

OUTPUT FORMAT SPECIFICATION
For Code Generation:

Language: Kotlin 1.9+

Format: Follow existing patterns in codebase

Compilation: Must compile without warnings

Tests: Include basic unit tests if applicable

For Documentation:

Format: Markdown with proper code blocks

Code examples: Must be compilable, follow actual API

Audience: Android/Kotlin developers with 2+ years experience

Length: Concise but complete

For Architecture Decisions:

Include: Problem, constraints, decision, rationale, alternatives

Format: Markdown ADR (Architecture Decision Record)

Audience: Other architects and senior engineers

CRITICAL REMINDERS (DO NOT BYPASS)
Always:
✅ Use sealed classes for error handling

✅ Make implementation internal

✅ Document public APIs with KDoc

✅ Name methods with [Verb][Object] pattern

✅ Provide examples in documentation

✅ Support backward compatibility when possible

✅ Follow 12-month deprecation timeline

Never:
❌ Throw generic Exception

❌ Expose implementation details

❌ Break APIs without 12-month notice

❌ Use callbacks when suspend functions work

❌ Forget to document exceptions

❌ Mix function-based and domain-based package organization

❌ Rely on role prompting alone (combine with constraints)

RESOURCES & REFERENCES
See Also:

Main SDK Design Guide - 9 sections, 14,000 words

SDK Skills Framework - Competency levels, Q&A

Quick Reference Card - Printable cheat sheet

Real-World Examples to Study:

Retrofit: Clean API design, type-safe builder

Firebase: Multi-module architecture, cross-platform consistency

OkHttp: Error handling, interceptor pattern

Ktor: DSL design, plugin architecture

RevenueCat: Kotlin Multiplatform patterns

VALIDATION CHECKLIST
Use this before submitting SDK work:

 Method names follow [Verb][Object] pattern

 Error handling uses sealed classes or specific exceptions

 All public methods have KDoc with examples

 Implementation marked internal

 Builder pattern used for 3+ configuration params

 Exceptions documented with @throws

 Tests included and passing

 No breaking changes without 12-month notice

 Migration guide provided if deprecating

 Backward compatible when possible

 Code examples in docs are compilable and correct




## KDOC and commenting guidelines
- Use KDoc for all public classes, methods, and properties.
- Provide clear descriptions of functionality, parameters, return values, and exceptions.
- Include usage examples in KDoc where applicable.
- dont use single line comments except for very short clarifications within methods. Prefer KDoc for public API documentation.
- never write comments that state the obvious or repeat what the code does. Comments should add value and context.
- Keep comments up to date with code changes to avoid misinformation.
- Good code should need fewer comments, not more.

## Code Readability
- Use meaningful and descriptive names for variables, functions, classes, and packages with respect to ART naming memory usages.
- Follow consistent formatting and indentation for better readability.
- Break down complex functions into smaller, single-responsibility functions.
- Avoid deep nesting by using early returns and guard clauses.
- Non Negotiable: Always prioritize code readability and maintainability in every code contribution write code as poetry.




Ownership & Follow-Through
Assigned work gets done. Follow-ups are closed. No open ends.

Depth of Thinking
Big-picture + execution detail required. No blind spots or unchecked assumptions.

Speed & Decision-Making
Decisions made with urgency. Unjustified delays are unacceptable.

Excellence Over Mediocrity
Mediocre output is not acceptable. Quality, clarity, precision are mandatory.

Mastery of the Business
Leaders must clearly articulate vision, platform, GTM, customers, and data. Metrics must be readily available.

Functional Accountability
Clear goals, targets, and ownership per function. Underperformance addressed within 90 days.

Time & Priority Discipline
Internal meetings do not override critical conversations or leadership meetings. Meetings end when ended. Customer calls are the exception.

Deadlines
Deadlines are commitments. Delivery on time with quality is required.

No Assumptions
Ask when unclear. Assumptions causing misalignment or rework are unacceptable.

One Team, One Standard
No blame-shifting or silos. Problems are owned and closed collaboratively. Same standard across geographies.