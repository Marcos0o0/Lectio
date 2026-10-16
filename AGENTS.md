# Lectio

Aplicación Android nativa en Kotlin y Jetpack Compose. Consulta README.md, PRODUCT.md y VALIDATION.md antes de cambiar el alcance o describir la cobertura de pruebas.

## Git

- Por indicación del usuario, después de cada conjunto coherente de cambios terminado, ejecuta las comprobaciones pertinentes, crea un commit descriptivo y haz push a la rama de trabajo en origin.
- Esta autorización persiste para los cambios del proyecto: no vuelvas a pedir permiso para commits y pushes normales.
- No hagas commits vacíos, ni inventes un historial por fases de trabajo ya realizado. No incluyas cambios ajenos a la tarea sin revisarlos.
- No subas claves, archivos .env, local.properties, almacenes de firma, datos personales de lectura, cachés ni archivos de compilación.
- No uses force push ni reescribas commits publicados sin una instrucción explícita del usuario.
- Si un push falla, conserva el commit local e informa del problema; no afirmes que quedó sincronizado.

## Validación

Usa el JDK 21 integrado de Android Studio y Android SDK 36. Para cambios de código, selecciona las comprobaciones apropiadas entre assembleDebug, testDebugUnitTest y lintDebug. Para cambios solo de documentación o configuración de Git, revisa el diff; no es necesario recompilar la aplicación.

Las capturas Robolectric son evidencia de JVM. No las presentes como pruebas en un dispositivo real; la selección dentro de WebView y el motor TTS requieren validación en Android.
