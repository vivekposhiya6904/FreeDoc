# FreeDoc

> **"Your Documents. Your Device. Your Control."**

FreeDoc is a free, privacy-focused, offline-first Android document workspace designed to view, create, edit, and convert PDF, DOCX, ODT, and common image formats directly on your mobile device.

---

## Mission & Vision

The mission of FreeDoc is to empower users with full sovereignty over their digital documents without compromising on privacy, security, or usability. FreeDoc aims to provide an all-in-one document workspace that operates completely on-device without cloud lock-in, forced accounts, subscription paywalls, or privacy-invasive tracking.

Our long-term vision is to develop our own efficient, modular document-engine components wherever they provide meaningful control, performance, or capabilities, while responsibly reusing compatible, permissively licensed open-source libraries where appropriate.

---

## Current Project Status

FreeDoc is currently in **Phase 0 (Engineering Foundation & Proof of Concept)**.

### Implemented & Verified Foundation
The repository contains a fully configured, compiling Android project with an initial experimental document engine:
- **Android Application Framework:** Built with Kotlin (2.0.21), Jetpack Compose (Material 3), Gradle Kotlin DSL, and Android Gradle Plugin (8.11.1).
- **Core App Shell:** Configured `MainActivity`, edge-to-edge Compose rendering, dynamic light/dark theme palette, and an informative `HomeScreen` displaying the FreeDoc branding, motto, and development status.
- **Strict Offline Configuration:** `AndroidManifest.xml` operates with zero network permissions (`android.permission.INTERNET` is not requested).
- **Internal Document Model (Proof of Concept):** `com.freedoc.app.core.model.DocumentModel` provides an initial semantic data model (`SimpleDocument`, `SimpleParagraph`, `SimpleTextRun`, `RunStyle`).
- **DOCX Extraction Engine (Proof of Concept):** `com.freedoc.app.engine.experiment.DocxExtractor` securely parses OpenXML packages via in-memory zip streams and hardened XML DOM parsing (external entity resolution disabled to prevent XXE), extracting headings, styled text runs, and paragraph structures.
- **PDF Serialization Engine (Proof of Concept):** `com.freedoc.app.engine.experiment.SimplePdfGenerator` emits standards-compliant, vector PDF-1.4 streams containing extracted text and Type-1 font structures without heavy third-party dependencies.
- **Automated Verification:** Automated unit test suites (`DocumentModelTest`, `DocxToPdfExperimentTest`) pass 100% on the JVM, and the Android debug APK compiles successfully via `./gradlew assembleDebug`.

### Planned Capabilities (Not Yet Implemented)
End-user document viewing, interactive editing, and desktop-fidelity conversion are currently under active development per our roadmap:
- On-device PDF rendering and multi-page interactive viewer.
- Storage Access Framework (SAF) system file picker and save workflows.
- Batch image-to-PDF and PDF-to-image extraction.
- Interactive rich-text and table editing canvas.
- Complex office layout engines (DOCX and ODT styling fidelity).
- Advanced structural reconstruction (PDF to editable DOCX).

---

## Core Principles

- **100% Offline by Design:** Core features work fully offline immediately after installation. No server connection is required to open, edit, or convert documents.
- **No Logins or Accounts:** Zero registration, sign-ins, profile creation, or identity tracking.
- **No Advertisements or Watermarks:** Exported and converted files remain strictly yours—clean, unaltered, and free of promotional watermarks or in-app ads.
- **No Subscriptions for Core Features:** Document viewing, editing, and conversion features are free and accessible without recurring fees.
- **On-Device Document Processing:** All parsing, rendering, text manipulation, and format conversions execute locally on your device.
- **Preserve Originals by Default:** FreeDoc adheres to non-destructive workflows. Original files are never modified or overwritten without explicit user confirmation.
- **Measured Performance & Pragmatic Footprint:** We strive to optimize installation size, processing speed, and memory consumption without arbitrarily sacrificing necessary functionality. We do not claim arbitrary unmeasured benchmarks.
- **Verified Compatibility:** Document compatibility and conversion fidelity are empirically tested against standard test suites rather than assumed.

---

## Planned Feature Scope

### 1. Document Viewing
- **PDF Documents:** Multi-page rendering, smooth zooming, text selection, page navigation, and bookmark outlines.
- **DOCX (Microsoft Word):** Native text flow, styling, tables, lists, and inline image rendering.
- **ODT (OpenDocument Text):** Comprehensive OpenDocument text format parsing and visual layout.
- **Images:** Common image formats including PNG, JPEG, and WebP.

### 2. Document Creation & Editing
- **Create from Scratch:** Start blank documents for quick note-taking, drafting, or formatted document authoring.
- **Text & Paragraph Editing:** Rich styling (bold, italic, underline, strikethrough), font selection, headings, text alignment, and line/paragraph spacing.
- **Tables:** Insert, configure, and format tables with custom row/column counts, cell borders, and cell alignment.
- **Images:** Insert images from device storage, reposition, scale, and configure text wrapping.
- **Page Layout & Geometry:** Configurable page margins, standard page sizes (A4, Letter, Legal), and orientation (portrait/landscape).
- **Structure & Navigation:** Headers, footers, automatic page numbering, and bulleted/numbered lists.

### 3. Document Conversion & Export
- **Images to PDF:** Batch-compile images into structured multi-page PDF documents with customizable page sizing and compression.
- **PDF Pages to Images:** Extract individual pages or complete PDF documents into high-resolution images (PNG, JPEG, WebP).
- **DOCX to PDF & ODT to PDF:** High-fidelity conversion from standard office formats to portable PDF documents.
- **DOCX ↔ ODT Interchange:** Bidirectional conversion between Microsoft Word and OpenDocument text formats.
- **PDF to Editable DOCX:** Structural layout reconstruction converting static PDF pages into editable DOCX files.
- **Export Capabilities:** Export active documents to PDF, DOCX, or ODT formats depending on editing mode.

### 4. File Handling & System Integration
- **Storage Access Framework (SAF):** Native integration with Android's system file picker and scoped storage, respecting modern Android security practices without broad storage permission requests.
- **Original File Protection:** Safe, non-destructive file operations using atomic writes and explicit export targets.
- **Large Document Resilience:** Memory-conscious streaming and page-by-page processing designed to prevent Out-Of-Memory (OOM) errors on mobile hardware.
- **Integrity Validation & Error Reporting:** Clear, actionable error reporting and automatic integrity checks for generated files.

---

## Technical Challenges & Engineering Realities

Developing an on-device document suite on Android presents real-world engineering hurdles that we address with transparency:

1. **PDF to Editable DOCX Reconstruction:** PDF is a visual layout format composed of absolute coordinate drawing instructions (glyphs, lines, bitmaps) without semantic structure. Inferring paragraphs, running text, table grids, headers, and column structures from raw coordinates is an inherently heuristic problem. FreeDoc will use algorithmic structure recovery, but we **do not claim or promise perfect conversion** across every complex or non-standard document.
2. **Office Document Layout Fidelity:** DOCX and ODT specifications are massive and intricate. Reproducing complex layouts (e.g., nested tables, complex text wrapping around non-rectangular shapes, multi-column flows) with 100% desktop fidelity on a mobile engine requires iterative engineering.
3. **Mobile Memory & Thermal Constraints:** Rendering high-resolution PDF pages or parsing large office files consumes significant RAM and CPU power. FreeDoc emphasizes strict memory budgeting, background concurrency, bitmap recycling, and page-level caching to prevent system UI freezes or crashes.

---

## Development Setup & Build Instructions

### Prerequisites
- **Java Development Kit (JDK):** JDK 17 or JDK 21 (recommended: OpenJDK 21).
- **Android SDK:** Android SDK Platform API 36 (or API 35) with Android SDK Build-Tools (34.0.0 or higher).
- **Gradle:** The project includes the standard Gradle wrapper (`gradle-8.14`).

### Local Environment Configuration
Configure your local Android SDK location by creating or verifying a `local.properties` file in the project root:
```properties
sdk.dir=/path/to/your/android/sdk
```
*(On Windows, escape backslashes: e.g. `sdk.dir=C\:\\Users\\Username\\AppData\\Local\\Android\\Sdk`)*. Note: `local.properties` is private and excluded from version control via `.gitignore`.

### Build & Test Commands

**Run Automated Unit Tests:**
- Linux / macOS:
  ```bash
  ./gradlew test
  ```
- Windows:
  ```powershell
  .\gradlew.bat test
  ```

**Build Debug APK:**
- Linux / macOS:
  ```bash
  ./gradlew assembleDebug
  ```
- Windows:
  ```powershell
  .\gradlew.bat assembleDebug
  ```

The resulting debug APK is generated at the local build path:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Development Roadmap Summary

Development is organized into phased milestones:

- **Phase 0:** Repository & Engineering Foundation *(Current Phase — Foundation established, baseline tests verified)*
- **Phase 1:** PDF Viewing, Image-to-PDF, PDF-to-Image, and Local File Handling
- **Phase 2:** Internal Document Model & DOCX/ODT Prototype
- **Phase 3:** Document Editing & Office Format Export
- **Phase 4:** Advanced Conversion & PDF-to-Editable-DOCX Pipeline
- **Phase 5:** Optimization, Security Auditing, Compatibility Testing, and Release Readiness

For detailed checklists and completion criteria, consult [docs/ROADMAP.md](docs/ROADMAP.md).

---

## Project Documentation

Detailed technical and product documentation is maintained in the repository:

- [Product Specification](docs/PRODUCT_SPECIFICATION.md): Comprehensive feature scope, requirements, and edge case definitions.
- [Architecture Blueprint](docs/ARCHITECTURE.md): Current package structure, proposed modular system architecture, format adapters, and rendering pipelines.
- [Roadmap](docs/ROADMAP.md): Milestone phases, task tracking, and release acceptance criteria.
- [Testing Strategy](docs/TESTING_STRATEGY.md): Testing methodology, test matrix, synthetic document corpus, and performance profiling guidelines.
- [Contributing Guide](CONTRIBUTING.md): Guidelines for code contributions, workflow, testing, and license compliance.
- [Code of Conduct](CODE_OF_CONDUCT.md): Community standards and participation guidelines.

---

## Contributing

We welcome contributions from developers, designers, and document format enthusiasts! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting pull requests.

Please note: While anyone is free to fork and clone the repository, direct write and push access to the official repository is restricted to authorized maintainers. All contributions must be submitted via pull requests and pass code reviews and automated checks.

---

## License

FreeDoc is distributed under the **Apache License, Version 2.0**.

See the full license text in the [LICENSE](LICENSE) file located in the root directory of this repository.
