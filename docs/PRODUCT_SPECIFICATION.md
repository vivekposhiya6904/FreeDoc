# FreeDoc — Product Specification & Long-Term Feature Scope

**Document Version:** 1.0.0  
**Status:** Approved Product Scope & Acceptance Criteria  
**Target Platform:** Android (API Level 26+ / Android 8.0 Oreo and above)  
**Reference Motto:** *"Your Documents. Your Device. Your Control."*

---

## 1. Product Overview & Purpose

FreeDoc is a privacy-first, free, offline Android document workspace that enables users to view, create, edit, and convert documents across standard open and industry-wide formats (PDF, DOCX, ODT, and common images) directly on their mobile devices.

The application operates as an autonomous, on-device utility suite designed to function without dependence on cloud backends, forced registration, telemetry tracking, advertising networks, or subscription walls.

### Scope Notice & Implementation Status
> **Important Distinction:** This document specifies the **approved long-term feature scope, product requirements, and future acceptance criteria**. 
> At the current stage (Phase 0), the repository provides the Android application foundation, user interface shell, and an isolated proof-of-concept document engine. 
> The viewing, editing, and conversion features documented below represent targets for upcoming implementation phases and are **not** claimed as fully operational user features in the current build.

---

## 2. Core Principles & Non-Functional Requirements

### 2.1 Privacy, Autonomy & Offline Operation
- **Strictly Offline Core Operation:** Every advertised core feature must execute entirely offline without an active network connection.
- **No Cloud Dependencies:** No document content, file metadata, or user telemetry may be transmitted to external servers for processing.
- **No User Accounts:** No sign-up, login, social authentication, or profile management is required or prompted.
- **Zero Advertising & Clean Output:** No in-app advertisements, promotional interstitial screens, or forced watermarks on exported, converted, or printed files.
- **No Subscriptions for Core Scope:** Core reading, writing, editing, and conversion workflows must remain freely accessible without recurring fees.

### 2.2 Performance & System Footprint
- **Measured Resource Efficiency:** The application architecture must actively budget memory, avoid leaks, recycle render bitmaps, and stream I/O.
- **Evidence-Based Performance:** We do not make unsupported numerical performance guarantees; performance targets and package-size thresholds must be empirically measured and documented through automated profiling suites on target Android devices.
- **Package Size Optimization:** Libraries and native dependencies must be audited to prevent unnecessary APK/AAB bloat. Redundant or oversized dependencies must be avoided or modularized.
- **Battery & Thermal Consideration:** Heavy document processing (e.g., rasterizing hundreds of pages or complex layout analysis) must run on appropriate background dispatchers with cooperative cancellation to protect battery life and UI responsiveness.

### 2.3 Data Integrity & Safety
- **Non-Destructive File Operations:** The original user file is treated as immutable by default. Any save operation defaults to a new version or requires explicit user authorization before overwriting.
- **Atomic Writes:** File writing must utilize temporary staging files and atomic rename operations to prevent partial or corrupted files during crashes, device shutdowns, or out-of-storage conditions.
- **Output Validation:** All export and conversion pipelines must include a post-generation verification step to validate structural format compliance before reporting completion to the user.

---

## 3. Functional Requirements: Document Viewing

FreeDoc will provide a unified reading interface capable of displaying heterogeneous document formats with smooth navigation.

### 3.1 PDF Viewing
- Multi-page continuous vertical and single-page horizontal view modes.
- Pinch-to-zoom with crisp text scaling and high-DPI tile rendering.
- Text selection, search within document, and copy to clipboard.
- Table of contents (PDF document outlines/bookmarks) navigation.
- Jump-to-page navigation and thumbnail grid preview.
- Support for password-protected PDF files with on-device decryption.

### 3.2 DOCX Viewing
- Structured layout rendering of Microsoft Word (`.docx`) files.
- Visual support for headings, paragraphs, styled text, bulleted/numbered lists, and block quotes.
- Table rendering with borders, column spans, row spans, and cell alignments.
- Inline and anchored image placement.
- Basic footnote and endnote reference display.

### 3.3 ODT Viewing
- Structured layout rendering of OpenDocument Text (`.odt`) files conforming to the ODF specification.
- Paragraph styling, inline text attributes, lists, headers, footers, and table rendering.
- Extraction and rendering of embedded image assets.

### 3.4 Image Viewing
- Support for common image formats: PNG, JPEG, WebP, and BMP.
- Pan, zoom, rotate, and image information inspection (dimensions, file size, color depth).

---

## 4. Functional Requirements: Creation & Editing

FreeDoc aims to provide an intuitive, responsive document authoring environment capable of generating standard office documents.

### 4.1 Document Creation from Scratch
- Create blank documents with configurable page geometries (A4, US Letter, Legal).
- Configure initial orientation (Portrait or Landscape) and standard page margins.

### 4.2 Text & Paragraph Editing
- **Typography & Font Styling:** Bold, italic, underline, strikethrough, subscript, superscript, font family, font size, and text color.
- **Paragraph Controls:** Alignment (left, center, right, justified), line spacing (single, 1.15, 1.5, double), and paragraph before/after spacing.
- **List Structures:** Ordered (numbered) lists, unordered (bulleted) lists, and nested indentation levels.
- **Structural Headings:** Heading 1, Heading 2, Heading 3, and standard body text styles.

### 4.3 Table Operations
- Insert tables with configurable rows and columns.
- Add or delete rows and columns.
- Adjust cell alignment, border styles, and cell background colors.
- Merge and split table cells where supported by the internal model.

### 4.4 Image Insertion & Manipulation
- Import images from device storage via the system photo picker or file picker.
- Move, resize, and maintain aspect ratio of embedded images.
- Text wrapping configuration (inline with text, break text, top and bottom).

### 4.5 Page Layout & Section Management
- Page setup adjustments: Margins (Normal, Narrow, Wide, Custom), page orientation, and sheet size.
- Headers and footers with custom text and automated page numbering fields (e.g., "Page X of Y").
- Manual page breaks and section breaks.

### 4.6 Editing UX & History
- Comprehensive Undo and Redo stack for all document mutation commands.
- Smooth cursor positioning, selection handles, and standard Android cut/copy/paste integration.

---

## 5. Functional Requirements: Conversion & Export

The conversion system provides bidirectional format interchange on-device.

### 5.1 Image & PDF Pipelines
- **Images to PDF:**
  - Select single or multiple images.
  - Reorder pages via drag-and-drop.
  - Page fitting options (Fit to Page, Stretch, Fill, Maintain Aspect Ratio).
  - Configurable page size and compression quality to balance visual fidelity against output file size.
- **PDF Pages to Images:**
  - Export all pages or a selected page range.
  - Select output format (PNG, JPEG, WebP).
  - Configurable rendering DPI (e.g., 150 DPI for drafts, 300 DPI for high-resolution archiving).

### 5.2 Office Document Conversion
- **DOCX to PDF:** Direct conversion of styled DOCX documents into static PDF files with font embedding and layout preservation.
- **ODT to PDF:** Direct conversion of ODT documents into standard PDF files.
- **DOCX ↔ ODT Interchange:** Conversion between Microsoft Word (`.docx`) and OpenDocument Text (`.odt`) formats, mapping core styles, tables, and images.

### 5.3 PDF to Editable DOCX (Advanced Conversion)
- Extract text blocks, font information, and spatial positions from PDF pages.
- Run heuristics to reconstruct paragraphs, reading order, headings, and column flows.
- Reconstruct tabular structures from detected line vectors and cell bounding boxes.
- Extract embedded raster images and position them relative to neighboring text flow.
- Generate a standards-compliant `.docx` document.

### 5.4 Export Formats
- Export edited internal documents to PDF, DOCX, or ODT.

---

## 6. Functional Requirements: File Handling & System Integration

### 6.1 Storage Access Framework (SAF)
- Integration with Android's Storage Access Framework (`ACTION_OPEN_DOCUMENT`, `ACTION_CREATE_DOCUMENT`, `ACTION_OPEN_DOCUMENT_TREE`).
- Full support for Scoped Storage without demanding broad legacy storage permissions (`READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE`).
- Seamless operation with external SD cards, USB OTG drives, and SAF-compliant file providers.

### 6.2 File Resilience & Large Documents
- Streaming parser implementation where possible to avoid loading multi-gigabyte or hundreds-of-megabytes files fully into heap memory.
- Progressive page rendering for PDF viewing: only visible and adjacent buffer pages are loaded into memory.
- Safe temporary file cleanup on application termination or crash recovery.

### 6.3 User Feedback & Validation
- Detailed, user-friendly error messages that diagnose issues (e.g., "Corrupted PDF header", "Unsupported password algorithm", "Insufficient device storage space") rather than silent failures or generic crashes.
- Progress reporting with cancellation support for long-running batch conversions or export jobs.

---

## 7. Known Engineering Realities & Honest Scope Boundaries

FreeDoc is committed to technical accuracy and transparent communication:

### 7.1 The Inherent Challenge of PDF Reconstruction
- **No Guaranteed "Perfect" PDF-to-DOCX Conversion:** PDF documents are designed for visual presentation, not semantic structuring. A PDF file often contains individual characters positioned at absolute X/Y coordinates without any encoding for words, paragraphs, lines, columns, or tables.
- **Heuristic Rebuilding:** Converting a PDF into an editable Word document involves reverse-engineering human-intended structures using spatial clustering and geometric heuristics. Complex layouts (e.g., magazine spreads, overlapping graphics, forms with intricate lines, scanned PDFs without OCR) will exhibit varying degrees of fidelity. FreeDoc will strive for maximum practical usability, but **does not promise perfect conversion across every document**.

### 7.2 Complex Office Document Rendering
- The DOCX and ODT specifications contain thousands of edge cases developed over decades of desktop office suite evolution.
- Features such as dynamic macro execution, embedded ActiveX/OLE objects, complex animated shapes, and advanced SmartArt are outside the core mobile scope. The engine will gracefully ignore or simplify unsupported complex elements while preserving text, tables, and images.

### 7.3 OCR (Optical Character Recognition)
- Initial PDF conversion and text extraction assume vector/digital PDFs containing text streams.
- Scanned (bitmap-only) PDFs require optical character recognition. OCR integration is a specialized capability scheduled for later evaluation and is not part of the initial baseline.

---

## 8. Verification & Acceptance Regimen

Before any feature in this specification is declared complete during development, it must satisfy the following testing regimen:

1. **Unit Test Suite:** Format parsers and serializers must maintain automated test coverage for structure parsing, XML handling, and geometry calculations.
2. **Corpus Compatibility Testing:** Document parsers must be validated against a curated corpus of real-world documents:
   - PDFs from various generators (Adobe, Chrome, LibreOffice, LaTeX, Quartz).
   - DOCX files created by different versions of Microsoft Office, LibreOffice, and Google Docs.
   - ODT documents created by LibreOffice and Apache OpenOffice.
   - Malformed and edge-case documents to verify robust error handling without crashes.
3. **Round-Trip Integrity Tests:** Converting Document A to Document B and back or exporting an edited file must produce valid files that pass third-party format validators without schema violation or document corruption.
4. **Offline Conformance:** Automated tests must run in isolated environments without network access to guarantee offline functionality.
5. **Physical Device Profiling:** Memory footprint and frame rate during scrolling must be verified on minimum-spec physical Android hardware.
