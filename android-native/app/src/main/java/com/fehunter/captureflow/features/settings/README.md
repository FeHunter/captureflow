# Settings

Feature responsável pelas preferências locais do aplicativo.

## Escopo atual
- Preferência de tema: `system`, `light` ou `dark`.
- Persistência por SharedPreferences, encapsulada em `SettingsRepository`.
- Presentation observa e altera o tema por `SettingsViewModel` e UseCases.

As preferências permanecem locais ao dispositivo. Novas configurações devem seguir o mesmo limite Domain/Data/Presentation.
