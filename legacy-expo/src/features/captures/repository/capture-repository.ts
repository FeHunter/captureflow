import * as SQLite from "expo-sqlite";
import type { Capture, CreateCaptureInput } from "../model/capture";

const databasePromise = SQLite.openDatabaseAsync("captureflow.db");

export async function initializeCaptureRepository(): Promise<void> {
  const database = await databasePromise;
  await database.execAsync(`
    PRAGMA journal_mode = WAL;
    CREATE TABLE IF NOT EXISTS captures (
      id TEXT PRIMARY KEY NOT NULL,
      title TEXT NOT NULL,
      content TEXT NOT NULL,
      kind TEXT NOT NULL,
      category TEXT NOT NULL,
      source_url TEXT,
      image_uri TEXT,
      created_at TEXT NOT NULL,
      is_completed INTEGER NOT NULL DEFAULT 0
    );
  `);
}

export async function listCaptures(): Promise<Capture[]> {
  const database = await databasePromise;
  const rows = await database.getAllAsync<{
    id: string; title: string; content: string; kind: Capture["kind"];
    category: Capture["category"]; source_url: string | null; image_uri: string | null;
    created_at: string; is_completed: number;
  }>("SELECT * FROM captures ORDER BY created_at DESC");
  return rows.map((row) => ({
    id: row.id, title: row.title, content: row.content, kind: row.kind,
    category: row.category, sourceUrl: row.source_url, imageUri: row.image_uri,
    createdAt: row.created_at, isCompleted: row.is_completed === 1,
  }));
}

export async function saveCapture(input: CreateCaptureInput): Promise<Capture> {
  const database = await databasePromise;
  const content = input.content.trim();
  const urlMatch = content.match(/https?:\/\/[^\s]+/i);
  const kind: Capture["kind"] = input.imageUri ? "image" : urlMatch ? "link" : "text";
  const sourceUrl = urlMatch?.[0] ?? null;
  const title = input.title?.trim() || inferTitle(content, kind);
  const category = inferCategory(content);
  const capture: Capture = {
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    title, content, kind, category, sourceUrl, imageUri: input.imageUri ?? null,
    createdAt: new Date().toISOString(), isCompleted: false,
  };
  await database.runAsync(
    "INSERT INTO captures (id, title, content, kind, category, source_url, image_uri, created_at, is_completed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)",
    capture.id, capture.title, capture.content, capture.kind, capture.category,
    capture.sourceUrl, capture.imageUri, capture.createdAt,
  );
  return capture;
}

export async function toggleCaptureCompleted(id: string, completed: boolean): Promise<void> {
  const database = await databasePromise;
  await database.runAsync("UPDATE captures SET is_completed = ? WHERE id = ?", completed ? 1 : 0, id);
}

export async function deleteCapture(id: string): Promise<void> {
  const database = await databasePromise;
  await database.runAsync("DELETE FROM captures WHERE id = ?", id);
}

function inferTitle(content: string, kind: Capture["kind"]): string {
  if (kind === "image") return "Imagem salva";
  const clean = content.replace(/https?:\/\/[^\s]+/gi, "").replace(/\s+/g, " ").trim();
  if (!clean && kind === "link") {
    try { return new URL(content.match(/https?:\/\/[^\s]+/i)?.[0] ?? content).hostname.replace(/^www\./, ""); }
    catch { return "Link salvo"; }
  }
  return (clean || content).slice(0, 72) || "Nova referência";
}

function inferCategory(content: string): Capture["category"] {
  const normalized = content.toLocaleLowerCase();
  if (/todo|to-do|checklist|preciso|lembrar|comprar|fazer|\[ \]/.test(normalized)) return "Tarefas";
  if (/ideia|inspir|design|criar|projeto/.test(normalized)) return "Ideias";
  if (/https?:\/\//.test(normalized)) return "Ler depois";
  return "Referência";
}
