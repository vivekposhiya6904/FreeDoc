# Contributing to FreeDoc

Thank you for your interest in contributing to FreeDoc! We are building a free, privacy-first, offline-first Android document workspace.

Please review this guide before submitting issues, discussions, or code changes.

---

## 1. Codebase Access & Governance

- **Public Cloning & Forking:** The repository is public. Anyone may clone, fork, study, and experiment with the codebase in accordance with the [Apache License 2.0](LICENSE).
- **Direct Write Access:** Direct push and commit access to the main repository and official branches is strictly restricted to authorized project maintainers.
- **Contributions via Pull Requests:** All community contributions—including those from maintainers—must be proposed via Pull Requests (PRs) originating from topic branches or personal forks.

---

## 2. Contribution Workflow

To propose a change, please follow these steps:

### Step 1: Check Existing Work & Issues
- Before beginning work, check the issue tracker and [docs/ROADMAP.md](docs/ROADMAP.md) to ensure the proposed change aligns with current milestone priorities and is not already in progress.
- For significant architectural or feature changes, open a Discussion or RFC issue first to discuss the design before writing extensive code.

### Step 2: Fork and Branch
1. Fork the FreeDoc repository to your personal GitHub account.
2. Clone your fork locally:
   ```bash
   git clone https://github.com/<your-username>/FreeDoc.git
   cd FreeDoc
   ```
3. Create a descriptive topic branch from `main`:
   ```bash
   git checkout -b feature/image-to-pdf-margins
   # or for bug fixes:
   git checkout -b fix/docx-table-border-parsing
   ```

### Step 3: Implement Focused Changes
- Keep changes concise, focused, and directly tied to a single objective. Avoid bundling unrelated refactors or formatting changes with bug fixes.
- Adhere to clean architecture and Kotlin/Android coding conventions.
- Maintain documentation integrity: update relevant documentation in `docs/` if modifying public contracts, configuration, or architectural assumptions.

### Step 4: Validate and Test Locally
Execute local build and test checks before opening a pull request:

- **Run Automated Unit Tests:**
  - Windows:
    ```powershell
    .\gradlew.bat test
    ```
  - Linux / macOS:
    ```bash
    ./gradlew test
    ```

- **Verify Debug Build:**
  - Windows:
    ```powershell
    .\gradlew.bat assembleDebug
    ```
  - Linux / macOS:
    ```bash
    ./gradlew assembleDebug
    ```

- **Document Integrity Testing:** Verify that changes do not corrupt files, alter original documents unintentionally, or produce invalid output formats.
- **Offline Conformance:** Verify that all modified or newly introduced features execute properly without requiring active network access.

### Step 5: Submit a Pull Request
1. Push your branch to your personal fork.
2. Open a Pull Request against the `main` branch of the official FreeDoc repository.
3. Provide a clear, detailed PR description explaining:
   - What problem is being solved.
   - What changes were made.
   - How the change was tested (including document types and test environments).
   - Any trade-offs or remaining questions.

---

## 3. Strict Contribution Policies

### 3.1 No Secrets or Sensitive Data
- **Never commit:** API keys, signing keys, passwords, credentials, tokens, or personal configuration files (`local.properties`, keystores).
- Verify `.gitignore` rules before staging files. Do not commit build artifacts (`build/`, `.gradle/`, `.apk`, `.aab`).

### 3.2 Offline & Privacy Guarantee
- FreeDoc strictly promises 100% on-device processing for core features.
- PRs that introduce network calls for core document operations, analytics trackers, advertising SDKs, or cloud-processing fallbacks will not be accepted.

### 3.3 Third-Party Dependency & License Review
- FreeDoc is distributed under the **Apache License 2.0**.
- Any new library, dependency, or external code snippet must be evaluated for license compatibility, redistribution obligations, patent terms, and scope of restrictions.
- Do not declare a library incompatible solely based on its license title. Examine the actual license terms, patent grants, copyleft scopes, and redistribution conditions.
- If there is legal or architectural uncertainty regarding whether a library's obligations are compatible with FreeDoc's Apache 2.0 distribution model, clearly flag the dependency in your pull request for maintainer review rather than asserting a conclusion.
- Evaluate the binary size impact before proposing a new external dependency; mobile package footprint must be respected.

---

## 4. Code of Conduct

All contributors and participants are expected to adhere to the [FreeDoc Code of Conduct](CODE_OF_CONDUCT.md). Please treat all community members with respect, patience, and professionalism.
