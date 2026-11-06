# Documentación Arquitectónica y Análisis Completo de UI: IbraStream (React + Tailwind + Capacitor)

> **Origen**: Aplicación extraída del dispositivo móvil (`com.ibrastream.app`), desensamblada y analizada desde el paquete de activos de producción (`assets/public/`).
> **Ubicación de código fuente reconstruido**: [`ui-react/src`](file:///c:/Users/potato/ibramusic-mobile/ui-react/src)

---

## 1. Resumen Ejecutivo y Arquitectura General

**IbraStream** es una aplicación cliente de streaming musical construida originalmente sobre **React**, **Vite** y **Tailwind CSS**, empaquetada como aplicación móvil mediante **Capacitor**. 

### Pila Tecnológica Extraída
- **Framework UI**: React 18 / JSX Runtime
- **Estilos**: Tailwind CSS 4 con capa de diseño personalizada (`@layer theme`, `@layer properties`)
- **Gestión de Estado**: Context API (`AudioContext`, `ThemeContext`, `AuthContext`) con persistencia local exhaustiva vía `localStorage`
- **Servicios Externos Integrados**:
  - **Supabase**: Autenticación de usuarios, perfiles, avatares, sincronización de playlists y tabla de clasificación (`https://nkhonqrseaymilneurgj.supabase.co`).
  - **YouTube & YouTube Music**: Resolución de streaming y sugerencias de búsqueda vía `YouTube.js` y APIs internas.
  - **Deezer API**: Búsqueda global de metadatos, artistas, álbumes y pistas (`https://api.deezer.com`).
  - **Lrclib.net**: Letras sincronizadas en tiempo real con marcas de tiempo LRC (`https://lrclib.net`).
  - **Spotify Web Scraper & PKCE OAuth**: Importación de listas de reproducción de Spotify mediante parseo de payloads Next.js y OAuth PKCE.

---

## 2. Sistema de Diseño (Design Tokens)

### Paleta de Colores
| Token | Valor HEX / OKLCH | Uso |
|---|---|---|
| `--color-brand-accent` | `#e8e8e8` | Acentos primarios, botones destacados, indicadores activos |
| `--color-brand-darkBg` | `#0f0f0f` | Fondo base principal de la aplicación |
| `surface-card` | `#181818` / `oklch(21.6% .006 56.043)` | Superficie de tarjetas de canciones, álbumes y playlists |
| `surface-glass` | `rgba(15, 15, 15, 0.85)` / `0x0f0f0f` al 90% | Barra flotante de mini-reproductor y paneles laterales |
| `border-active` | `rgba(255, 255, 255, 0.10)` | Bordes sutiles en tarjetas y barras de herramientas |
| `accent-emerald` | `#00bb7f` | Indicador de Crossfade activo, playlists públicas |
| `accent-red` | `#fb2c36` | Botón de favoritos (Like) activo, eliminar |
| `accent-amber` | `#edb200` | Corona y podio del "Desempleado del Mes", modo offline |

### Tipografía
- **Fuente Principal**: `Inter`, con fallback a `system-ui, sans-serif`.
- **Pesos**: Regular (400), Medium (500), SemiBold (600), Bold (700), ExtraBold (800), Black (900).
- **Tabular Nums**: Utilizado en contadores de tiempo (`0:00 / 3:45`) y rankings numéricos.

### Efectos Visuales y Glassmorphism
- **Backdrop Blur**: `backdrop-blur-xl` y `backdrop-blur-2xl` para el mini-reproductor móvil y el modal de letras.
- **Ambient Glow**: Reflejo difuso reactivo en el fondo (`blur-3xl`, opacidad 25%) que utiliza la imagen de portada de la canción actual.
- **Animaciones Clave**:
  - `fadeIn (0.3s ease)`: Transición entre páginas y pestañas.
  - `slideUp (0.2s cubic-bezier(0.16, 1, 0.3, 1))`: Aparición del reproductor a pantalla completa y del mini-reproductor.
  - `animate-spin-slow (12s linear infinite)`: Rotación visual del disco de vinilo en la pantalla de reproducción.
  - `pulse`: Punto brillante sobre la carátula indicando reproducción activa.

---

## 3. Desglose Pantalla por Pantalla y Componentes

### 3.1. Navegación y Estructura Base ([`Navbar.tsx`](file:///c:/Users/potato/ibramusic-mobile/ui-react/src/components/layout/Navbar.tsx))
- **En Escritorio (`>= 768px`)**: Barra lateral fija (`w-64`) con logotipo "ibrastream", enlaces directos a Home, Search, Your Library, Stats & Wrapped, Visualizer y Settings, junto con la tarjeta de perfil de usuario en la base.
- **En Móvil (`< 768px`)**: Barra de navegación inferior flotante fija (`h-16`) con acceso a 4 pestañas: Home, Search, Library y Settings.

### 3.2. Pantalla de Inicio ([`HomeView.tsx`](file:///c:/Users/potato/ibramusic-mobile/ui-react/src/components/views/HomeView.tsx))
- **Hero Banner**: Mensaje de bienvenida con degradado sutil (`Let The Music Take You Away`).
- **Quick Picks**: Cuadrícula de 4 elementos con canciones reproducidas recientemente o recomendadas para acceso rápido con 1 clic.
- **Featured Playlists**: Carrusel o rejilla de tarjetas cuadradas con carátula e información de pistas.

### 3.3. Buscador Global ([`SearchView.tsx`](file:///c:/Users/potato/ibramusic-mobile/ui-react/src/components/views/SearchView.tsx))
- **Campo de Búsqueda**: Con icono de lupa, botón de limpieza instantánea y ejecución al presionar Enter.
- **Chips de Filtrado**: Permite filtrar entre `ALL`, `TRACK` (canciones), `ARTIST` (artistas), `ALBUM` (álbumes) y `COMMUNITY` (listas de la comunidad).
- **Historial Reciente**: Chips con términos de búsqueda anteriores guardados en `ibrastream_search_history`.
- **Lista de Resultados**: Muestra índice, carátula, título con resaltado al pasar el ratón, artista enlazado y botón flotante para añadir a playlist.

### 3.4. Biblioteca del Usuario ([`LibraryView.tsx`](file:///c:/Users/potato/ibramusic-mobile/ui-react/src/components/views/LibraryView.tsx))
- **Sub-pestañas**:
  - `Playlists`: Listas creadas e importadas.
  - `Liked Songs`: Canciones marcadas con me gusta.
  - `Albums`: Álbumes guardados en la biblioteca.
  - `Artists`: Artistas seguidos con contador de oyentes.
  - `Listening History`: Historial cronológico de reproducción.
- **Herramientas de Biblioteca**:
  - Ordenación desplegable: `Recents`, `Alphabetical`, `Custom (Drag)`.
  - Botón de importar enlace de Spotify (`Import Spotify Link`).
  - Botón de crear playlist (`Create Playlist`).
  - Indicador de listas fijadas (`Pinned •`) y selector de listas archivadas/ocultas.

### 3.5. Estadísticas y Wrapped ([`StatsView.tsx`](file:///c:/Users/potato/ibramusic-mobile/ui-react/src/components/views/StatsView.tsx))
- **Banner Wrapped 2026**: "Your Journey In Sound".
- **Tarjetas KPI**:
  - Minutos totales escuchados.
  - Total de pistas reproducidas.
  - Personalidad musical calculada según los géneros más escuchados.
- **Top Artistas y Top Canciones**: Tablas de clasificación con recuento de reproducciones y minutos dedicados.
- **Desempleado del Mes 👑**: Podio mensual de la comunidad donde se corona al usuario con mayor tiempo de escucha registrado.

### 3.6. Ajustes y Configuración ([`SettingsView.tsx`](file:///c:/Users/potato/ibramusic-mobile/ui-react/src/components/views/SettingsView.tsx))
- **Audio Playback**:
  - Interruptor y deslizador de duración para **Seamless Transitions (Crossfade)** de 1 a 12 segundos.
  - Selector de calidad de streaming: Normal (128 kbps), High (256 kbps) y Pristine (320 kbps).
  - Modo Offline / Solo Descargadas.
- **Apariencia**:
  - Selección de tema entre `noir` (negro puro), `dark` (gris muy oscuro `#0f0f0f`) y `bright`.
  - Conmutador de brillo ambiental (`Ambient Glow`).
- **Cuenta**: Estado de sesión en Supabase y cierre de sesión.

---

## 4. Componentes del Reproductor de Audio

### 4.1. Mini-Reproductor Móvil ([`MobileMiniPlayer.tsx`](file:///c:/Users/potato/ibramusic-mobile/ui-react/src/components/player/MobileMiniPlayer.tsx))
- Flotante a 12px de los bordes laterales y situado sobre la barra de pestañas móvil.
- Altura de 60px con esquinas redondeadas (`rounded-2xl`) y fondo con desenfoque (`bg-[#0f0f0f]/90`).
- **Línea de progreso ultra-fina** de 2px en el borde superior que avanza en tiempo real.
- Carátula de 44x44 con punto pulsante, título, artista, botón de like, botón circular blanco de play/pause y botón de salto a la siguiente canción.

### 4.2. Barra de Reproducción de Escritorio ([`DesktopPlayerBar.tsx`](file:///c:/Users/potato/ibramusic-mobile/ui-react/src/components/player/DesktopPlayerBar.tsx))
- Fija en la base en pantallas grandes (`h-24`).
- Estructura en tres columnas: Información a la izquierda, controles centrales de transporte con deslizador de tiempo numérico, y controles de volumen, cola y crossfade a la derecha.

### 4.3. Reproductor a Pantalla Completa ([`NowPlayingModal.tsx`](file:///c:/Users/potato/ibramusic-mobile/ui-react/src/components/player/NowPlayingModal.tsx))
- Fondo dinámico ambient glow con la imagen de portada ampliada y desenfocada.
- Alternador entre modo carátula y modo vinilo rotatorio (`Vinyl`).
- Barra de herramientas inferior con botón directo para abrir letras sincronizadas y botón de apertura de la cola.

### 4.4. Letras Sincronizadas ([`LyricsView.tsx`](file:///c:/Users/potato/ibramusic-mobile/ui-react/src/components/player/LyricsView.tsx))
- Obtención automática desde `lrclib.net` con marcas de tiempo precisas.
- Desplazamiento automático suave hacia la línea activa.
- Clic en cualquier estrofa para saltar a ese segundo exacto de la canción.
- **Herramienta de sincronización manual**: controles `-0.5s` y `+0.5s` para calibrar el retardo de audio en tiempo real.

### 4.5. Cola de Reproducción ([`QueueDrawer.tsx`](file:///c:/Users/potato/ibramusic-mobile/ui-react/src/components/player/QueueDrawer.tsx))
- Panel lateral derecho con división clara entre:
  1. Canción en reproducción (`Now Playing`).
  2. Cola prioritaria añadida por el usuario (`User Priority Queue`) con opción de eliminar canciones individuales o vaciar la cola.
  3. Próximas canciones del contexto o playlist original.

---

## 5. Tabla de Correspondencia: IbraStream (React) vs IbraMusic (Kotlin Compose)

| Característica / Vista | Implementación en IbraStream (React) | Estado en IbraMusic Mobile (Android/Compose) |
|---|---|---|
| **Mini Reproductor** | `MobileMiniPlayer.tsx` flotante 60px | `MiniPlayerBar.kt` nativo en Compose con pulso y progreso |
| **Reproductor Completo** | `NowPlayingModal.tsx` con Vinilo y Glow | `NowPlayingScreen.kt` con soporte para ecualizador y cola |
| **Cola de Reproducción** | `QueueDrawer.tsx` | `QueueScreen.kt` con drag and drop y `QueueManager` |
| **Letras Sincronizadas** | `LyricsView.tsx` con sync offset | `LyricsScreen.kt` integrado con `lrclib.net` |
| **Transiciones Crossfade** | Dual-deck en AudioContext | `ExoPlayerWrapper` con arquitectura de doble deck simultáneo |
| **Estadísticas y Wrapped** | `StatsView.tsx` ("Desempleado del Mes") | `UserStatsManager` y `StatsScreen.kt` |
| **Persistencia de Sesión** | `localStorage` | Room SQLite (`QueueDao`) y `PlaybackSessionManager` |
| **Importador Spotify** | `spotifyImporter.js` (Embed parse + PKCE) | Integrable desde el nuevo mapa `ui-react` |
