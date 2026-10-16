# Validación de Lectio 0.3.0

16 de octubre de 2026. Windows, JDK 21 de Android Studio y SDK 36. Paquete `app.lectio.reader`, versión 0.3.0 (código 4), mínimo API 26, objetivo API 36.

## Compilación y pruebas

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug --offline --max-workers=1 --console=plain '-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8'
```

- Compilación correcta del APK y del APK de pruebas.
- **21 pruebas JVM, cero fallos:** ocho de EPUB, tres de recursos WebView, tres de Gemini, tres de diccionario, tres de almacenamiento/preferencias y una del flujo Compose.
- La regresión del flujo Compose aplica cambios consecutivos de márgenes, párrafos y pantalla encendida en el mismo turno, comprueba que se acumulan y espera su persistencia. El ViewModel aplica cada modificación sobre el estado más reciente; una cola guarda las preferencias en orden. Actualizar biblioteca o vocabulario no restaura una copia anterior de ajustes.
- Lint: **cero errores**, 12 advertencias y tres sugerencias, principalmente dependencias/KTX y estados primitivos.
- Firma del APK verificada con `apksigner`, esquema v2; `aapt` confirma versión, código y niveles de API. El hash final figura al final del informe.

## Pruebas en Android

**Tres pruebas instrumentadas aprobadas**, `OK (3 tests)`, 32,782 segundos, en el emulador Android 15/API 35 x86_64 con Chromium real. No se utilizó un teléfono físico.

```powershell
adb shell am instrument -w -r -e class app.lectio.reader.ReaderDeviceTest app.lectio.reader.test/androidx.test.runner.AndroidJUnitRunner
```

La primera comprueba opciones de página y controles, recreación de la actividad, persistencia, CSS calculado en Chromium (fondo, margen, justificación y separación de párrafos), pantalla encendida, ayuda oculta y restablecimiento desde el panel completamente abierto del lector.

La segunda usa geometría del DOM para ubicar una palabra y eventos táctiles reales de Android para mantener pulsado «precious» y tocar «minutes». Verifica traducción con el modelo Google ML Kit, una entrada real de Wiktionary, pestañas de contexto y notas, apertura de Gemini y regreso al mismo libro, capítulos, marcador y reapertura. La selección de varias palabras se crea con un rango DOM para comprobar su contenido y traducción: **no valida el arrastre físico de los tiradores de selección**.

La tercera comprueba los temas claro/oscuro en la app y el lector, y abre la configuración de Gemini sin introducir una clave ni efectuar solicitudes de pago. Cada prueba restablece sus preferencias iniciales para no depender de las elecciones de otra prueba.

## Significados y atribución

Se consultó la API REST pública de Wiktionary con una palabra del libro de muestra y se comprobó el resultado en Android. Solo se envía la palabra normalizada; el libro y la oración no se envían al diccionario. El cliente limita la respuesta a 512 KB, convierte HTML a texto, muestra hasta cuatro acepciones y conserva entrada y licencia CC BY-SA 4.0. Se muestran dos sentidos inicialmente; los demás y el original inglés se despliegan a petición.

Las definiciones españolas son traducciones automáticas locales de las originales inglesas, no contenido editorial revisado ni significados escritos a mano. Se conservan los créditos en la caché y en las palabras guardadas. Las pruebas cubren normalización, rutas no válidas, lectura del formato, límites, enlaces permitidos y persistencia de la entrada y su traducción. Las consultas nuevas requieren internet; las entradas guardadas pueden reutilizarse sin otra consulta al servicio.

Los fallos del diccionario no bloquean la traducción local. La descarga inicial del idioma ya se verificó en 0.2.0; esta ejecución reutilizó el modelo descargado. No se usaron claves personales ni llamadas reales a Gemini. Su facturación y respuesta real siguen pendientes con una cuenta del usuario.

## Evidencia visual

Capturas nuevas del emulador en [.impeccable/review/v030](.impeccable/review/v030). Las de `android15` corresponden a la versión anterior.

- [Significado](.impeccable/review/v030/phone-lookup-dark-android15.png), [contexto](.impeccable/review/v030/phone-context-dark-android15.png) y [notas](.impeccable/review/v030/phone-notes-dark-android15.png).
- [Ajustes oscuros](.impeccable/review/v030/phone-settings-dark-android15.png), [claros](.impeccable/review/v030/phone-settings-light-android15.png) y [diccionario](.impeccable/review/v030/phone-dictionary-settings-android15.png).
- [Texto](.impeccable/review/v030/phone-reading-text-android15.png), [página](.impeccable/review/v030/phone-reading-page-android15.png), [controles](.impeccable/review/v030/phone-reading-controls-android15.png) y [lector personalizado](.impeccable/review/v030/phone-reader-custom-android15.png).
- [Conexión desde el libro](.impeccable/review/v030/phone-gemini-from-reader-android15.png) y [formulario](.impeccable/review/v030/phone-gemini-setup-android15.png).

Las comprobaciones de tamaño y escala usan ventanas del emulador, no tablets físicas: teléfono 720 × 1600/densidad 280; amplia 1280 × 1800/densidad 240; amplia horizontal 1800 × 1280/densidad 240; teléfono horizontal 1600 × 720/densidad 280; texto al 130 %. En estas capturas se midió la geometría del texto en Chromium y se utilizó una pulsación prolongada nativa de Android; el contenido no se sustituyó por un mockup. En altura compacta, el cuerpo de la ficha se desplaza entre su cabecera y el guardado fijo.

- [Texto al 130 %](.impeccable/review/v030/phone-font130-lookup-android15.png) y [su contexto](.impeccable/review/v030/phone-font130-context-android15.png).
- [Ventana amplia](.impeccable/review/v030/tablet-lookup-android15.png) y [amplia horizontal](.impeccable/review/v030/tablet-landscape-lookup-android15.png).
- [Teléfono horizontal](.impeccable/review/v030/phone-landscape-lookup-android15.png), [ficha desplazada](.impeccable/review/v030/phone-landscape-lookup-scrolled-android15.png) y [contexto](.impeccable/review/v030/phone-landscape-context-android15.png).
- [Panel de lectura completamente abierto](.impeccable/review/v030/phone-reader-appearance-android15.png). La paleta de 0.1.1 se mantiene: contraste de 14 pares de texto por tema ≥4,70:1 y contornos principales ≥3,98:1. [Mediciones](.impeccable/review/android15/contrast-pairs.txt). No es una certificación completa de accesibilidad.

## Límites

El Redmi Note 13 5G del usuario no estuvo conectado. Quedan por verificar allí los tiradores de selección y menú del fabricante, TTS, selector de archivos, restauración tras terminar el proceso, TalkBack y rendimiento con libros grandes. La corrección de apertura de libros de 0.1.1 se conserva. Límites EPUB y almacenamiento en README.md.

## SHA-256 del APK validado

82C275DC0B9F1B847A2B6A8D0C4FA75AA4221964E0EA9374BA6DBDC20072CF4A
