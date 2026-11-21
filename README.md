# Asistencia Colle (Android)

App nativa (Kotlin + Jetpack Compose) del control de asistencia del UD Collerense.
Funciona sin conexión y se sincroniza con el Excel `UD_Collerense_Asistencia_*.xlsm`
mediante OneDrive / Google Drive.

## Sincronización
* **PC → móvil**: en la app, *Sincronizar › Importar Excel (.xlsm)*. Lee jugadores (con fotos),
  sesiones, horarios, motivos y asistencia directamente del .xlsm (lector propio, solo lectura).
* **Móvil → PC**: *Sincronizar › Exportar cambios* crea `cambios_<equipo>_<fecha>.colle` (JSON UTF-8).
  En el Excel: botón **📱 IMPORTAR DEL MÓVIL** (módulo `modSync`), con fusión a tres bandas
  (base / móvil / PC) y resolución de conflictos.

## Compilar
GitHub Actions (`.github/workflows/build.yml`) compila el APK firmado en cada push.
Descárgalo en *Actions › último run › Artifacts › Collerense_Asistencia_APK*.
La firma (`app/collerense.jks`) es fija para que las actualizaciones se instalen encima sin perder datos.

© Oliver Gutiérrez · UD Collerense
