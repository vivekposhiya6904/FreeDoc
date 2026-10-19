# FreeDoc — Project Roadmap & Milestones

**Status:** Living Engineering Document  
**Current Milestone:** Phase 0 (Repository & Engineering Foundation)  
**Tracking Model:** Milestone-based completion criteria with unchecked task lists. In accordance with project policy, no artificial delivery dates are specified; progress is determined by verified test passage, code review, and empirical device validation.

---

## Roadmap Overview

```
Phase 0 ──▶ Phase 1 ──▶ Phase 2 ──▶ Phase 3 ──▶ Phase 4 ──▶ Phase 5
  │           │           │           │           │           │
Foundation    PDF Core    Document    Editing &   Advanced    Hardening &
& Tooling     & Images    Model &     Office      Conversion  Release
              Handling    Viewers     Export      Engine      Readiness
```

---

## Phase 0: Repository & Engineering Foundation

**Objective:** Establish the development environment, baseline documentation, project structure, continuous integration, and licensing compliance.

- [x] Complete initial project documentation suite (`README.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `docs/PRODUCT_SPECIFICATION.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `docs/TESTING_STRATEGY.md`).
- [x] Initialize Android project structure using Gradle Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`), AndroidX, and Jetpack Compose.
- [x] Configure Gradle wrapper and reproducible build environment (Gradle 8.14, Kotlin 2.0.21, AGP 8.11.1).
- [x] Build core application foundation (`MainActivity`, `HomeScreen`, `FreeDocTheme` with Material 3).
- [x] Implement initial isolated document-engine experiment (`DocxExtractor`, `SimplePdfGenerator`, `SimpleDocument`).
- [x] Implement unit testing infrastructure and automated test suite verifying DOCX text extraction and baseline PDF generation on JVM (`DocumentModelTest`, `DocxToPdfExperimentTest`).
- [ ] Configure Git workflows, automated linting checks (ktlint / detekt), and editorconfig.
- [ ] Establish automated GitHub Actions CI pipeline for build verification, unit testing, and license scanning.
- [ ] Perform smoke-test validation on a physical device or running Android emulator.
- [ ] Verify runtime offline conformance in a network-isolated device environment.

**Completion Criteria for Phase 0:**
- [x] Application compiles and builds debug APK (`app-debug.apk`) successfully via `./gradlew assembleDebug`.
- [x] Automated unit test suite passes with 100% success rate via `./gradlew test` on JVM.
- [x] Documentation accurately reflects repository state, package boundaries, and architectural decisions.
- [ ] CI pipeline validated in GitHub repository for pull request builds.
- [ ] On-device launch verified on physical test hardware or emulator.

---

## Phase 1: PDF Viewing, Image-to-PDF, PDF-to-Image & Local File Handling

**Objective:** Deliver an offline-first MVP supporting core PDF viewing, image conversion, and robust local file operations via the Storage Access Framework.

- [ ] Implement Storage Access Framework (SAF) integration for opening and creating documents via content URIs (`ACTION_OPEN_DOCUMENT`, `ACTION_CREATE_DOCUMENT`).
- [ ] Build basic `DocumentHubScreen` UI with file selection and recent documents cache.
- [ ] Integrate and profile on-device PDF rendering engine (evaluating native `PdfRenderer` against memory constraints).
- [ ] Implement `DocumentReaderScreen` for PDF:
  - [ ] Multi-page vertical scrolling view.
  - [ ] Pinch-to-zoom and double-tap zoom gestures.
  - [ ] Page jump slider and thumbnail navigator.
  - [ ] Text extraction and on-page text search.
- [ ] Implement image-to-PDF conversion pipeline:
  - [ ] Batch image selection and drag-to-reorder interface.
  - [ ] Page orientation, margins, and fit-to-page settings.
  - [ ] Streamed PDF generation with atomic output writing.
- [ ] Implement PDF-to-image conversion pipeline:
  - [ ] Single-page and full-document export to PNG, JPEG, and WebP.
  - [ ] Configurable export resolution (DPI).
- [ ] Implement memory-aware bitmap recycling and LRU page cache.

**Completion Criteria for Phase 1:**
- [ ] Opens and renders multi-page PDF files on physical test devices without Out-Of-Memory errors.
- [ ] Generates valid PDF files from multiple camera images verifiable by standard independent PDF readers.
- [ ] Passes automated integration tests for SAF file reading and writing.
- [ ] All features verified to operate with physical device network connectivity disabled.

---

## Phase 2: Document Model & DOCX/ODT Prototype

**Objective:** Construct the format-agnostic Internal Document Model and build read-only viewers for Microsoft Word (`.docx`) and OpenDocument (`.odt`) files.

- [ ] Define the full Core Document Model (data structures for Document, Sections, Paragraphs, Spans, Tables, Images).
- [ ] Implement streaming DOCX format reader:
  - [ ] Unpack OpenXML ZIP packages safely with Zip Slip and Zip Bomb protection.
  - [ ] Parse `word/document.xml` into Internal Document Model.
  - [ ] Extract and resolve inline character styles, headings, and font metadata.
  - [ ] Parse basic table grids and cell structures.
  - [ ] Extract embedded media assets into local session storage.
- [ ] Implement streaming ODT format reader:
  - [ ] Unpack OpenDocument ZIP packages safely.
  - [ ] Parse `content.xml` and `styles.xml` into Internal Document Model.
  - [ ] Map ODF paragraph, text, and table elements to Internal Document Model.
- [ ] Develop native text layout and pagination engine:
  - [ ] Measure text spans against device font metrics.
  - [ ] Compute line breaking and paragraph flow.
  - [ ] Paginate content into discrete pages for reader preview.
- [ ] Extend `DocumentReaderScreen` to render DOCX and ODT documents.

**Completion Criteria for Phase 2:**
- [ ] Successfully parses and renders standard sample DOCX and ODT test files without crashes.
- [ ] Document layout displays styled text, headings, lists, tables, and images accurately.
- [ ] Parser handles malformed or partially corrupt XML files with user-friendly error messages rather than unhandled exceptions.

---

## Phase 3: Document Editing & Office Format Export

**Objective:** Enable interactive document editing from scratch or from existing files, along with export back to standard office formats.

- [ ] Implement `DocumentEditorScreen` with Jetpack Compose canvas:
  - [ ] Interactive text caret and touch selection handles.
  - [ ] On-screen formatting toolbar (Bold, Italic, Underline, Fonts, Colors, Alignment).
  - [ ] Paragraph indentation, line spacing, and bullet/numbered list toggles.
- [ ] Build Command/Transaction engine for undo and redo support.
- [ ] Implement table editing tools (insert table, add/remove rows/columns, resize cells).
- [ ] Implement image insertion, resizing, and alignment controls.
- [ ] Build Page Setup dialog (margins, orientation, page size).
- [ ] Implement format writers:
  - [ ] Internal Model to DOCX serializer (OpenXML generator).
  - [ ] Internal Model to ODT serializer (ODF generator).
  - [ ] Internal Model to PDF generator (print/export renderer).
- [ ] Implement non-destructive save flows (Save As new file, explicit overwrite confirmation).

**Completion Criteria for Phase 3:**
- [ ] Users can author a rich document with text, tables, and images, and save it to DOCX, ODT, or PDF.
- [ ] Generated files open seamlessly in desktop office suites (Microsoft Word, LibreOffice) without warnings or repair prompts.
- [ ] Undo/redo stack operates without state drift or data loss across repeated mutation cycles.

---

## Phase 4: Advanced Conversion Engine (Including PDF to Editable DOCX)

**Objective:** Implement bidirectional format interchange and the structural layout reconstruction pipeline for converting PDF documents into editable DOCX files.

- [ ] Implement DOCX to PDF direct conversion pipeline.
- [ ] Implement ODT to PDF direct conversion pipeline.
- [ ] Implement DOCX ↔ ODT bidirectional format translation.
- [ ] Design and implement PDF-to-editable-DOCX structural reconstruction engine:
  - [ ] Extract raw glyphs, character strings, and bounding boxes from PDF content streams.
  - [ ] Heuristic reading-order analysis and text column detection.
  - [ ] Paragraph clustering based on line spacing and horizontal indentation.
  - [ ] Font style inference (bold/italic weights, font sizes, heading level classification).
  - [ ] Table structure recognition from vector lines and aligned text blocks.
  - [ ] Image extraction and geometric anchoring.
  - [ ] Map reconstructed structures into the Internal Document Model and serialize to DOCX.
- [ ] Add conversion preview, warning notices for complex layouts, and side-by-side comparison screen.

**Completion Criteria for Phase 4:**
- [ ] Clean text-based PDF documents convert into editable DOCX documents with preserved paragraphs, headings, and tables.
- [ ] Conversion pipeline operates fully on-device without internet access.
- [ ] Clear user feedback is displayed for non-standard or complex layouts that cannot be fully reconstructed.

---

## Phase 5: Optimization, Security Hardening, Compatibility Testing & Release Readiness

**Objective:** Profile performance, audit security boundaries, expand compatibility testing across diverse real-world documents, and prepare for initial public release.

- [ ] Memory and performance optimization:
  - [ ] Heap memory profiling and leak elimination using Android Studio Profiler.
  - [ ] Render performance optimization (aiming for fluid 60fps scrolling on target devices, provisional benchmark).
  - [ ] Background thread task management and battery impact review.
- [ ] Security audit:
  - [ ] XML parser hardening (disabling external entities, DTD attacks, zip-bomb protection).
  - [ ] SAF permission boundaries and temporary file scrubbing.
- [ ] Compatibility verification:
  - [ ] Run test suite across extensive document corpus (synthetic and open-source documents).
  - [ ] Test on multiple Android OS versions (Android 8.0 through latest stable).
  - [ ] Test on multiple screen form factors (phones, foldables, tablets).
- [ ] Accessibility review (TalkBack support, dynamic text scaling, high-contrast themes).
- [ ] Finalize release builds (ProGuard/R8 rules, reproducible builds, release notes).

**Completion Criteria for Phase 5:**
- [ ] Zero critical crashes or unhandled exceptions across the verification corpus.
- [ ] Complete automated test suite passes with high coverage across core modules.
- [ ] Fully verified offline operation with zero network permissions in `AndroidManifest.xml`.
