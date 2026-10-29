# Captures

The feature receives, classifies, persists, searches and manages link, text and image references.

## Current native baseline
- Manual text/link entry.
- Android Sharesheet `ACTION_SEND` for `text/plain` and `image/*`, including image URIs supplied through `EXTRA_STREAM` or `ClipData`.
- Shared images are copied to app-private storage before persistence when the provider allows reading them.
- Basic title/category suggestions and exact-content duplicate avoidance for text; each shared image is retained as its own capture.
- Room persistence, search, category filters, completion and deletion.
- On-demand Google ML Kit OCR runs on device; extracted text replaces the generic image placeholder and is saved locally.
- OCR displays processing, success, empty-result and error feedback.

## Rules and gaps
- Keep source app and source URL optional unless verified.
- OCR output is extracted content, not verified metadata; users should review it before relying on it.
- No captured content or image is uploaded.
- Screenshot discovery is not implemented; users currently share an image/screenshot to CaptureFlow explicitly.
- If copying a shared image fails, the original URI is retained as a fallback; OCR may fail later if the provider's temporary access expires.
- Validate sharing and OCR against multiple gallery apps and real devices.
