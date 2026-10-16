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
- Continuar la lectura con progreso por capítulo y posición guardados.
- Navegar con el índice, buscar palabras o frases y crear marcadores.
- Seleccionar texto para consultar el diccionario inglés-español inicial o una respuesta en caché, sin conexión.
- Guardar vocabulario, añadir una traducción propia y escuchar el texto con la voz inglesa de Android.
- Guardar subrayados y notas, y volver a ellos desde las anotaciones del libro.
- Repasar palabras con flashcards e intervalos de 1, 3, 6, 12… días. Una respuesta fallida vuelve en 10 minutos; desde 7 días se marca como aprendida.
- Consultar minutos de lectura, días con actividad y palabras aprendidas.
- Ajustar tipografía, tamaño, interlineado y tema claro, oscuro o del sistema. Navegación con barra en teléfono y rail en pantallas amplias.

La apariencia inicial es oscura, con superficies grafito y acentos azules. Si ya elegiste Claro o Sistema en Ajustes, se respeta esa elección. El lector, los menús y la selección de texto comparten los colores del tema.

Al abrir se incluye **A Little More Slowly**, una lectura de muestra original de Lectio, con tres capítulos en inglés. Las portadas tipográficas de reserva representan el título; no son portadas editoriales originales.

## IA opcional

La app no incluye una clave ni realiza consultas automáticas de pago. En **Ajustes** introduce una URL base HTTPS compatible con `/chat/completions`, el nombre de un modelo disponible en tu proveedor y tu clave. El ejemplo configurado es `https://api.openai.com/v1` y `gpt-4.1-mini`; puedes cambiarlos.

Seleccionar texto consulta primero los datos locales. **Traducir con IA**, **Explicar**, **Inglés sencillo** y **Gramática** solicitan una respuesta al proveedor configurado. Se envían el fragmento y hasta 3.000 caracteres de contexto; el proveedor puede cobrar por ello. Las respuestas se identifican como IA y se guardan en caché por texto, contexto, acción, modelo y proveedor. La clave se cifra con AES-GCM y una clave de Android Keystore; no se guarda en código ni se copia con backups del sistema.

La pronunciación utiliza Android TextToSpeech. Una voz descargada permite uso offline; la disponibilidad y el uso de red dependen del motor de voz del dispositivo.

## Alcance de esta versión

Es un **MVP**, no un lector EPUB certificado. Admite contenido EPUB 2/3 reflowable con capítulos XHTML, estructura de lectura, índice nav/NCX, formato semántico, imágenes raster locales y enlaces entre capítulos. El contenido activo se elimina y los recursos remotos se bloquean.

- No soporta DRM, maquetación fija, audio, vídeo, JavaScript editorial, SVG ni formatos PDF/MOBI/CBZ.
- Reinterpreta los estilos del libro para aplicar las preferencias de lectura; no conserva toda la maquetación editorial.
- Los enlaces del índice a secciones se agrupan por capítulo; no se reproduce cada nivel de navegación del EPUB.
- El diccionario es una selección inicial redactada para el proyecto, no un diccionario general completo. El nivel CEFR es orientativo. Las palabras faltantes permiten traducción manual o consulta opcional a IA.
- Los subrayados se restauran buscando la primera coincidencia exacta del texto en su capítulo. Frases idénticas repetidas y selecciones que cruzan párrafos requieren anclajes más precisos en una versión posterior.
- El progreso es aproximado por capítulo y desplazamiento, no por número de páginas editoriales.
- La búsqueda muestra hasta cinco coincidencias por capítulo, con un máximo de 100 resultados.
- El tiempo se registra en intervalos de 10 segundos con el lector en primer plano; no detecta si estás mirando la pantalla.
- Los libros y los datos se guardan solo en este dispositivo. No hay sincronización ni exportación; desinstalar elimina los datos.
- Las funciones de IA necesitan conexión y un proveedor que respete el formato solicitado. No se verifican con solicitudes reales que consuman créditos.

Para una siguiente fase con compatibilidad editorial completa, conviene migrar el motor a Readium y usar localizadores CFI/Readium para progreso y anotaciones.

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
    AiClient.kt            API opcional por HTTPS
    DemoBook.kt            EPUB original de muestra
  ui/                      Pantallas, tema y lector aislado
app/src/main/assets/dictionary.json
app/src/test/               Pruebas del parser, datos y flujos de interfaz
app/src/androidTest/        Regresión nativa del lector con Chromium real
```

Se usa SQLiteOpenHelper y preferencias locales para mantener el MVP pequeño; no se incorporan Room, DataStore, WorkManager ni Readium todavía. Las importaciones se ejecutan fuera del hilo de interfaz. Cada EPUB está limitado a 100 MB y cada recurso descomprimido a 16 MB, sin extraer rutas del archivo al sistema de archivos.

## Validación

Resultados, capturas y pasos pendientes en dispositivo: [VALIDATION.md](VALIDATION.md).

Las 14 pruebas JVM cubren el parser, recursos WebView, persistencia y navegación Compose. Dos pruebas instrumentadas ejecutadas en Android 15 comprueban capítulos reales en Chromium, selección mediante DOM y consulta nativa, reapertura y temas. Las capturas actuales del emulador y los límites de esta cobertura están en VALIDATION.md.

Quedan por validar en hardware: selección mediante gestos dentro del WebView, motor TTS, selector de archivos de cada fabricante, restauración tras terminar el proceso y rendimiento con libros grandes. El código está disponible en GitHub; la app aún no se ha publicado en una tienda.

Referencias técnicas: [Compose Compiler](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler), [AGP 8.13](https://developer.android.com/build/releases/agp-8-13-0-release-notes), [Readium Kotlin](https://github.com/readium/kotlin-toolkit).
