# Playwright MCP Exploration Rules

1. Every generated Page Object MUST extend `pageobjects.BasePage`.
2. Locators must be private constants inside the Page Object class.
3. Every action method must return `this` or the next logical PageObject.
4. Absolute XPaths are strictly forbidden.
