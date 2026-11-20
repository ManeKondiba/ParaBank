# Test Automation Toolkit Rules

1. Every UI test must extend `base.BaseClass`.
2. Arbitrary `Thread.sleep` is strictly prohibited.
3. Test methods must define descriptive TestNG groups (`Sanity`, `Smoke`, `Regression`, etc.).
4. Data generation must use `CustomerDataFactory` instead of hardcoded strings.
