# API Test Automation & Review Plugin

An enterprise-grade Antigravity plugin for AI-powered REST Assured test generation, OpenAPI contract validation, negative scenario synthesis, and automated code review.

## Capabilities

1. **OpenAPI-Driven Test Generation**: Synthesizes structured TestNG tests using typed domain clients and `ApiRequestFactory`.
2. **Pre/Post Ledger Invariant Auditing**: Verifies financial balance math and ledger entries for every transactional API call.
3. **Negative Scenario Synthesis**: Generates boundary, BOLA/IDOR, and SQLi negative test suites.
4. **Multi-Perspective Review**: Audits API automation pull requests across contract integrity, state invariance, security, and performance.

## Structure
```
plugins/api-test-automation/
├── plugin.json
├── README.md
├── agents/
│   └── api-test-architect.md
├── rules/
│   └── AGENTS.md
└── skills/
    └── api-test-automation/
        └── SKILL.md
```
