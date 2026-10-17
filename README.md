# Lectio

Un lector EPUB para Android pensado para leer en inglés y aprender vocabulario sin abandonar el libro. Implementación inicial en Kotlin y Jetpack Compose.

Lectio es el nombre confirmado del proyecto. Del latín *lēctiō*: lectura.

Repositorio: [Marcos0o0/Lectio](https://github.com/Marcos0o0/Lectio).

## Abrir y ejecutar

1. Abre **esta carpeta** en Android Studio.
2. Usa el JDK integrado de Android Studio (Java 21) y Android SDK 36. No uses el Java 25 del sistema con esta versión de Gradle.
3. Espera la sincronización de Gradle y ejecuta `app` en un dispositivo Android 8 o posterior.

Desde PowerShell, con el SDK instalado:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
```

Si hace falta, configura `sdk.dir` en `local.properties` (Android Studio lo crea automáticamente). El APK de desarrollo queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Qué puedes hacer

- Importar EPUB sin DRM mediante el selector de archivos de Android. El libro se copia al almacenamiento privado: no depende del archivo original.
- Leer página a página con flechas o deslizamientos horizontales. Al terminar un capítulo, el siguiente avance abre el próximo; retroceder desde su inicio vuelve a la última página anterior. El índice permite saltar capítulos directamente.
- Continuar donde se dejó la lectura, con posición de texto guardada y restaurada aunque cambien letra, márgenes o tamaño de pantalla.
- Navegar con el índice, buscar palabras o frases y crear marcadores.
- Tocar una palabra o mantener pulsado para seleccionar una frase y abrir su traducción en una ficha inferior. La ficha separa Significado, Contexto y Notas; incluye definiciones y la oración traducida cuando corresponde.
- Guardar vocabulario, añadir una traducción propia y escuchar el texto con la voz inglesa de Android.
- Guardar subrayados y notas, y volver a ellos desde las anotaciones del libro.
- Repasar palabras con flashcards e intervalos de 1, 3, 6, 12… días. Una respuesta fallida vuelve en 10 minutos; desde 7 días se marca como aprendida.
- Consultar minutos de lectura, días con actividad y palabras aprendidas.
- Personalizar letra, interlineado, fondo del libro, márgenes, separación de párrafos y alineación con vista previa. Activar o desactivar la consulta al tocar, la ayuda de gestos y la pantalla encendida. Navegación con barra en teléfono y rail en pantallas amplias.

La apariencia inicial es oscura, con superficies grafito y acentos azules. Si ya elegiste Claro o Sistema en Ajustes, se respeta esa elección. En Apariencia de lectura → Página puedes elegir **Papel**, un fondo crema amarillo suave, además de Claro, Oscuro o Como la app. El fondo del libro y las barras del lector siguen esta elección; la biblioteca conserva su tema.

Al abrir se incluye **A Little More Slowly**, una lectura de muestra original de Lectio, con tres capítulos en inglés. Las portadas tipográficas de reserva representan el título; no son portadas editoriales originales.

## Traducción y explicaciones

La traducción usa **Google Translate mediante Google ML Kit en el dispositivo**, no una lista de significados escrita en el código. La primera consulta ofrece descargar español (alrededor de 30 MB) por Wi-Fi o, si lo eliges, datos móviles. Después traduce palabras y frases sin conexión, sin clave y sin cobros por consulta. [Documentación de traducción](https://developers.google.com/ml-kit/language/translation), [Google Translate](https://cloud.google.com/translate).

**Significados sin clave:** el diccionario consulta únicamente la palabra en [Wiktionary](https://en.wiktionary.org/wiki/Wiktionary:Main_Page), muestra hasta cuatro acepciones y ejemplos, y conserva la entrada y su licencia CC BY-SA 4.0. Los significados se traducen al español con el idioma descargado; el original en inglés sigue disponible. Las consultas se guardan para usarlas sin conexión. Las palabras nuevas requieren internet y algunas no tienen entrada; se puede desactivar la consulta en Ajustes → Diccionario y traducción. Una definición general no determina por sí sola cuál es el sentido del libro.

**Gemini es opcional** para significado contextual, gramática e inglés sencillo. En Ajustes → Gemini → Conectar Gemini se pega una clave personal de Google AI Studio una sola vez. No se pide URL ni modelo en el flujo normal. Al abrir la conexión desde el lector, Volver al libro conserva el capítulo y la posición. El modelo inicial es `gemini-3.5-flash-lite`; las opciones avanzadas permiten cambiarlo o usar un proveedor compatible con chat completions. No hay una clave del desarrollador dentro del APK.

Solo al pulsar una explicación se envían la selección y hasta 1.200 caracteres de contexto a Gemini. Las respuestas se limitan a 800 tokens de salida y se guardan en caché por texto, contexto, acción, modelo y proveedor; no hay reintentos automáticos de pago. El proveedor puede cobrar. La clave personal se cifra con AES-GCM y Android Keystore, fuera del código y de backups. Para ofrecer IA a terceros con una cuenta común se necesitaría un backend con autenticación y límites; no se debe incrustar esa clave en el APK.

Referencia de coste consultada el 16 de octubre de 2026: Gemini 3.5 Flash-Lite, USD 0,30 por millón de tokens de entrada y USD 2,50 de salida. Un ejemplo de 1.000 consultas de 600 tokens de entrada y 300 de salida cuesta aproximadamente USD 0,93, antes de impuestos y tokens adicionales de razonamiento. No es una garantía del número de consultas que cubra un saldo en CLP. [Precios](https://ai.google.dev/gemini-api/docs/pricing).

La pronunciación utiliza Android TextToSpeech. Una voz descargada permite uso offline; la disponibilidad y el uso de red dependen del motor de voz del dispositivo.

## Alcance de esta versión

Es un **MVP**, no un lector EPUB certificado. Admite contenido EPUB 2/3 reflowable con capítulos XHTML, estructura de lectura, índice nav/NCX, formato semántico, imágenes raster locales y enlaces entre capítulos. El contenido activo se elimina y los recursos remotos se bloquean.

- No soporta DRM, maquetación fija, audio, vídeo, JavaScript editorial, SVG ni formatos PDF/MOBI/CBZ.
- Reinterpreta los estilos del libro para aplicar las preferencias de lectura; no conserva toda la maquetación editorial.
- Los enlaces del índice a secciones se agrupan por capítulo; no se reproduce cada nivel de navegación del EPUB.
- La traducción local ofrece traducciones automáticas, no definiciones de un diccionario editorial ni explicaciones contextuales completas. Las definiciones de Wiktionary complementan esa traducción. Gemini puede ampliar el significado contextual cuando se configura; sus respuestas pueden contener errores.
- Los subrayados se restauran buscando la primera coincidencia exacta del texto en su capítulo. Frases idénticas repetidas y selecciones que cruzan párrafos requieren anclajes más precisos en una versión posterior.
- Las páginas se calculan dentro de cada capítulo y cambian con la tipografía y la ventana; no son números de una edición impresa. El porcentaje del libro sigue siendo aproximado por capítulo y posición de texto. Las posiciones antiguas se recuperan por su fracción guardada; las nuevas incorporan un anclaje de texto, también en marcadores y anotaciones.
- La búsqueda muestra hasta cinco coincidencias por capítulo, con un máximo de 100 resultados.
- El tiempo se registra en intervalos de 10 segundos con el lector en primer plano; no detecta si estás mirando la pantalla.
- Los libros y los datos se guardan solo en este dispositivo. No hay sincronización ni exportación; desinstalar elimina los datos.
- Las funciones de IA necesitan conexión y un proveedor que respete el formato solicitado. No se verifican con solicitudes reales que consuman créditos.

Para una siguiente fase con compatibilidad editorial completa, conviene migrar el motor a Readium y usar localizadores CFI/Readium para anotaciones y posiciones portables entre motores.

## Estructura

```text
app/src/main/java/app/lectio/reader/
  MainActivity.kt          Entrada, Compose y pronunciación
  ReaderViewModel.kt       Estado, acciones y corrutinas
  data/
    Epub.kt                Paquete EPUB, recursos y saneado
    ReaderRepository.kt    Importación y resolución local/IA
    LibraryDb.kt           SQLite: libros, palabras, notas, caché y sesiones
    SettingsStore.kt       Preferencias y clave cifrada
    DictionaryClient.kt    Definiciones, ejemplos y créditos de Wiktionary
    LocalTranslation.kt    Modelos inglés-español de Google ML Kit
    AiClient.kt            Proveedores de explicaciones por HTTPS
    GeminiClient.kt        Conexión personal con Gemini
    DemoBook.kt            EPUB original de muestra
  ui/                      Pantallas, tema y lector aislado
app/src/main/assets/
  reader-pagination.js     Páginas, anclajes de texto y búsqueda local
app/src/test/               Pruebas del parser, datos y flujos de interfaz
app/src/androidTest/        Regresión nativa del lector con Chromium real
```

Se usa SQLiteOpenHelper y preferencias locales para mantener el MVP pequeño; no se incorporan Room, DataStore, WorkManager ni Readium todavía. Las importaciones se ejecutan fuera del hilo de interfaz. Cada EPUB está limitado a 100 MB y cada recurso descomprimido a 16 MB, sin extraer rutas del archivo al sistema de archivos.

## Validación

Resultados, capturas y pasos pendientes en dispositivo: [VALIDATION.md](VALIDATION.md).

Las 22 pruebas JVM cubren EPUB, recursos WebView, Gemini, persistencia, migración de ubicaciones y navegación Compose. La suite de cuatro pruebas instrumentadas en Android 15 añade páginas de un capítulo largo, flechas y deslizamiento reales, cambios de tipografía, giro, reapertura, búsqueda y cruce de capítulos; conserva las comprobaciones de selección, traducción local, contexto, Gemini y personalización aplicada en Chromium. Las frases se seleccionan mediante rango DOM, sin validar el arrastre físico de sus tiradores. La regresión JVM cubre cambios consecutivos de preferencias sin sobrescribir las elecciones anteriores. Los resultados y capturas actuales están en VALIDATION.md.

Quedan por validar en hardware: selección mediante gestos dentro del WebView, motor TTS, selector de archivos de cada fabricante, restauración tras terminar el proceso y rendimiento con libros grandes. El código está disponible en GitHub; la app aún no se ha publicado en una tienda.

Referencias técnicas: [Compose Compiler](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler), [AGP 8.13](https://developer.android.com/build/releases/agp-8-13-0-release-notes), [Readium Kotlin](https://github.com/readium/kotlin-toolkit).
