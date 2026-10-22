# Third-party notices

The small set of Material icon source files in `app/src/main/java/androidx/compose/material/icons/` is copied from AndroidX Compose Material Icons Extended Android 1.7.8, published by the Android Open Source Project under the Apache License, Version 2.0. Each file retains its original copyright and license header.

Source archive: https://dl.google.com/dl/android/maven2/androidx/compose/material/material-icons-extended-android/1.7.8/material-icons-extended-android-1.7.8-sources.jar

License: https://www.apache.org/licenses/LICENSE-2.0

Only the icons referenced by this app are included, avoiding the full extended icon bundle. Other dependencies retain their respective licenses; see each dependency's published metadata.

Literata is bundled for offline reading and headings under the SIL Open Font License 1.1. Its license is included at `app/src/main/assets/fonts/OFL.txt`. Source: https://github.com/google/fonts/tree/main/ofl/literata

On-device translations use Google ML Kit Translation. Google Translate attribution graphics in `drawable-nodpi/google_translate_*.png` are unmodified official assets from https://docs.cloud.google.com/static/translate/images/google-translate-attribution.zip (retrieved October 16, 2026). They are displayed adjacent to translation results. Usage and attribution: https://developers.google.com/ml-kit/language/translation/translation-terms and https://cloud.google.com/translate/attribution . Google marks retain their respective ownership.

Dictionary definitions are retrieved at runtime from the English Wiktionary REST definition endpoint (`https://en.wiktionary.org/api/rest_v1/page/definition/{word}`). Contributors to the linked Wiktionary entry are the authors. Text is reused under CC BY-SA 4.0: https://creativecommons.org/licenses/by-sa/4.0/ . The app preserves an entry link and license link alongside cached and saved definitions. HTML is converted to plain text, the displayed senses are limited, and Spanish versions are automatic adaptations produced on the device; the English originals remain available. These adaptations remain under CC BY-SA 4.0. Wiktionary content is not bundled as a hardcoded dictionary. Terms: https://foundation.wikimedia.org/wiki/Policy:Terms_of_Use .

Spanish dictionary definitions are retrieved at runtime from Spanish Wiktionary (Wikcionario), using its MediaWiki parse API (`https://es.wiktionary.org/w/api.php?action=parse&prop=text&format=json&formatversion=2&redirects=1&page={word}`). The public search API resolves only matching accent/diaeresis variants; explicitly linked inflection lemmas may be followed once. Sources and CC BY-SA 4.0 attribution are preserved; definitions are converted to plain text and limited for display. Spanish definitions are original Spanish text, not machine translations. Linked terms are extracted from definition links.

The six recorded API responses under `app/src/test/resources/dictionary-es/` are test fixtures retrieved on October 22, 2026, from the Spanish entries patriarcado, feminista, ensombrecieron, despolitización, metalingüísticas and sesgos. Original entry URLs follow `https://es.wiktionary.org/wiki/{entry}`. Their text is by the contributors of those entries under CC BY-SA 4.0; these fixtures are not included as a production dictionary in the APK.
