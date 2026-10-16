# Validación de Lectio 0.1.0

Fecha: 15 de octubre de 2026. Entorno: Windows, JDK 21 integrado de Android Studio y Android SDK 36.

## Comprobaciones ejecutadas

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --max-workers=1 --console=plain
```

- **APK debug generado:** `app/build/outputs/apk/debug/app-debug.apk`.
- **11 pruebas, cero fallos y cero errores:** ocho del parser y lógica, dos de persistencia y una del flujo Compose mediante Robolectric con Android API 35.
- **Lint sin errores:** 12 advertencias (actualizaciones de dependencias y sugerencias KTX) y tres sugerencias sobre estados primitivos de Compose. La ejecución conjunta de compilación, pruebas y lint terminó con `BUILD SUCCESSFUL`.
- **Firma verificada:** `apksigner verify --verbose`, APK Signature Scheme v2.
- **Identidad comprobada con aapt:** `app.lectio.reader`, versión `0.1.0`, Android mínimo API 26 y objetivo API 36.

SHA-256 del APK generado:

```text
1B3681E615076072EE8AB967C99075D6B278D8CB585AA8DA7D22EC756A4934B8
```

## Cobertura

El parser comprueba rutas relativas y escapes, EPUB de muestra con metadatos y orden de lectura, eliminación de contenido activo, límites de lectura, progreso y separación de cachés por contexto/acción/proveedor. También rechaza DTD explícitamente tanto en UTF-8 como en UTF-16 y lee metadatos XML con prefijos y UTF-16 usando el parser incluido en la app.

Las pruebas de almacenamiento verifican que importar el mismo EPUB conserve el progreso y que libros, vocabulario, anotaciones, caché y tiempo sobrevivan a reabrir la base de datos.

El flujo Compose abre biblioteca, vocabulario, actividad y ajustes; cambia a tema oscuro, abre el libro de muestra, guarda un marcador y abre el índice. Incluye una regresión para impedir que el aviso de marcador guardado tape los controles del lector.

Capturas del renderizador nativo de Robolectric, a 412 × 915:

- [Biblioteca clara](.impeccable/review/phone-library-jvm.png)
- [Vocabulario vacío](.impeccable/review/phone-vocabulary-jvm.png)
- [Ajustes oscuros](.impeccable/review/phone-settings-dark-jvm.png)

Estas tres capturas fueron revisadas visualmente. La captura del lector en JVM no demuestra el contenido HTML: Robolectric no ejecuta aquí Chromium como un teléfono real.

## Pendiente en dispositivo

No había dispositivo conectado en `adb devices`. No se ejecutaron pruebas instrumentadas ni una instalación real.

Antes de considerar una publicación, comprobar en un teléfono o emulador:

1. Importar un EPUB propio con imágenes y enlaces; abrirlo sin conexión.
2. Seleccionar palabras y párrafos mediante gestos; comprobar la consulta, el guardado, las notas y los subrayados.
3. Cambiar de capítulo, volver desde un marcador y comprobar el progreso después de cerrar y terminar el proceso.
4. Escuchar pronunciación con el motor TTS y una voz inglesa instalada.
5. Revisar teclado, escalado de texto, lector de pantalla, orientación, pantallas amplias y un libro grande.
6. Configurar un proveedor de IA y comprobar traducción, caché y errores de red. No se hicieron peticiones reales con una clave de pago.

La compatibilidad EPUB del MVP y las restricciones del diccionario están detalladas en [README.md](README.md). La verificación automatizada no equivale a certificación EPUB ni a validación en hardware.
