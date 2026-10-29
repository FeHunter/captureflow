import { useState } from "react";
import { Alert, Image, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";
import * as ImagePicker from "expo-image-picker";
import { colors } from "@/shared/theme/colors";
import type { Capture } from "../model/capture";
import { useCaptureViewModel } from "../view_models/use-capture-view-model";

const filters = ["Todas", "Pendentes", "Concluídas"] as const;

export function CaptureHomeView() {
  const vm = useCaptureViewModel();
  const [draft, setDraft] = useState("");
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const pickImage = async () => {
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) {
      Alert.alert("Permissão necessária", "Permita o acesso às fotos para importar uma screenshot.");
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ["images"], quality: 0.9 });
    if (!result.canceled && result.assets[0]) setSelectedImage(result.assets[0].uri);
  };

  const submit = async () => {
    if (!draft.trim() && !selectedImage) {
      Alert.alert("Adicione um conteúdo", "Cole um link, escreva uma nota ou escolha uma imagem.");
      return;
    }
    setSaving(true);
    const success = await vm.addCapture(draft, selectedImage);
    setSaving(false);
    if (success) { setDraft(""); setSelectedImage(null); }
  };

  return (
    <KeyboardAvoidingView style={styles.screen} behavior={Platform.OS === "ios" ? "padding" : undefined}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <View style={styles.header}>
          <View style={styles.brandMark}><Text style={styles.brandLetter}>C</Text></View>
          <View style={styles.headerText}>
            <Text style={styles.eyebrow}>SEU ESPAÇO DE CAPTURA</Text>
            <Text style={styles.title}>CaptureFlow</Text>
          </View>
          <View style={styles.countPill}><Text style={styles.countText}>{vm.captures.length}</Text></View>
        </View>

        <Text style={styles.headline}>Tire da cabeça.{ "\n" }Guarde aqui.</Text>
        <Text style={styles.subtitle}>Links, ideias e imagens em um lugar só — organizados para você.</Text>

        <View style={styles.captureCard}>
          <Text style={styles.sectionTitle}>Nova captura</Text>
          <TextInput
            value={draft}
            onChangeText={setDraft}
            placeholder="Cole um link ou escreva uma ideia..."
            placeholderTextColor={colors.muted}
            multiline
            style={styles.input}
            accessibilityLabel="Conteúdo para salvar"
          />
          {selectedImage ? (
            <View style={styles.imagePreview}>
              <Image source={{ uri: selectedImage }} style={styles.previewImage} />
              <Pressable onPress={() => setSelectedImage(null)} accessibilityLabel="Remover imagem"><Text style={styles.removeImage}>Remover imagem ×</Text></Pressable>
            </View>
          ) : null}
          <View style={styles.actions}>
            <Pressable style={styles.secondaryButton} onPress={pickImage}>
              <Text style={styles.secondaryButtonText}>＋ Imagem / screenshot</Text>
            </Pressable>
            <Pressable style={[styles.primaryButton, saving && styles.disabledButton]} onPress={submit} disabled={saving}>
              <Text style={styles.primaryButtonText}>{saving ? "Salvando..." : "Salvar captura"}</Text>
            </Pressable>
          </View>
          <Text style={styles.helper}>Organização básica automática • Dados salvos no dispositivo</Text>
        </View>

        <View style={styles.listHeader}>
          <View><Text style={styles.sectionTitle}>Sua biblioteca</Text><Text style={styles.listSubtitle}>{vm.captures.length ? "Tudo o que você guardou." : "Suas próximas descobertas começam aqui."}</Text></View>
        </View>
        <TextInput value={vm.query} onChangeText={vm.setQuery} placeholder="Buscar referências..." placeholderTextColor={colors.muted} style={styles.search} />
        <View style={styles.filters}>
          {filters.map((filter) => (
            <Pressable key={filter} onPress={() => vm.setFilter(filter)} style={[styles.filter, vm.filter === filter && styles.activeFilter]}>
              <Text style={[styles.filterText, vm.filter === filter && styles.activeFilterText]}>{filter}</Text>
            </Pressable>
          ))}
        </View>

        {vm.error ? <Text style={styles.error}>{vm.error}</Text> : null}
        {!vm.isReady ? <Text style={styles.emptyText}>Preparando seu espaço...</Text> : null}
        {vm.isReady && vm.visibleCaptures.length === 0 ? (
          <View style={styles.emptyCard}><Text style={styles.emptyIcon}>✳</Text><Text style={styles.emptyTitle}>Nada por aqui ainda</Text><Text style={styles.emptyText}>Salve um link, uma ideia ou importe uma screenshot para começar.</Text></View>
        ) : vm.visibleCaptures.map((item) => <CaptureCard key={item.id} item={item} onToggle={() => void vm.toggleCompleted(item)} onDelete={() => Alert.alert("Excluir captura?", "Esta ação não pode ser desfeita.", [{ text: "Cancelar", style: "cancel" }, { text: "Excluir", style: "destructive", onPress: () => void vm.removeCapture(item.id) }])} />)}
        <Text style={styles.footer}>Privado por padrão. Suas capturas ficam neste dispositivo.</Text>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

function CaptureCard({ item, onToggle, onDelete }: { item: Capture; onToggle: () => void; onDelete: () => void }) {
  const icon = item.kind === "link" ? "↗" : item.kind === "image" ? "▧" : "≡";
  return (
    <View style={styles.itemCard}>
      <View style={styles.itemTop}>
        <View style={styles.itemIcon}><Text style={styles.itemIconText}>{icon}</Text></View>
        <View style={styles.itemMain}>
          <Text numberOfLines={2} style={[styles.itemTitle, item.isCompleted && styles.completedTitle]}>{item.title}</Text>
          <Text style={styles.itemMeta}>{item.category} · {new Date(item.createdAt).toLocaleDateString("pt-BR")}</Text>
        </View>
        <Pressable onPress={onToggle} style={[styles.check, item.isCompleted && styles.checked]} accessibilityLabel={item.isCompleted ? "Marcar como pendente" : "Marcar como concluída"}>
          <Text style={styles.checkText}>{item.isCompleted ? "✓" : ""}</Text>
        </Pressable>
      </View>
      {item.imageUri ? <Image source={{ uri: item.imageUri }} style={styles.itemImage} resizeMode="cover" /> : null}
      {item.content && item.kind !== "image" ? <Text numberOfLines={3} style={styles.itemContent}>{item.content}</Text> : null}
      <View style={styles.itemBottom}><Text style={styles.typeLabel}>{item.kind === "link" ? "LINK" : item.kind === "image" ? "IMAGEM" : "NOTA"}</Text><Pressable onPress={onDelete}><Text style={styles.deleteText}>Excluir</Text></Pressable></View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  content: { padding: 22, paddingTop: 24, paddingBottom: 40 },
  header: { flexDirection: "row", alignItems: "center", marginBottom: 28 },
  brandMark: { width: 46, height: 46, borderRadius: 15, backgroundColor: colors.primary, alignItems: "center", justifyContent: "center" },
  brandLetter: { color: "#FFF", fontSize: 24, fontWeight: "900" },
  headerText: { marginLeft: 12, flex: 1 },
  eyebrow: { fontSize: 9, fontWeight: "800", color: colors.primary, letterSpacing: 1.2, marginBottom: 3 },
  title: { fontSize: 20, fontWeight: "800", color: colors.text },
  countPill: { minWidth: 36, height: 36, borderRadius: 18, backgroundColor: "#EAE4FF", alignItems: "center", justifyContent: "center" },
  countText: { color: colors.primary, fontWeight: "800" },
  headline: { fontSize: 34, lineHeight: 39, letterSpacing: -1, fontWeight: "900", color: colors.text },
  subtitle: { fontSize: 15, lineHeight: 22, color: colors.muted, marginTop: 10, marginBottom: 24, maxWidth: 330 },
  captureCard: { padding: 18, backgroundColor: colors.surface, borderRadius: 22, borderWidth: 1, borderColor: colors.border, marginBottom: 30 },
  sectionTitle: { fontSize: 17, fontWeight: "800", color: colors.text },
  input: { minHeight: 88, maxHeight: 150, textAlignVertical: "top", padding: 13, borderRadius: 14, borderWidth: 1, borderColor: colors.border, marginTop: 14, color: colors.text, fontSize: 14, lineHeight: 21 },
  imagePreview: { marginTop: 12 },
  previewImage: { width: "100%", height: 130, borderRadius: 12 },
  removeImage: { color: colors.warning, fontSize: 12, fontWeight: "700", marginTop: 6, alignSelf: "flex-end" },
  actions: { flexDirection: "row", flexWrap: "wrap", gap: 8, marginTop: 12 },
  secondaryButton: { flexGrow: 1, borderRadius: 12, paddingVertical: 13, paddingHorizontal: 12, backgroundColor: "#F1EDFF", alignItems: "center" },
  secondaryButtonText: { color: colors.primary, fontSize: 12, fontWeight: "800" },
  primaryButton: { flexGrow: 1, borderRadius: 12, paddingVertical: 13, paddingHorizontal: 14, backgroundColor: colors.primary, alignItems: "center" },
  primaryButtonText: { color: "#FFF", fontSize: 12, fontWeight: "800" },
  disabledButton: { opacity: 0.6 },
  helper: { fontSize: 11, lineHeight: 16, color: colors.muted, marginTop: 12 },
  listHeader: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  listSubtitle: { fontSize: 12, color: colors.muted, marginTop: 4 },
  search: { height: 46, borderRadius: 13, borderWidth: 1, borderColor: colors.border, backgroundColor: colors.surface, marginTop: 16, paddingHorizontal: 14, color: colors.text },
  filters: { flexDirection: "row", gap: 8, marginTop: 12, marginBottom: 14 },
  filter: { borderRadius: 18, borderWidth: 1, borderColor: colors.border, paddingHorizontal: 13, paddingVertical: 8, backgroundColor: colors.surface },
  activeFilter: { backgroundColor: colors.primary, borderColor: colors.primary },
  filterText: { color: colors.muted, fontSize: 11, fontWeight: "700" },
  activeFilterText: { color: "#FFF" },
  error: { color: "#B42318", marginBottom: 12, fontSize: 12 },
  emptyCard: { borderRadius: 18, borderWidth: 1, borderStyle: "dashed", borderColor: colors.border, alignItems: "center", padding: 24, backgroundColor: "#FFFFFF90" },
  emptyIcon: { color: colors.primary, fontSize: 26, marginBottom: 8 },
  emptyTitle: { color: colors.text, fontSize: 15, fontWeight: "800", marginBottom: 5 },
  emptyText: { color: colors.muted, textAlign: "center", fontSize: 12, lineHeight: 18 },
  itemCard: { backgroundColor: colors.surface, borderRadius: 18, borderWidth: 1, borderColor: colors.border, padding: 15, marginBottom: 11 },
  itemTop: { flexDirection: "row", alignItems: "flex-start" },
  itemIcon: { width: 36, height: 36, borderRadius: 11, backgroundColor: "#F1EDFF", alignItems: "center", justifyContent: "center" },
  itemIconText: { color: colors.primary, fontWeight: "900", fontSize: 17 },
  itemMain: { flex: 1, marginLeft: 10, marginRight: 8 },
  itemTitle: { color: colors.text, fontSize: 14, lineHeight: 19, fontWeight: "800" },
  completedTitle: { textDecorationLine: "line-through", color: colors.muted },
  itemMeta: { color: colors.muted, fontSize: 10, marginTop: 5 },
  check: { width: 24, height: 24, borderRadius: 8, borderWidth: 1.5, borderColor: colors.border, alignItems: "center", justifyContent: "center" },
  checked: { backgroundColor: colors.success, borderColor: colors.success },
  checkText: { color: "#FFF", fontWeight: "900", fontSize: 13 },
  itemImage: { width: "100%", height: 160, borderRadius: 12, marginTop: 12, backgroundColor: colors.background },
  itemContent: { color: colors.muted, fontSize: 12, lineHeight: 18, marginTop: 10 },
  itemBottom: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginTop: 13, paddingTop: 10, borderTopWidth: 1, borderTopColor: colors.border },
  typeLabel: { color: colors.primary, fontSize: 9, letterSpacing: 1, fontWeight: "900" },
  deleteText: { color: colors.warning, fontSize: 11, fontWeight: "700" },
  footer: { textAlign: "center", color: colors.muted, fontSize: 11, marginTop: 18 },
});
