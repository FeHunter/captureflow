export type CaptureStatus = "inbox" | "organized" | "completed";

export type MetadataOrigin =
  | "share-intent"
  | "page-metadata"
  | "ocr"
  | "user"
  | "ai-inference";

export interface CapturedMetadataField<T> {
  value: T;
  origin: MetadataOrigin;
  verified: boolean;
}

export interface CapturedTask {
  id: string;
  title: string;
  completed: boolean;
  createdAt: string;
}

export interface CapturedItem {
  id: string;
  createdAt: string;
  updatedAt: string;
  imageUri?: string;
  sourceApp?: CapturedMetadataField<string>;
  sourceUrl?: CapturedMetadataField<string>;
  title: CapturedMetadataField<string>;
  description?: CapturedMetadataField<string>;
  extractedText?: string;
  category?: string;
  status: CaptureStatus;
  tasks: CapturedTask[];
}
