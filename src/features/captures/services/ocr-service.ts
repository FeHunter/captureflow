import { extractTextFromImage, isSupported } from "expo-text-extractor";

/** Extracts visible text locally using the platform OCR engine. No image is uploaded. */
export async function extractImageText(imageUri: string): Promise<string> {
  if (!isSupported) {
    throw new Error("O reconhecimento de texto não é compatível com este dispositivo.");
  }

  const lines = await extractTextFromImage(imageUri);
  return lines.map((line) => line.trim()).filter(Boolean).join("\n");
}
