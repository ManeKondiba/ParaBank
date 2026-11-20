---
name: security-guard
description: Pre-commit pattern warnings and LLM diff review detecting 25+ vulnerability classes including SQLi, XSS, SSRF, and secrets.
---

# Security Guidance Guard Skill

This skill acts as a real-time pre-commit security filter scanning diffs against 25+ vulnerability patterns.

---

## 1. Monitored Vulnerability Classes

1. **Hardcoded Secrets**: AWS keys, JWT tokens, private keys, plaintext passwords (`password = "..."`).
2. **SQL Injection (SQLi)**: String concatenation into SQL queries (`"SELECT * FROM users WHERE id = '" + id + "'"`).
3. **Cross-Site Scripting (XSS)**: Unsanitized DOM injection via `innerHTML` or `document.write`.
4. **Server-Side Request Forgery (SSRF)**: Forwarding unvalidated user input into HTTP requests.
5. **Insecure Direct Object References (IDOR/BOLA)**: Querying records solely by client-supplied IDs without session validation.
6. **Path Traversal**: Unsanitized file path construction (`"../../etc/passwd"`).
7. **Insecure Deserialization**: Deserializing untrusted byte streams without class filtering.
8. **XML External Entity (XXE)**: Parsing XML without disabling DOCTYPE declarations or external DTDs.
9. **Broken Cryptography**: Use of MD5, SHA-1 for password hashing; hardcoded DES/AES keys.
10. **Information Leakage**: Printing stack traces (`e.printStackTrace()`) or raw database errors.
11. **Session Hijacking**: Missing `HttpOnly` or `Secure` flags on session cookies.
12. **Sensitive PII in Logs**: Passwords, SSNs, credit cards printed in logs or reports.

---

## 2. Pre-Commit Review Workflow

When a code change or git diff is reviewed:
1. Scan added/modified lines for regex matches of sensitive tokens and string concatenation in queries.
2. Verify that `CustomerProfile` or other record types mask sensitive attributes in `toString()`.
3. If any pattern is detected, trigger an immediate warning with the exact line number, remediation advice, and safer alternative.
