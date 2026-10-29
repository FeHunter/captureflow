/** On-device OCR adapter. Loaded lazily so an older native binary does not crash app startup. */
export async function extractImageText(imageUri: string): Promise<string> {
  let ocr: typeof import("expo-text-extractor");
  try {
    ocr = await import("expo-text-extractor");
  } catch {
    throw new Error(
      "O OCR ainda não está incluído nesta versão instalada do CaptureFlow. Recompile e reinstale o app Android com `npx expo run:android`, depois reinicie o Metro com `npx expo start -c`."
    );
  }

  if (!ocr.isSupported) {
    throw new Error("O reconhecimento de texto não é compatível com este dispositivo.");
  }

  const lines = await ocr.extractTextFromImage(imageUri);
  return lines.map((line) => line.trim()).filter(Boolean).join("\n");
}
