# Cambios

## 0.2.0

- Abre la consulta inferior al tocar o mantener pulsada una palabra; añade «Traducir» al menú de selección de Android y recupera el gesto cuando los controles nativos no envían `touchend` a la página.
- Sustituye el diccionario de muestra escrito a mano por traducción inglés-español con Google ML Kit. Descarga inicial del idioma con elección de red; después traduce palabras, frases y la oración de contexto en el dispositivo.
- Añade conexión personal con Gemini, explicaciones bajo demanda, respuestas breves y caché. Oculta dirección/modelo en opciones avanzadas y conserva proveedores previamente configurados.
- Simplifica la biblioteca a «¿Qué te gustaría leer?» y elimina eslóganes e introducciones promocionales del resto de pantallas.
- Añade pruebas de solicitudes/respuestas de Gemini y amplía la regresión Android para gestos táctiles, descarga y traducción con el modelo real.

## 0.1.1

- Corrige el cierre al abrir libros en Android: WebView puede solicitar URLs sin nombre de archivo y `URLConnection.guessContentTypeFromName(null)` lanza una excepción. Ahora WebView carga sus documentos internos `data:`/`about:blank` y el interceptor valida las rutas de recursos antes de detectar su tipo.
- Añade pruebas de regresión para solicitudes WebView sin ruta, bloqueo de recursos activos y disponibilidad de la fuente local; añade pruebas instrumentadas del lector con Chromium real.
- Cambia la paleta café/crema por grafito, pizarra y azul suave. El tema inicial es oscuro y se mantienen las elecciones explícitas de apariencia.
- Aplica los colores del tema a bloques de cita, selección, subrayados y mensajes de error del lector, además de menús, pantallas y el icono de la app.
- Respeta el margen inferior del sistema para que «Importar EPUB» quede visible sobre la barra de tareas en pantallas grandes.

## 0.1.0

- Versión inicial: biblioteca EPUB local, lectura, consultas de vocabulario, anotaciones, repasos, actividad e IA opcional.
