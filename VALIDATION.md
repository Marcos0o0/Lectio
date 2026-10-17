# Validación de Lectio 0.4.0

17 de octubre de 2026. Windows, JDK 21 de Android Studio y SDK 36. Paquete `app.lectio.reader`, versión 0.4.0 (código 5), mínimo API 26, objetivo API 36.

## Compilación y almacenamiento

Compilación completa: `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug --offline --max-workers=1`, con JDK 21 y heap de 1 GB. Terminó correctamente en 5 min 28 s. Los ajustes posteriores afectan únicamente al test instrumentado y documentación; su compilación y lint también terminaron correctamente.

- **22 pruebas JVM, cero fallos:** ocho de EPUB, tres de recursos WebView, tres de Gemini, tres de diccionario, cuatro de almacenamiento y una del flujo Compose.
- Reapertura de libros y anotaciones con posición de texto, y migración de una base versión 1 a versión 2 conservando marcadores y progreso anteriores. Las posiciones antiguas usan el valor compatible -1 y su fracción guardada.
- Papel persiste junto al resto de personalización. Se conserva la regresión de cambios consecutivos de preferencias sin sobrescribir elecciones anteriores.
- Lint: **cero errores**, ocho advertencias y tres sugerencias.
- Firma v2 verificada con apksigner; aapt confirma versión, código y niveles de API.

## Android y Chromium

**Cuatro pruebas instrumentadas aprobadas**, `OK (4 tests)`, 75,158 segundos, en Android 15/API 35 x86_64 con Chromium real.

La prueba nueva importa un EPUB de dos capítulos: el primero tiene 90 párrafos y debe generar más de 15 páginas. Comprueba avance y retroceso mediante controles nativos, un deslizamiento horizontal real sin cambiar de capítulo, tamaño de letra 28 y márgenes amplios conservando el anclaje, tema Papel, giro a horizontal y regreso, reapertura, búsqueda y enlaces a fragmentos, llegada al siguiente capítulo, avance desactivado al final del libro y retorno a la última página del capítulo anterior.

Las otras tres pruebas conservan la cobertura de personalización persistida y CSS calculado en Chromium, temas claro/oscuro, pulsación prolongada y toque reales de palabras, traducción con el modelo local, ficha de significado/contexto/notas, apertura de Gemini y regreso al mismo libro, índice, marcador y reapertura. El modelo de traducción descargado y la caché de Wiktionary se reutilizan. No se introducen claves ni se consumen solicitudes de Gemini.

La selección de una frase se crea mediante un rango DOM: **no valida el arrastre físico de los tiradores de selección**. La paginación y los cambios de ventana se prueban en un emulador, no en el Redmi físico del usuario.

## Comportamiento de las páginas

El motor usa columnas con la altura útil del WebView y un contenedor que muestra una página cada vez. Las flechas, el teclado y el deslizamiento horizontal llaman al mismo avance; solo el límite del capítulo solicita otro capítulo. Se espera a la fuente local y se recalcula al variar la ventana o cargarse una imagen.

Los anclajes cuentan posiciones de texto del capítulo saneado, independientes del tamaño de letra. La búsqueda marca el resultado y abre su página, y los enlaces internos resuelven el fragmento. Las páginas compuestas solo por imágenes conservan una fracción como alternativa. No son localizadores CFI ni posiciones portables entre motores. Las anotaciones antiguas conservan su posición aproximada.

El pie identifica la página y total **del capítulo**. Cambian con la tipografía, los márgenes y la ventana; no equivalen a una edición impresa. El porcentaje del libro sigue siendo aproximado por capítulo y posición.

## Evidencia visual y Papel

Capturas del emulador en [.impeccable/review/v040](.impeccable/review/v040), con teléfono 720 × 1600/densidad 280, texto al 130 %, ventana amplia 1280 × 1800/densidad 240, amplia horizontal 1800 × 1280/densidad 240 y teléfono horizontal 1600 × 720/densidad 280. Se capturan el lector y los ajustes nativos; no son mockups ni capturas de un navegador de escritorio.

- [Papel](.impeccable/review/v040/phone-paper-reader-android15.png) y [página siguiente del mismo capítulo](.impeccable/review/v040/phone-paper-next-page-android15.png).
- [Opciones de fondo](.impeccable/review/v040/phone-paper-options-android15.png) y [texto al 130 %](.impeccable/review/v040/phone-font130-paper-reader-android15.png).
- [Ventana amplia](.impeccable/review/v040/tablet-paper-reader-android15.png), [amplia horizontal](.impeccable/review/v040/tablet-landscape-paper-reader-android15.png) y [teléfono horizontal](.impeccable/review/v040/phone-landscape-paper-reader-android15.png).
- [Capítulo largo, 190 páginas con letra 28](.impeccable/review/v040/phone-paper-page-android15.png) y [final del libro](.impeccable/review/v040/phone-final-page-android15.png).
- [Oscuro](.impeccable/review/v040/phone-reader-dark-android15.png), [claro](.impeccable/review/v040/phone-reader-light-android15.png) y [consulta de palabra](.impeccable/review/v040/phone-lookup-dark-android15.png).

Papel aplica fondo #F6EBCF y texto #302B22 al libro y sus barras, manteniendo el tema propio de la biblioteca. Las medidas de 13 pares de texto son ≥5,23:1; el cuerpo alcanza 11,85:1 y los contornos principales 3,96:1. Selección, subrayados y contenedores usan los roles correspondientes del tema. Las paletas clara y oscura de 0.3.0 no cambian. Estas medidas no constituyen una auditoría completa de accesibilidad.

## Límites

No hubo Redmi conectado. Siguen pendientes en hardware los tiradores y menú de selección del fabricante, TTS, selector de archivos, TalkBack, restauración tras terminar el proceso y rendimiento con EPUB grandes o maquetaciones complejas. Un capítulo de prueba largo no certifica todos los libros. Se mantienen los límites de EPUB sin DRM y contenido reflowable del README.

Los significados nuevos requieren internet; los ya guardados y las traducciones con el modelo descargado pueden reutilizarse sin conexión. Las explicaciones reales de Gemini y su facturación no se verifican con una cuenta personal en estas pruebas.

## SHA-256 del APK validado

D6C709D52D6E22932BB4E925784B3A651F8BA001508DD815DCF1D49AD81BD28A
