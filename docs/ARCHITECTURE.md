# FreeDoc — System Architecture Blueprint

**Document Status:** Living Architectural Specification  
**Current Milestone:** Phase 0 (Engineering Foundation & Proof of Concept)  
**Target Platform:** Android (Modern Android development with Kotlin & Jetpack Compose, `minSdk = 26`, `compileSdk = 36`, `targetSdk = 35`)

---

## 1. Architectural Philosophy & Strategy

FreeDoc is designed as a privacy-focused, offline-first mobile document suite. Because parsing, laying out, rendering, and converting rich document formats (PDF, DOCX, ODT, images) is computationally intensive, the architecture adheres to these core structural principles:

1. **Separation of Presentation & Computation:** UI components (Jetpack Compose) do not directly perform file parsing, layout calculation, or format conversion.
2. **Intermediate Semantic Model:** File formats (DOCX, ODT, PDF) are decoupled from the user interface and editing commands through an Internal Document Model.
3. **Non-Destructive File Operations:** Files opened via the Storage Access Framework (SAF) remain immutable by default; write operations use atomic staging.
4. **Pragmatic Evolution:** The architecture avoids premature over-engineering. Concrete engine modules and complex layout abstractions are introduced only as justified by empirical proof-of-concept experiments.

---

## 2. Current Implementation State (Phase 0)

The codebase currently contains the working Android application foundation and an isolated proof-of-concept document engine:

```
app/src/main/java/com/freedoc/app/
├── MainActivity.kt                 # Application launch entry point
├── ui/
│   ├── HomeScreen.kt               # Phase 0 landing screen & branding
│   └── theme/
│       ├── Color.kt                # Primary Navy & Teal color tokens
│       ├── Theme.kt                # Material 3 light/dark theme wrapper
│       └── Type.kt                 # Typography hierarchy
├── core/
│   └── model/
│       └── DocumentModel.kt        # Proof-of-concept Document Model
└── engine/
    └── experiment/
        ├── DocxExtractor.kt        # Secure DOCX text/style extractor
        └── SimplePdfGenerator.kt   # Zero-dependency PDF-1.4 text serializer
```

### Current Component Responsibilities
- **Application Shell (`com.freedoc.app`):** `MainActivity` sets up edge-to-edge window insets and hosts `HomeScreen` inside `FreeDocTheme`.
- **UI Presentation (`com.freedoc.app.ui`):** Compose-based screens communicating project status and branding without business logic coupling.
- **Experimental Document Model (`com.freedoc.app.core.model`):**
  - Defines `SimpleDocument`, `SimpleParagraph`, `SimpleTextRun`, and `RunStyle`.
  - Serves as the minimal data structure to validate text extraction pipelines without premature layout overhead.
- **Document-Engine Experiment (`com.freedoc.app.engine.experiment`):**
  - `DocxExtractor`: Parses DOCX archives using standard `ZipInputStream` and hardened XML DOM parsing (external entity resolution disabled to prevent XXE), extracting paragraphs, heading levels, and inline bold/italic runs.
  - `SimplePdfGenerator`: Constructs valid, standard PDF-1.4 streams with standard Type-1 fonts (Helvetica/Helvetica-Bold), catalog, page dictionary, text positioning operators, and cross-reference table.
  - *Distinction:* This experiment proves basic semantic text extraction and clean PDF stream generation. It does **not** constitute a full visual layout engine or claim full DOCX layout reproduction.

---

## 3. Proposed Target Architecture (Planned)

As FreeDoc progresses through upcoming roadmap phases, the application will evolve into a layered, modular system:

```
+-----------------------------------------------------------------------+
|                       Android UI Layer (Proposed)                     |
|             (Jetpack Compose, Material 3, Navigation, ViewModels)     |
+-----------------------------------------------------------------------+
                                   |
                                   v
+-----------------------------------------------------------------------+
|                    Application Orchestration (Proposed)               |
|       (Document Session Management, File Orchestration, History)       |
+-----------------------------------------------------------------------+
         |                                                 |
         v                                                 v
+-----------------------------+           +-----------------------------+
| Rendering & Layout (Proposed|           |  Editing & Mutation (Proposed)
|  (Flow Layout, Pagination,  |           |  (Command Pattern, Undo/Redo|
|   Font Metrics, Tile Cache) |           |   Selection, Caret Model)   |
+-----------------------------+           +-----------------------------+
         \                                                 /
          \                                               /
           v                                             v
+-----------------------------------------------------------------------+
|                 Full Core Document Model (Internal DOM)               |
|      (Sections, Paragraphs, Spans, Tables, Images, Style Cascade)     |
+-----------------------------------------------------------------------+
                                   ^
                                   |
+-----------------------------------------------------------------------+
|                Format Adapters & Converters (Proposed)                |
|   +---------------+  +---------------+  +---------------+  +--------+ |
|   |  PDF Adapter  |  | DOCX Adapter  |  |  ODT Adapter  |  | Images | |
|   +---------------+  +---------------+  +---------------+  +--------+ |
+-----------------------------------------------------------------------+
                                   ^
                                   |
+-----------------------------------------------------------------------+
|              Storage & System Integration Layer (Proposed)            |
|    (Storage Access Framework, Scoped Storage, Streams, Safe Temp)     |
+-----------------------------------------------------------------------+
```

### 3.1 Android UI Layer (Proposed)
- **Framework:** Jetpack Compose with Material 3.
- **Pattern:** Unidirectional Data Flow (UDF) with Android `ViewModel` and immutable `StateFlow`.
- **Target Destinations:**
  - `DocumentHubScreen`: Local recent files, quick actions (Create, Open, Convert).
  - `DocumentReaderScreen`: High-performance viewer optimized for scrolling, zooming, and text search across PDF, DOCX, and ODT.
  - `DocumentEditorScreen`: Authoring canvas with contextual toolbars, formatting panels, and table inspectors.
  - `ConversionStudioScreen`: Batch image-to-PDF setup, format interchange dialogs, and conversion progress indicators.

### 3.2 Storage Access Framework (SAF) & File Integration (Proposed)
- Integration with system contracts (`ActivityResultContracts.OpenDocument`, `CreateDocument`, `OpenDocumentTree`).
- Operates on scoped `content://` URIs without broad storage permissions (`MANAGE_EXTERNAL_STORAGE` or `READ_EXTERNAL_STORAGE`).
- Non-destructive atomic writes: mutations write to application cache first, validate output integrity, and stream atomically to destination URIs.

### 3.3 Full Core Document Model (Proposed Future Evolution)
The current `SimpleDocument` will evolve into a comprehensive, format-agnostic document tree:
- `Document`: Root container holding metadata, page setup, header/footer definitions, and sections.
- `Section`: Defines page geometry (margins, orientation, paper size) and block streams.
- `BlockElement`: Base for `Paragraph`, `Table`, and `PageBreak`.
- `InlineSpan`: Base for `TextSpan`, `ImageSpan`, and dynamic `FieldSpan` (page numbers, dates).

### 3.4 Format Adapters (Proposed Future Modules)
- **PDF Adapter:** Evaluation of Android native `PdfRenderer` for lightweight viewing vs. permissively licensed engines for text extraction and document assembly.
- **DOCX Adapter (OpenXML):** Evolution from `DocxExtractor` to full streaming reader and serializer supporting styled tables and embedded media.
- **ODT Adapter (OpenDocument):** Bidirectional parser and generator conforming to OASIS OpenDocument (ISO/IEC 26300).
- **Image Adapter:** Memory-aware bitmap decoding (`BitmapRegionDecoder`), Exif orientation normalization, and downsampling.

### 3.5 Text Layout, Pagination & Rendering Engine (Proposed)
- **Flow Layout Engine:** Computes line breaks, hyphenation, and word boundaries using Android font metrics.
- **Pagination Engine:** Segments continuous content into pages with margin constraints and widow/orphan control.
- **Tile Caching:** LRU memory cache for rendered page bitmaps to maintain smooth 60fps scrolling without memory spikes.

### 3.6 Editing & Mutation Engine (Proposed)
- Command/Transaction model encapsulating edits into reversible operations for robust Undo/Redo history.
- Caret positioning and selection geometry mapping touch gestures to character offsets in the document model.

---

## 4. Resource & Performance Management Strategy

- **Heap Budgeting:** In-memory page bitmap caches will enforce strict caps (e.g. maximum 25% of available JVM heap) with explicit bitmap recycling to prevent Out-Of-Memory errors.
- **Background Dispatching:** File I/O, XML parsing, layout computation, and conversion tasks execute strictly on background coroutine dispatchers (`Dispatchers.IO`, `Dispatchers.Default`). The main thread (`Dispatchers.Main`) is reserved exclusively for UI rendering and gesture handling.
- **Graceful Error Handling:** Malformed files, corrupted archives, and unsupported XML tags must trigger user-visible recovery states rather than unhandled fatal exceptions.

---

## 5. Security & License Governance

- **Untrusted File Input:** All external files are treated as untrusted. XML parsers enforce secure processing (`XMLConstants.FEATURE_SECURE_PROCESSING`), disallow DOCTYPE declarations where appropriate, and disable external entity resolution (`XXE`).
- **Zero Network Exposure:** The application core requires no network access and declares no internet permissions.
- **License Compatibility:** All candidate libraries must be rigorously reviewed for compatibility with the **Apache License 2.0**. Copyleft or ambiguous licenses that compromise distribution terms cannot be accepted into the core codebase.
