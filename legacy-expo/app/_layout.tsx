import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";

export default function RootLayout() {
  return (
    <>
      <StatusBar style="auto" />
      <Stack
        screenOptions={{
          headerStyle: { backgroundColor: "#F7F5FF" },
          headerTintColor: "#29213D",
          contentStyle: { backgroundColor: "#F7F5FF" },
        }}
      >
        <Stack.Screen name="index" options={{ title: "CaptureFlow" }} />
      </Stack>
    </>
  );
}
