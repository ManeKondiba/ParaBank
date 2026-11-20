# PR Review Quality Rules

1. Every test class must have descriptive test annotations (`@Test(description = "...")`).
2. Page Object methods must not exceed 25 lines of code.
3. Empty catch blocks without documentation are strictly forbidden.
4. All test data containers must be immutable records where possible.
