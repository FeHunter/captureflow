import { useCallback, useEffect, useMemo, useState } from "react";
import type { Capture } from "../model/capture";
import { extractImageText } from "../services/ocr-service";
import { deleteCapture, initializeCaptureRepository, listCaptures, saveCapture, toggleCaptureCompleted } from "../repository/capture-repository";

export function useCaptureViewModel() {
  const [captures, setCaptures] = useState<Capture[]>([]);
  const [isReady, setIsReady] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState<"Todas" | "Pendentes" | "Concluídas">("Todas");

  const refresh = useCallback(async () => {
    try {
      const items = await listCaptures();
      setCaptures(items);
      setError(null);
    } catch {
      setError("Não foi possível carregar suas capturas.");
    }
  }, []);

  useEffect(() => {
    let active = true;
    initializeCaptureRepository()
      .then(() => listCaptures())
      .then((items) => { if (active) { setCaptures(items); setIsReady(true); } })
      .catch(() => { if (active) { setError("Falha ao iniciar o armazenamento local."); setIsReady(true); } });
    return () => { active = false; };
  }, []);

  const extractText = useCallback(async (imageUri: string): Promise<string | null> => {
    try {
      const text = await extractImageText(imageUri);
      setError(null);
      return text;
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Não foi possível extrair o texto da imagem.");
      return null;
    }
  }, []);

  const addCapture = useCallback(async (content: string, imageUri?: string | null) => {
    if (!content.trim() && !imageUri) return false;
    try {
      await saveCapture({ content: content.trim(), imageUri });
      await refresh();
      return true;
    } catch {
      setError("Não foi possível salvar. Tente novamente.");
      return false;
    }
  }, [refresh]);

  const toggleCompleted = useCallback(async (item: Capture) => {
    await toggleCaptureCompleted(item.id, !item.isCompleted);
    await refresh();
  }, [refresh]);

  const removeCapture = useCallback(async (id: string) => {
    await deleteCapture(id);
    await refresh();
  }, [refresh]);

  const visibleCaptures = useMemo(() => captures.filter((item) => {
    const matchesQuery = `${item.title} ${item.content} ${item.category}`.toLocaleLowerCase().includes(query.toLocaleLowerCase());
    const matchesFilter = filter === "Todas" || (filter === "Concluídas" ? item.isCompleted : !item.isCompleted);
    return matchesQuery && matchesFilter;
  }), [captures, filter, query]);

  return { captures, visibleCaptures, isReady, error, query, setQuery, filter, setFilter, addCapture, extractText, toggleCompleted, removeCapture, refresh };
}
