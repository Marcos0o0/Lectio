# Validación de Lectio 0.1.1

Fecha: 16 de octubre de 2026. Windows, JDK 21 integrado de Android Studio y Android SDK 36. Paquete `app.lectio.reader`, versión 0.1.1 (código 2), mínimo API 26 y objetivo API 36.

## Compilación y pruebas

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --max-workers=1 --console=plain
```

- APK debug: `app/build/outputs/apk/debug/app-debug.apk`, firma verificada con `apksigner` (APK Signature Scheme v2).
- **14 pruebas JVM, cero fallos:** ocho de EPUB/lógica, dos de almacenamiento, tres del interceptor WebView y una del flujo Compose con Robolectric API 35.
- **Lint: cero errores**, 12 advertencias y tres sugerencias. Son principalmente actualizaciones de dependencias, KTX y estados primitivos de Compose.
- **Dos pruebas instrumentadas, cero fallos**, ejecutadas sobre Android 15/API 35 x86_64 con Chromium real (`com.android.webview` 124.0.6367.219).

El SHA-256 del APK final figura al final de este informe.

## Regresión de apertura de libros

Se reprodujo el cierre de la versión 0.1.0 al abrir la muestra en Android 15: una solicitud interna de WebView sin nombre de archivo llegaba a `URLConnection.guessContentTypeFromName(null)` y provocaba una `NullPointerException` en `DefaultFileNameMap`.

El interceptor ahora permite que WebView cargue sus documentos internos `data:` y `about:blank`; valida las rutas del dominio local antes de consultar su tipo y mantiene bloqueadas las solicitudes externas y los recursos activos de los libros. Las pruebas de regresión cubren URLs sin ruta, recursos pasivos, rutas no válidas y la fuente local.

Las pruebas nativas verifican contenido de capítulos renderizado, cambio de capítulo, marcador, retorno a biblioteca y reapertura. Una selección mediante un rango DOM real dispara `selectionchange`, cruza el puente JavaScript y muestra la traducción española en la interfaz nativa. También se comprueban los fondos claro y oscuro mediante estilos calculados de Chromium y la persistencia de la elección de tema. Las acciones nativas usan accesibilidad de Android; esto no valida el gesto físico de selección.

Con un emulador o teléfono conectado y los dos APK instalados:

```powershell
adb shell am instrument -w -r -e class app.lectio.reader.ReaderDeviceTest app.lectio.reader.test/androidx.test.runner.AndroidJUnitRunner
```

El resultado debe indicar `OK (2 tests)`. El APK de pruebas está en `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.

## Revisión visual nativa

Se revisaron capturas del emulador Android 15 en teléfono (720 × 1600, densidad 280), ventana amplia (1280 × 1800, densidad 240), orientación horizontal (1600 × 720) y escala de texto 130 %. Las dimensiones amplias simulan una clase de ventana; no representan pruebas en una tablet física.

- [Biblioteca oscura](.impeccable/review/android15/phone-library-dark-android15.png)
- [Lector oscuro](.impeccable/review/android15/phone-reader-dark-android15.png)
- [Consulta de vocabulario](.impeccable/review/android15/phone-lookup-dark-android15.png)
- [Ajustes oscuros](.impeccable/review/android15/phone-settings-dark-android15.png) y [claros](.impeccable/review/android15/phone-settings-light-android15.png)
- [Lector claro](.impeccable/review/android15/phone-reader-light-android15.png)
- [Vocabulario](.impeccable/review/android15/phone-vocabulary-dark-android15.png) y [actividad](.impeccable/review/android15/phone-activity-dark-android15.png)
- [Texto al 130 %](.impeccable/review/android15/phone-settings-font130-android15.png)
- [Biblioteca amplia](.impeccable/review/android15/tablet-library-dark-android15.png) y [lector amplio](.impeccable/review/android15/tablet-reader-dark-android15.png)
- [Lector horizontal](.impeccable/review/android15/phone-landscape-reader-dark-android15.png)

El contraste calculado de 14 pares de texto por tema es al menos 4,70:1; el de los contornos principales sobre la superficie es al menos 3,98:1. [Mediciones](.impeccable/review/android15/contrast-pairs.txt). Estas mediciones de tokens no certifican toda la accesibilidad de la app.

Las capturas JVM anteriores permanecen como evidencia histórica; no muestran la paleta actual ni prueban Chromium.

## Límites de la verificación

El Redmi Note 13 5G del usuario no estuvo conectado. Falta comprobar en ese hardware la selección mediante gestos, motor TTS y voces instaladas, selector de archivos del fabricante, restauración tras terminar el proceso, lector de pantalla y rendimiento con libros grandes. No se hicieron consultas de IA con claves de pago.

El parser y almacenamiento conservan sus pruebas de rutas, escapes, saneado, límites de recursos, metadatos UTF-8/UTF-16, orden de lectura, progreso, anotaciones, vocabulario y caché. La compatibilidad EPUB del MVP y las restricciones del diccionario se detallan en [README.md](README.md).

## SHA-256 del APK validado

D4905E4988A68F3CB2A5925F821DB271A09CE9FE80B358C2DEBCC2C03550EF98
