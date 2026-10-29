import { StyleSheet, Text, View } from "react-native";

const colors = {
  background: "#F7F5FF",
  surface: "#FFFFFF",
  primary: "#6D4AFF",
  text: "#29213D",
  muted: "#756D87",
  border: "#E7E1F2",
};

export default function HomeScreen() {
  return (
    <View style={styles.screen}>
      <View style={styles.hero}>
        <View style={styles.mark}>
          <Text style={styles.markText}>C</Text>
        </View>
        <Text style={styles.eyebrow}>SUA CAIXA DE ENTRADA INTELIGENTE</Text>
        <Text style={styles.title}>{"Capture agora.\nOrganize depois."}</Text>
        <Text style={styles.description}>
          Salve links, imagens e ideias em um só lugar. Transforme o que você
          encontra em checklists que ajudam você a agir.
        </Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.cardTitle}>Comece por compartilhar</Text>
        <Text style={styles.cardDescription}>
          Quando a entrada nativa estiver implementada, você poderá enviar um
          link, texto ou imagem de outro aplicativo diretamente para o
          CaptureFlow.
        </Text>
        <View style={styles.status}>
          <View style={styles.statusDot} />
          <Text style={styles.statusText}>Base do projeto criada</Text>
        </View>
      </View>

      <Text style={styles.footer}>Feito para guardar o que importa.</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
    paddingHorizontal: 24,
    paddingTop: 36,
    paddingBottom: 24,
    justifyContent: "space-between",
  },
  hero: { alignItems: "flex-start" },
  mark: {
    width: 56, height: 56, borderRadius: 18, backgroundColor: colors.primary,
    alignItems: "center", justifyContent: "center", marginBottom: 28,
  },
  markText: { color: "#FFFFFF", fontSize: 28, fontWeight: "800" },
  eyebrow: {
    color: colors.primary, fontSize: 11, fontWeight: "800",
    letterSpacing: 1.5, marginBottom: 12,
  },
  title: {
    color: colors.text, fontSize: 34, lineHeight: 40, fontWeight: "800",
    letterSpacing: -0.8, marginBottom: 16,
  },
  description: {
    color: colors.muted, fontSize: 16, lineHeight: 24, maxWidth: 360,
  },
  card: {
    backgroundColor: colors.surface, borderColor: colors.border,
    borderWidth: 1, borderRadius: 24, padding: 20,
  },
  cardTitle: {
    color: colors.text, fontSize: 18, fontWeight: "700", marginBottom: 8,
  },
  cardDescription: { color: colors.muted, fontSize: 14, lineHeight: 21 },
  status: {
    flexDirection: "row", alignItems: "center", marginTop: 20, paddingTop: 16,
    borderTopWidth: 1, borderTopColor: colors.border,
  },
  statusDot: {
    width: 8, height: 8, borderRadius: 4, backgroundColor: "#2E9B68",
    marginRight: 8,
  },
  statusText: { color: colors.text, fontSize: 12, fontWeight: "600" },
  footer: { textAlign: "center", color: colors.muted, fontSize: 12 },
});
