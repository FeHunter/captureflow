# Settings

Feature responsável pelas preferências locais do aplicativo.

## Escopo atual
- Preferência de tema: `system`, `light` ou `dark`.
- Ordenação dos containers de categoria: padrão/manual, mais recentes, mais antigas, nome A–Z e Z–A. A ordenação por data considera a referência mais recente ou mais antiga dentro de cada categoria; os thumbnails do preview mantêm a ordem original.
- Ordenação das referências dentro da categoria expandida: mais recentes, mais antigas, título A–Z e Z–A.
- Persistência por SharedPreferences, encapsulada em `SettingsRepository`.
- Presentation observa e altera o tema por `SettingsViewModel` e UseCases.

As preferências permanecem locais ao dispositivo em SharedPreferences. As escolhas de ordenação e compressão são observadas pelo SettingsViewModel e aplicadas pela biblioteca. Novas configurações devem seguir o mesmo limite Domain/Data/Presentation.
