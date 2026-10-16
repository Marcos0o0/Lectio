# Cambios

## 0.1.1

- Corrige el cierre al abrir libros en Android: WebView puede solicitar URLs sin nombre de archivo y `URLConnection.guessContentTypeFromName(null)` lanza una excepción. Ahora WebView carga sus documentos internos `data:`/`about:blank` y el interceptor valida las rutas de recursos antes de detectar su tipo.
- Añade pruebas de regresión para solicitudes WebView sin ruta, bloqueo de recursos activos y disponibilidad de la fuente local; añade pruebas instrumentadas del lector con Chromium real.
- Cambia la paleta café/crema por grafito, pizarra y azul suave. El tema inicial es oscuro y se mantienen las elecciones explícitas de apariencia.
- Aplica los colores del tema a bloques de cita, selección, subrayados y mensajes de error del lector, además de menús, pantallas y el icono de la app.
- Respeta el margen inferior del sistema para que «Importar EPUB» quede visible sobre la barra de tareas en pantallas grandes.

## 0.1.0

- Versión inicial: biblioteca EPUB local, lectura, consultas de vocabulario, anotaciones, repasos, actividad e IA opcional.
