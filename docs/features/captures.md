# Captures

The feature receives, classifies, persists, searches and manages link, text and image references.

## Current native baseline
- Manual text/link entry.
- Android Sharesheet `ACTION_SEND` for `text/plain` and `image/*`.
- Basic title/category suggestions and exact-content duplicate avoidance.
- Room persistence, search, category filters, completion and deletion.
- On-demand Google ML Kit OCR; extracted text is saved locally.

## Rules and gaps
- Keep source app and source URL optional unless verified.
- OCR output is extracted content, not verified metadata.
- No captured content is uploaded.
- Screenshot discovery is not implemented.
- Shared image URI access varies by provider; verify it and copy to app-private storage if durable access is required.
