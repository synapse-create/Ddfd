# ⚡ ClipLuz — Claridad, Orden y Acción Local

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-orange.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform: Android Native](https://img.shields.io/badge/Platform-Android_Native-green.svg)](#)
[![Privacy: 100% Offline](https://img.shields.io/badge/Privacy-100%25_Offline-blue.svg)](#)

**ClipLuz** es una herramienta nativa para Android diseñada bajo un paradigma estricto de **privatividad total (privacy-first)** y **operabilidad fuera de la red (offline-first)**. Convierte transcripciones, capturas, imágenes, notas complejas o contenido copiado en el portapapeles en hermosas tarjetas visuales, listas de tareas estructuradas con casillas interactivas, recordatorios locales y magníficos formatos personalizables listos para compartir.

La aplicación opera 100% de manera local y en el dispositivo del usuario. No contiene analíticas cruzadas, trackers, dependencias de red obligatorias, logins ni llamadas externas.

---

## 🎨 Principios de Diseño Visual & Experiencia

- **Minimalismo Cálido**: Diseñado utilizando una paleta reconfortante de arcilla, terracota y tonos salvia suaves que reducen la fatiga ocular y transmiten alivio y orden inmediato.
- **Asimetría Tactil**: Tarjetas con bordes generosos, tipografías contrastadas y espaciados calculados que rompen la rigidez de las listas tradicionales de Android.
- **Microinteracciones Fluidas**: Transiciones de hasta 300ms basadas en dinámicas físicas primaverales (`spring()`) que hacen que presionar casillas o alternar categorías se sienta humano y receptivo.
- **Identidad Viral Sutil**: Las exportaciones visuales cuentan con un marco de diseño y marca sutil ("*Creado con ClipLuz*") que impulsa el descubrimiento de la app de forma natural y sin anuncios molestos.

---

## 🏛️ Arquitectura de Paquetes (Clean Strategy)

El proyecto se despliega bajo una arquitectura limpia simple (Model-View-ViewModel) para robustecer la mantenibilidad y modularidad:

```text
com.example
│
├── domain
│   └── models          # ClipItem y TaskItem (estructuras de datos del dominio)
│
├── data
│   ├── local           # ClipDatabase, ClipDao y Converters para SQLite (Room)
│   ├── repository      # ClipRepository (interfaz simplificada de acceso a modelos)
│   ├── processor       # LocalContentProcessor (motor de análisis léxico y regex local)
│   ├── export          # LocalExporter (generador nativo de PNG en Canvas / MD text)
│   ├── security        # LocalSecurityManager (obfuscador XOR para notas privadas locales)
│   └── settings        # DataStoreManager (almacén de preferencias como tema o PIN de bloqueo)
│
└── ui
    ├── theme           # Color, Theme y Type para Material Design 3
    ├── navigation      # ClipNavigation (grafo de navegación fluido de Compose)
    ├── viewmodel       # ClipViewModel (coordinador general de lógica reactiva de negocio)
    └── screens         # LockScreen, HomeScreen, DetailScreen, TrashScreen y SettingsScreen
```

---

## ⚡ Funciones Esenciales Implementadas

1. **Escanear e Importar (Motor local)**: Analizador que busca patrones léxicos mediante expresiones regulares (regex) para extraer títulos, casillas checkbox (`[ ]` o `- [ ]`), fechas internacionales/locales, recordatorios y tiempos (HH:MM).
2. **Editor de Tarjeta Dinámico**: Permite cambiar los tonos pastel de la tarjeta en tiempo real, clasificar elementos en carpetas y editar manualmente.
3. **Casillas checked interactivas**: Alterna tareas completadas directamente desde el muro principal o el editor.
4. **Cifrado Vault Privado**: Permite guardar textos sensibles obfuscándolos con un algoritmo local de encriptación XOR byte-masking sin peso de librerías ni dependencias remotas.
5. **Historial, Favoritos y Papelera**: Gestión local de papelera para restaurar o purgar permanentemente elementos sin dejar rastro de metadatos en memoria flash.
6. **PIN Bloqueador**: Numpad local diseñado desde cero que bloquea la entrada a la app si se define una clave de seguridad.
7. **Exportador visual Premium**: Dibuja programáticamente sobre un `Canvas` nativo a 1080px para plasmar la tarjeta exacta, los ticks de tareas, badges de fechas y una firma opcional de descubrimiento.

---

## 🛠️ Tecnologías Obligatorias Utilizadas

- **Kotlin** y **Jetpack Compose** (Material 3).
- **Room Database** con Kotlin Symbol Processing (KSP) y reactividad mediante `kotlinx.coroutines.flow.Flow`.
- **Jetpack DataStore Preferences** para un almacenamiento ágil y asíncrono de configuraciones de bloqueo y visuales.
- **Jetpack Navigation Composable** para control de flujos nativos sin bloqueos.
- **Moshi** para una deserialización ultra-rápida y serialización segura de listas complejas de tareas en campos tipo String de Room.

---

## 🚀 Instrucciones de Compilación y Publicación

### Prerrequisitos
- Android Studio Ladybug (2024.2.1) o superior.
- JDK 17 o superior.
- SDK de Android 24 en adelante (minSdk = 24, targetSdk = 36).

### Compilar Proyecto
1. Clona el repositorio:
   ```bash
   git clone https://github.com/tu_usuario/clipluz.git
   cd clipluz
   ```
2. Ejecuta una compilación limpia utilizando Gradle:
   ```bash
   gradle assembleDebug
   ```
3. O corre tus pruebas nativas de interfaz y lógica:
   ```bash
   gradle :app:testDebugUnitTest
   ```

### Publicar en GitHub / Open Source
Este repositorio está legalmente listo y licenciado bajo la **Licencia Apache-2.0**. Puedes subirlo directamente a tu cuenta pública de GitHub reuniendo las banderas de un producto de alta calidad técnica y pulcritud de diseño:
- No contiene ninguna clave secreta insertada en código.
- Cumple con las guías de accesibilidad (touch targets >= 48dp, contrastes).
- Mantiene aislados los archivos locales mediante `FileProvider`.

---

## 📄 Licencia

Este proyecto está licenciado bajo la **Licencia Apache 2.0**: consulta el archivo `LICENSE` en la raíz para más detalles.

*Creado con pasión, claridad y estricto respeto a la privacidad humana. Diseñado para brillar localmente con ClipLuz.*
