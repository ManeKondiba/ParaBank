# Security Guidance Rules

1. Never commit hardcoded credentials, API keys, or private certificates.
2. String concatenation in SQL statements or HTTP paths with unencoded parameters is strictly forbidden.
3. All sensitive customer attributes (passwords, SSNs) must be masked in string representations.
