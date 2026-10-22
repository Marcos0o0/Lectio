# Lectio
<!-- impeccable:product-schema 1 -->

## Platform
android

## Stack
Elección delegada por el usuario: Kotlin y Jetpack Compose. Lectura EPUB reflowable mediante un WebView aislado; SQLite local; corrutinas y ViewModel. Android 8 o posterior.

## Users
Personas hispanohablantes que leen libros en español o inglés y quieren comprender palabras sin abandonar la lectura.

## Product Purpose
Importar libros, continuar donde se dejó la lectura y construir vocabulario personal con consultas contextuales.

## Capabilities and Constraints
MVP local: EPUB sin DRM, progreso, índice, búsqueda, marcadores, subrayados y vocabulario. Traducción inglés-español con modelos de Google ML Kit en el dispositivo, tras una descarga inicial del idioma. Significados nativos en español mediante Wikcionario y en inglés mediante Wiktionary, con caché por idioma y traducción local de las definiciones inglesas. El idioma del EPUB se puede corregir en la consulta y la elección se recuerda por libro. Gemini personal opcional para explicaciones; se conserva compatibilidad avanzada con chat completions. El lector debe funcionar sin red. No se promete compatibilidad con DRM, maquetación fija, audio ni todos los formatos.

## Brand Commitments
Lectio es el nombre confirmado por el usuario el 16 de octubre de 2026. Del latín *lēctiō*: lectura. Repositorio público: https://github.com/Marcos0o0/Lectio. La publicación en tiendas sigue pendiente.

Preferencia visual confirmada: oscuro moderno con grafito, gris pizarra y azul suave para la aplicación. Conservar la tipografía literaria y los controles nativos. El tema inicial es oscuro; las elecciones explícitas Claro y Sistema se conservan. El 17 de octubre de 2026 el usuario pidió además un fondo de lectura amarillo pastel: Papel es una alternativa crema para el libro y su marco, independiente del tema de la aplicación.

La interfaz usa preguntas y acciones concretas, sin eslóganes ni descripciones promocionales. Tocar una palabra abre la consulta inferior; la selección prolongada permite consultar frases. Traducir y consultar significados no requiere una clave del usuario. La consulta separa significado, contexto y notas; los ajustes separan lectura, diccionario y conexión. La personalización ofrece vista previa y controla la página y sus gestos. Las explicaciones en línea son opcionales y su consumo de saldo se indica antes de solicitarlas.

La lectura avanza por páginas adaptadas a la pantalla y la tipografía, mediante flechas o deslizamiento horizontal. Un capítulo largo no se salta con un avance; al terminar sus páginas se continúa con el siguiente. El índice conserva los saltos de capítulo. Los números de página se identifican como relativos al capítulo; no equivalen a una edición impresa. El lugar de lectura se conserva por posición de texto al cambiar la presentación. La lectura inmersiva oculta las barras y ofrece un control visible para salir, además de Atrás. La consulta de palabras y los deslizamientos siguen disponibles.

## Evidence on Hand
Brief: proyecto-lector-epub-ia.md. No hay usuarios, estadísticas ni testimonios verificados. Los libros de muestra se identifican como demostración.

## Product Principles
- Priorizar la lectura y el progreso real.
- Identificar la procedencia de traducciones y explicaciones.
- Mantener el uso local sin cuenta.
- Enviar fragmentos a la IA solo cuando el usuario activa una consulta con su configuración.
