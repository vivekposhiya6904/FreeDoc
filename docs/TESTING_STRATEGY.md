# FreeDoc — Testing, Performance & Compatibility Strategy

**Document Status:** Baseline Test Strategy & Test Suite Specification  
**Current Milestone:** Phase 0 (Engineering Foundation & Proof of Concept)  
**Execution Environment:** Android Gradle Plugin 8.11.1, Kotlin 2.0.21, JUnit 4, JVM (JDK 21)

---

## 1. Quality & Verification Philosophy

In accordance with FreeDoc core principles:
1. **Measured, Not Assumed:** Document compatibility and performance metrics must be empirically measured on target hardware rather than assumed.
2. **Deterministic & Offline:** Automated verification suites must execute without network connectivity.
3. **Non-Destructive Integrity:** Tests must verify that source documents remain unaltered during read and conversion workflows.
4. **Honest Scope Boundaries:** Tests distinguish between semantic text extraction and desktop-grade visual layout reproduction. A passing string search test inside a generated PDF does **not** prove layout fidelity or PDF viewer compatibility.

---

## 2. Implemented Test Suite (Phase 0 Evidence)

The repository currently contains automated JVM unit tests under `app/src/test/java/com/freedoc/app/`:

### 2.1 `com.freedoc.app.core.model.DocumentModelTest`
- **What it tests:** Basic data accumulation and plain-text serialization in `SimpleDocument` and `SimpleParagraph`.
- **What it actually proves:**
  - Concatenation of multiple styled runs (`SimpleTextRun`) across paragraphs produces expected plain text.
  - An empty document produces an empty string without throwing null pointer exceptions.
- **Verification Result:** 2 tests executed, 0 failures, 0 errors (Execution time: 0.062s).

### 2.2 `com.freedoc.app.engine.experiment.DocxToPdfExperimentTest`
- **What it tests:** End-to-end pipeline from an in-memory synthetic DOCX OpenXML package to a PDF-1.4 output stream.
- **What it actually proves:**
  - In-memory ZIP package creation and extraction.
  - `DocxExtractor` parses `word/document.xml`, resolves `w:pStyle` headings, extracts bold and italic text runs, and populates `SimpleDocument`.
  - `SimplePdfGenerator` writes a compliant PDF-1.4 header, catalog dictionary, pages tree, A4 page dictionary, Type-1 Helvetica font dictionaries, content stream with text positioning operators, cross-reference table, and trailer dictionary.
  - Output bytes contain expected strings and structural tokens.
- **Critical Limitation Disclosed:** This test confirms byte-level stream construction and basic text extraction. It does **not** prove visual rendering correctness in an independent PDF viewer, text wrapping, pagination, or compatibility with complex real-world DOCX files.
- **Verification Result:** 1 test executed, 0 failures, 0 errors (Execution time: 0.111s).

---

## 3. Verification Matrix by Category

| Verification Category | Current Status | Tools & Commands | Current Evidence & Limitations |
| :--- | :--- | :--- | :--- |
| **JVM Unit Tests** | **Passed** | `./gradlew test` | 3 unit tests pass with 100% success rate on JVM. |
| **Android Build Verification** | **Passed** | `./gradlew assembleDebug`<br>`./gradlew assembleRelease` | Debug APK generated (`app-debug.apk`, ~9.04 MB). Release APK generated (`app-release-unsigned.apk`, ~6.15 MB). |
| **Offline Conformance** | **Partially Verified** | Manifest inspection | Manifest declares 0 network permissions. Physical device verification with Wi-Fi disabled is **pending**. |
| **Device / Emulator Execution** | **Pending** | `adb install ...` | No active emulator or physical device was attached during Phase 0 build. Smoke test is pending. |
| **DOCX Fuzzing & Malformed Files** | **Pending** | Planned test suite | Secure XML processing enabled in code, but dedicated malformed input tests are pending. |
| **Independent PDF Rendering** | **Pending** | `PdfRenderer` / PDFBox | Visual pixel-level validation is pending Phase 1 viewer integration. |
| **Real-World Document Corpus** | **Pending** | Curated file corpus | Compatibility against diverse office generators is scheduled for Phase 2+. |
| **Performance & Memory Profiling** | **Pending** | Android Studio Profiler | Heap and frame-rate measurements require physical device benchmarking in Phase 1+. |

---

## 4. Future Test Specifications

### 4.1 Future Test Definitions for `DocxExtractor`
The current extractor must be hardened and validated against the following planned test cases:
1. **Malformed ZIP Archives:**
   - Truncated files and corrupt central directories.
   - Files with missing `word/document.xml` or missing `[Content_Types].xml`.
2. **Oversized Input & Security Hazards:**
   - Zip Slip attacks: entries attempting path traversal via `../`.
   - Zip Bomb attacks: highly compressed recursive payloads.
   - XML Entity attacks: billion-laughs expansion and external DTD references (assert parser rejection).
3. **XML Structure Variances:**
   - Namespaces with differing prefixes (e.g. `w14`, `w15`).
   - Deeply nested paragraphs and custom paragraph styles.
   - Text split across arbitrary run boundaries (`<w:r>`).
   - Unusual whitespace, non-breaking spaces, and tab characters.
4. **Unsupported Complex Structures:**
   - Graceful degradation: documents with tables, drawingML, smartArt, embedded OLE objects, or equations must not crash the parser; text must be extracted cleanly.

### 4.2 Future Test Definitions for PDF Generator (`SimplePdfGenerator` & Future Engine)
The PDF generation engine must be validated against the following planned test cases:
1. **Independent PDF Viewer Validation:**
   - Load generated PDF streams into an independent validator (e.g., Android `PdfRenderer` or Apache PDFBox) to assert parseability and visual correctness.
2. **Multi-Page Layout & Pagination:**
   - Content exceeding single page height must paginate cleanly across multiple `Page` objects with appropriate coordinate resets.
3. **Text Wrapping & Flow:**
   - Long paragraphs must compute line breaks and wrap cleanly within margins rather than drawing past page boundaries.
4. **Character Encoding & Escaping:**
   - Escaping of PDF delimiters: literal parentheses `(`, `)`, backslashes `\`, and special characters.
   - Unicode character handling: beyond basic WinAnsi/Type-1 fonts, verify TrueType/OpenType font embedding or character fallbacks.
5. **Output Stream Integrity:**
   - Verification of exact byte offsets in cross-reference tables (`xref`) to guarantee standard compliance across strict PDF readers.

---

## 5. Synthetic Test Corpus Specification

To prevent copyright issues and ensure reproducible, license-clean benchmarking, FreeDoc defines a standardized synthetic document corpus created specifically for this project:

| Corpus ID | Document Name | Format | Complexity & Feature Focus | License |
| :--- | :--- | :--- | :--- | :--- |
| `SYN-01` | `simple-paragraphs.docx` | DOCX | Single-column plain text paragraphs, standard margins. Baseline text parser test. | CC0 1.0 Universal / Public Domain |
| `SYN-02` | `styled-text.docx` | DOCX | Inline bold, italic, underline, font sizes, heading styles (H1, H2). | CC0 1.0 Universal / Public Domain |
| `SYN-03` | `multi-paragraph.docx` | DOCX | Multi-page text flow, paragraph spacing, line breaks, bulleted/numbered lists. | CC0 1.0 Universal / Public Domain |
| `SYN-04` | `table-basic.docx` | DOCX | 3x4 structured table with headers, borders, and column alignment. | CC0 1.0 Universal / Public Domain |
| `SYN-05` | `embedded-image.docx` | DOCX | Inline PNG/JPEG illustration with text wrapping. | CC0 1.0 Universal / Public Domain |
| `SYN-06` | `multi-page-doc.docx` | DOCX | 10+ page mixed document (headings, tables, text) testing streaming and memory. | CC0 1.0 Universal / Public Domain |
| `SYN-PDF-01` | `vector-text.pdf` | PDF | Clean vector PDF with standard Type-1 and TrueType text streams. | CC0 1.0 Universal / Public Domain |
| `SYN-MAL-01` | `corrupted-header.docx` | DOCX | Truncated ZIP archive simulating incomplete download or disk corruption. | CC0 1.0 Universal / Public Domain |
| `SYN-MAL-02` | `xxe-injection.docx` | DOCX | XML payload attempting external DTD / entity expansion. | CC0 1.0 Universal / Public Domain |

*Policy Note:* External third-party documents used for compatibility testing must be open-source documents (e.g., Apache POI test fixtures, OASIS ODF test suites) and tracked with explicit provenance and license records.

---

## 6. Execution Commands & Reproducibility

**Run Automated Unit Tests:**
- Windows:
  ```powershell
  .\gradlew.bat test
  ```
- Linux / macOS:
  ```bash
  ./gradlew test
  ```

**Build Debug APK:**
- Windows:
  ```powershell
  .\gradlew.bat assembleDebug
  ```
- Linux / macOS:
  ```bash
  ./gradlew assembleDebug
  ```

**Inspect Test Output XML:**
Unit test XML reports are written to:
`app/build/test-results/testDebugUnitTest/`
