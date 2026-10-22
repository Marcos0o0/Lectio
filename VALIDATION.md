# Validación de Lectio 0.5.0

22 de octubre de 2026. Windows, JDK 21 de Android Studio y Android SDK 36. Paquete `app.lectio.reader`, versión 0.5.0 (código 6), mínimo API 26, objetivo API 36.

## Compilación y datos

`:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug --offline --max-workers=1`, con heap de 1 GB, terminó correctamente. La última compilación completa del APK duró 4 min 53 s. Los ajustes posteriores al test instrumentado también compilaron correctamente.

- **28 pruebas JVM, cero fallos:** ocho de EPUB, tres de recursos WebView, tres de Gemini, seis de diccionario, dos de idioma, cinco de almacenamiento y una del flujo Compose.
- Los seis ejemplos del usuario tienen respuestas reales de Wikcionario como fixtures de test: patriarcado, feminista, ensombrecieron, despolitización, metalingüísticas y sesgos. Se verifican separación de idiomas, limpieza de ejemplos, enlaces a formas base, Unicode/NFC y atribución tras caché.
- La búsqueda de grafías sin acento solo acepta la misma palabra al quitar tildes o diéresis; conserva la diferencia entre n y ñ y rechaza resultados aproximados. Un homógrafo con significado propio conserva sus acepciones aunque incluya también una forma verbal.
- Cachés española e inglesa independientes para la misma palabra, lectura compatible de cachés antiguas, idioma y elección por libro persistentes. Se conservan las pruebas de posiciones de texto, migración de datos y preferencias consecutivas.
- Lint: **cero errores, nueve advertencias y tres sugerencias**. Las advertencias corresponden a actualizaciones de dependencias y recomendaciones KTX.
- Firma v2 verificada con apksigner; aapt confirma versión, código y niveles de API.

## Android y Chromium

**Cinco flujos instrumentados aprobados en Android 15/API 35 x86_64**, con Chromium real y el mismo APK. Cuatro pruebas se aprobaron en el recorrido conjunto; la comprobación Claro/Oscuro se aprobó en una ejecución focalizada de 15,302 s (`OK (1 test)`) después de mejorar la consulta de accesibilidad del test y la espera al regresar a la biblioteca. Los registros locales están en `.tools/android15/v050-native.txt` y `v050-themes-native.txt`.

1. Capítulo de 90 párrafos: páginas sin saltar capítulos, flechas y deslizamiento táctil, cambio de letra y márgenes, Papel, búsqueda, enlaces internos, cruce de capítulos y reapertura conservando el texto. Añade entrada inmersiva desde una página posterior al inicio, barras del sistema ocultas, deslizamiento dentro de pantalla completa, salida con Atrás sin cerrar el libro, giro conservando el texto y salida mediante el botón visible.
2. Libro en español: toque real de las seis palabras comunicadas por el usuario. Confirma definiciones originales españolas, forma base de ensombrecieron y metalingüísticas, grafía corregida de despolitizacion, términos enlazados de feminista y ausencia de la definición inglesa incorrecta «hispana». Guarda sesgos sin traducción ni Gemini; cambia Español/Inglés, recupera la caché española y conserva la elección al reabrir. La consulta funciona dentro del modo inmersivo y también por pulsación prolongada.
3. Personalización persistida y estilos calculados en Chromium: fondo, márgenes, separación de párrafos, alineación y controles.
4. Regresión inglesa: selección prolongada y toque reales, traducción local, ficha de significado/contexto/notas, acceso a Gemini y regreso al libro, índice, marcador y reapertura.
5. Cambio Claro/Oscuro aplicado al lector y flujo de conexión opcional de Gemini.

La prueba española usa la API pública real de Wikcionario, además de reutilizar las entradas guardadas al repetir consultas. No se introducen claves ni se consumen solicitudes de Gemini. Las frases inglesas se seleccionan mediante rango DOM: sigue pendiente verificar el arrastre físico de los tiradores de selección.

## Capturas nativas

Evidencia en [.impeccable/review/v050](.impeccable/review/v050). Teléfono 720 × 1600/densidad 280, texto al 130 %, ventana amplia horizontal 1800 × 1280/densidad 240 y teléfono horizontal 1600 × 720/densidad 280.

- Pantalla completa [oscura](.impeccable/review/v050/phone-immersive-dark-android15.png) y [Papel](.impeccable/review/v050/phone-immersive-paper-android15.png).
- Significados de [feminista](.impeccable/review/v050/phone-spanish-feminista-android15.png), [ensombrecieron](.impeccable/review/v050/phone-spanish-verb-android15.png) y [sesgos guardado](.impeccable/review/v050/phone-spanish-cached-android15.png).
- [Regresión inglesa](.impeccable/review/v050/phone-lookup-dark-android15.png).
- Texto al 130 %: [lector inmersivo](.impeccable/review/v050/phone-font130-spanish-immersive-android15.png) y [consulta](.impeccable/review/v050/phone-font130-spanish-lookup-android15.png).
- Ventana amplia: [lector inmersivo](.impeccable/review/v050/tablet-landscape-spanish-immersive-android15.png) y [consulta](.impeccable/review/v050/tablet-landscape-spanish-lookup-android15.png).
- Teléfono horizontal: [lector inmersivo](.impeccable/review/v050/phone-landscape-spanish-immersive-android15.png) y [consulta desplazada](.impeccable/review/v050/phone-landscape-spanish-lookup-scrolled-android15.png).

Las paletas y la tipografía de 0.4.0 se conservan. Las capturas corresponden a Android y controles nativos; no son mockups ni un navegador de escritorio.

La revisión Impeccable del alcance nativo terminó con disposición **ship**: las 17 capturas no mostraron defectos visuales materiales. Se corrigió la documentación de diseño para reflejar las consultas españolas y la lectura inmersiva, conservando los tokens existentes; una revisión posterior confirmó esa corrección. Esta aprobación no amplía la cobertura de hardware indicada abajo.

## Límites

No hubo Redmi físico conectado. Siguen pendientes en hardware TTS y sus voces instaladas, tiradores y menú de selección del fabricante, selector de archivos, TalkBack, restauración tras terminar el proceso y rendimiento con EPUB grandes. Un capítulo largo no certifica todos los libros. Se mantienen los límites de EPUB sin DRM y contenido reflowable del README.

Wikcionario y Wiktionary pueden carecer de una palabra o de una acepción. Las formas base se consultan a partir de enlaces explícitos, con un máximo de un salto. No se adivina el sentido contextual de una acepción. Las palabras nuevas requieren internet; las entradas guardadas se reutilizan por idioma. Las traducciones inglesas requieren el modelo local descargado. Las explicaciones reales de Gemini y su facturación no se verifican con una cuenta personal.

## SHA-256 del APK

`B59C7BF41E712C5AF0AF7F3759AE0B02A8A41B57417011DE9FED154E4A0295CC`
