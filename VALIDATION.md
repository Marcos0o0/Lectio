# Validación de Lectio 0.2.0

Fecha: 16 de octubre de 2026. Windows, JDK 21 de Android Studio, SDK 36. Paquete `app.lectio.reader`, versión 0.2.0 (código 3), mínimo API 26 y objetivo API 36.

## Comprobaciones

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --max-workers=1 --console=plain
```

- **17 pruebas JVM, cero fallos:** ocho de EPUB/lógica, dos de almacenamiento, tres del interceptor WebView, tres de Gemini y una del flujo Compose con Robolectric API 35.
- **Dos pruebas instrumentadas, cero fallos**, en Android 15/API 35 x86_64 con Chromium real (`com.android.webview` 124.0.6367.219).
- Lint sin errores: 12 advertencias y tres sugerencias, principalmente dependencias/KTX y estados primitivos de Compose. El observador táctil conserva la gestión de clics y accesibilidad de WebView; no sustituye su comportamiento nativo.
- APK: `app/build/outputs/apk/debug/app-debug.apk`. Firma verificada con `apksigner`, esquema v2. El SHA-256 final figura al final de este informe.

## Selección y traducción

La prueba nativa localiza una palabra mediante su geometría en Chromium y envía eventos táctiles de Android, sin crear la selección con JavaScript. Una pulsación prolongada sobre «precious» abre la ficha inferior; un toque en «minutes» abre otra consulta. La prueba espera a que Android devuelva el foco al lector antes del siguiente gesto.

Se comprobó la descarga inicial real del idioma y la traducción con el modelo Google ML Kit en Android. La ficha muestra la palabra traducida y la oración de contexto traducida. No usa un listado de significados escrito a mano. Una selección posterior mediante rango DOM verifica el contenido de varias palabras y su traducción con el mismo modelo; **el arrastre físico de los tiradores de selección de una frase sigue pendiente en hardware**.

El flujo también comprueba capítulos renderizados, cambio de capítulo, marcador, vuelta a biblioteca y reapertura, y fondos claro/oscuro calculados por Chromium. La segunda prueba abre la configuración de Gemini sin introducir una clave real ni hacer peticiones de pago.

```powershell
adb shell am instrument -w -r -e class app.lectio.reader.ReaderDeviceTest app.lectio.reader.test/androidx.test.runner.AndroidJUnitRunner
```

El resultado debe indicar `OK (2 tests)`. El APK de pruebas está en `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. La descarga del idioma necesita red la primera vez; las consultas posteriores pueden resolverse en el dispositivo o con la caché.

## Gemini y almacenamiento

Las pruebas de Gemini verifican separación entre instrucciones y datos del libro, límite de contexto de 1.200 caracteres, límite de salida de 800 tokens, lectura del JSON con traducción de contexto y rechazo de respuestas bloqueadas/truncadas. Las claves de caché distinguen proveedor, acción y contexto. La persistencia comprueba que la traducción de contexto sobreviva a reabrir SQLite.

No se usaron claves personales ni se hicieron consultas reales a Gemini. Su respuesta real, cuotas, saldo y permisos deben comprobarse con la cuenta del usuario. No se entrega una clave común en el APK ni un backend para compartir una cuenta de pago con terceros.

## Capturas nativas

Capturas del emulador Android 15: teléfono 720 × 1600/densidad 280, ventana amplia 1280 × 1800/densidad 240, horizontal 1600 × 720 y texto al 130 %. Las clases de ventana no son tablets físicas.

- [Biblioteca](.impeccable/review/android15/phone-library-dark-android15.png) y [lector oscuro](.impeccable/review/android15/phone-reader-dark-android15.png)
- [Descarga inicial](.impeccable/review/android15/phone-download-android15.png), [palabra y oración](.impeccable/review/android15/phone-lookup-dark-android15.png), [frase](.impeccable/review/android15/phone-phrase-android15.png)
- [Ajustes oscuros](.impeccable/review/android15/phone-settings-dark-android15.png), [claros](.impeccable/review/android15/phone-settings-light-android15.png), [conexión de Gemini](.impeccable/review/android15/phone-gemini-setup-android15.png)
- [Lector claro](.impeccable/review/android15/phone-reader-light-android15.png), [vocabulario](.impeccable/review/android15/phone-vocabulary-dark-android15.png), [actividad](.impeccable/review/android15/phone-activity-dark-android15.png)
- [Texto al 130 %](.impeccable/review/android15/phone-settings-font130-android15.png)
- [Biblioteca amplia](.impeccable/review/android15/tablet-library-dark-android15.png), [lector amplio](.impeccable/review/android15/tablet-reader-dark-android15.png), [lector horizontal](.impeccable/review/android15/phone-landscape-reader-dark-android15.png)

La paleta de la versión 0.1.1 se conserva: 14 pares de texto por tema con contraste calculado ≥4,70:1 y contornos principales ≥3,98:1. [Mediciones](.impeccable/review/android15/contrast-pairs.txt). No equivale a una certificación completa de accesibilidad. Los distintivos de Google Translate son recursos oficiales sin modificar; procedencia en THIRD_PARTY_NOTICES.md.

## Regresiones y límites

Se conserva la corrección de 0.1.1: las solicitudes internas `data:` y `about:blank` de WebView no pasan por la detección de MIME con un nombre de archivo nulo. Las rutas externas y recursos activos de los libros siguen bloqueados. El lector conserva el margen inferior para controles y acción de importación en ventanas amplias.

El Redmi Note 13 5G del usuario no estuvo conectado. Quedan por verificar allí los gestos y el menú del fabricante, los tiradores de selección, TTS y voces, selector de archivos, restauración tras terminar el proceso, TalkBack y libros grandes. La calidad de traducción local es la de un modelo automático, no la de un diccionario editorial; Gemini puede ampliar el contexto de forma opcional. Límites EPUB en README.md.

## SHA-256 del APK validado

90F171473320B71D9BC6B11A887AF4D546506A3A9C96EA7DD12D03716ED471CB
