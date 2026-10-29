export type CaptureKind = "link" | "text" | "image";
export type CaptureCategory = "Ler depois" | "Ideias" | "Tarefas" | "Referência";

export interface Capture {
  id: string;
  title: string;
  content: string;
  kind: CaptureKind;
  category: CaptureCategory;
  sourceUrl: string | null;
  imageUri: string | null;
  createdAt: string;
  isCompleted: boolean;
}

export interface CreateCaptureInput {
  content: string;
  imageUri?: string | null;
  title?: string;
}
