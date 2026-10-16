# Lectio
<!-- impeccable:product-schema 1 -->

## Platform
android

## Stack
Elección delegada por el usuario: Kotlin y Jetpack Compose. Lectura EPUB reflowable mediante un WebView aislado; SQLite local; corrutinas y ViewModel. Android 8 o posterior.

## Users
Personas hispanohablantes que leen libros en inglés y quieren comprender palabras sin abandonar la lectura.

## Product Purpose
Importar libros, continuar donde se dejó la lectura y construir vocabulario personal con consultas contextuales.

## Capabilities and Constraints
MVP local: EPUB sin DRM, progreso, índice, búsqueda, marcadores, subrayados y vocabulario. Traducción inglés-español con modelos de Google ML Kit en el dispositivo, tras una descarga inicial del idioma. Gemini personal opcional para explicaciones; se conserva compatibilidad avanzada con chat completions. El lector debe funcionar sin red. No se promete compatibilidad con DRM, maquetación fija, audio ni todos los formatos.

## Brand Commitments
Lectio es el nombre confirmado por el usuario el 16 de octubre de 2026. Del latín *lēctiō*: lectura. Repositorio público: https://github.com/Marcos0o0/Lectio. La publicación en tiendas sigue pendiente.

Preferencia visual confirmada: oscuro moderno con grafito, gris pizarra y azul suave; sustituye el café y la crema. Conservar la tipografía literaria y los controles nativos. El tema inicial es oscuro; las elecciones explícitas Claro y Sistema se conservan.

La interfaz usa preguntas y acciones concretas, sin eslóganes ni descripciones promocionales. Tocar una palabra abre la consulta inferior; la selección prolongada permite consultar frases. Traducir no requiere una clave del usuario. Las explicaciones en línea son opcionales y su consumo de saldo se indica antes de solicitarlas.

## Evidence on Hand
Brief: proyecto-lector-epub-ia.md. No hay usuarios, estadísticas ni testimonios verificados. Los libros de muestra se identifican como demostración.

## Product Principles
- Priorizar la lectura y el progreso real.
- Identificar la procedencia de traducciones y explicaciones.
- Mantener el uso local sin cuenta.
- Enviar fragmentos a la IA solo cuando el usuario activa una consulta con su configuración.
