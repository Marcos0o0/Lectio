# Validación de Lectio 0.6.0

Comprobaciones del 23 de octubre y cierre de revisión el 24 de octubre de 2026. Windows, JDK 21 de Android Studio y Android SDK 36. Paquete `app.lectio.reader`, versión 0.6.0 (código 7), mínimo API 26 y objetivo API 36.

## Compilación y pruebas

`:app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline --max-workers=1`, con heap de 1 GB, terminó correctamente en 4 min 26 s. El APK de pruebas instrumentadas también compiló correctamente. Lint no encontró errores; conserva seis advertencias y dos sugerencias.

**32 pruebas JVM, cero fallos:** ocho de EPUB, tres de recursos WebView, tres de Gemini, seis de diccionario, dos de idioma, tres de organización de biblioteca, seis de almacenamiento y una del flujo Compose.

Las comprobaciones nuevas cubren orden español por título/autor, actividad reciente frente a fecha de importación, desempates estables, búsqueda sin tildes, Unicode combinado, distinción n/ñ, combinación con favoritos y estados de lectura. Se comprueba que favoritos y orden sobrevivan a otra instancia de SettingsStore, sin modificar las preferencias de lectura, y que una instalación anterior reciba valores compatibles. La biblioteca se organiza sin escribir en los registros de progreso de los libros.

La firma del APK fue verificada con apksigner; aapt confirma la versión, el código y los niveles de API.

## Android 15

**Un flujo instrumentado nuevo aprobado**, en emulador Android 15/API 35 x86_64, con el APK 0.6.0: `libraryOrganizationPersistsWithoutChangingReadingProgress`, `OK (1 test)` en 14,575 s. Registro local: `.tools/android15/v060-library-native.txt`.

El recorrido usa tres libros de prueba identificados como «Muestra». Busca un título con tilde omitiendo ese acento, cambia el orden y comprueba su guardado, marca dos favoritos, recrea la actividad, verifica el filtro y las preferencias restauradas, quita ambos favoritos hasta el estado vacío y vuelve a añadirlos. Al terminar comprueba en SQLite que la posición y fecha de lectura del libro de prueba siguen intactas. Los controles se activan mediante la accesibilidad nativa de Android.

La primera ejecución quedó bloqueada por un aviso de System UI del emulador, antes de llegar a Lectio; se cerró ese aviso. En otra ejecución se ajustó el test para desplazar la lista hasta el resultado de búsqueda antes de comprobarlo. Esos intentos no se presentan como pruebas aprobadas. No fue necesario cambiar el APK de la app durante estos ajustes del test.

No se repitieron los cinco flujos instrumentados de lectura de 0.5.0, cuya implementación no cambia en esta entrega. Su evidencia previa permanece en Git y en [.impeccable/review/v050](.impeccable/review/v050). La prueba JVM de navegación y apertura del lector sí forma parte de las 32 pruebas actuales.

## Evidencia visual

Capturas reales del emulador en [.impeccable/review/v060](.impeccable/review/v060): biblioteca, favoritos, búsqueda, menú de orden y estado vacío. La matriz incluye teléfono 720 × 1600/densidad 280, texto al 130 %, tableta horizontal 1800 × 1280/densidad 240 y teléfono horizontal 1600 × 720/densidad 280, con temas Claro y Oscuro. Las muestras son datos de prueba; sus portadas tipográficas no son cubiertas editoriales originales.

Se conservan las paletas, fuentes y componentes Material de 0.5.0. Las capturas JVM no se usan como evidencia de dispositivo.

La revisión independiente Impeccable de esta extensión terminó con disposición **ship**, sin correcciones materiales: las diez capturas son válidas y los controles de biblioteca mantienen el sistema existente. En horizontal compacto la lista se desplaza; las capturas inicial y desplazada muestran que el orden y las acciones de fila siguen accesibles.

La comparación de documentación confirmó que no hay decisiones nuevas del sistema visual que persistir: DESIGN.md y su sidecar se conservan sin cambios. La función está descrita en PRODUCT.md, README.md, CHANGELOG.md y el contrato de biblioteca.

## Límites

No hubo Redmi físico conectado. Siguen pendientes en hardware los gestos y tiradores de selección del fabricante, TTS y voces instaladas, TalkBack, selector de archivos, restauración de lectura tras terminar el proceso y rendimiento con EPUB grandes. La recreación de una actividad no demuestra restauración después de que Android termine el proceso.

Los favoritos y el orden son locales; no hay cuenta, sincronización ni copias de seguridad. Quitar un favorito conserva el libro y su progreso. Eliminar un libro también elimina su marca de favorito; se conserva el comportamiento previo para vocabulario y archivo original. El filtro visible y la búsqueda se restauran durante la recreación de pantalla, mientras que favoritos y orden se guardan entre aperturas.

No se introducen permisos ni servicios nuevos, ni se realizan consultas de Gemini. Se mantienen los límites de EPUB y diccionario indicados en el README.

## SHA-256 del APK

`C8F82E585A66FBF23465F8999D8E512ED3FD640FF7C54A5A49ABE393E6EF831A`
