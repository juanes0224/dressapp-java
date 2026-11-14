# Guía para grabar el video de DressApp (≈ 3 minutos, sin narración)

El video solo muestra la pantalla. En cada paso está lo que hay que **escribir** (cada línea se
confirma con **Enter**) y lo que **debe verse** antes de seguir. Si editas el video, la última
columna tiene un texto corto opcional para poner en pantalla.

## Antes de grabar (no sale en el video)

1. **Tener Java.** Abre una terminal (`cmd`) y escribe `javac -version`. Si sale un número de
   versión, listo. Si no, instala un JDK 17 o 21 (por ejemplo *Eclipse Temurin*) y vuelve a probar.
2. **Descargar el proyecto** de GitHub: botón verde **Code → Download ZIP** y descomprímelo,
   o con git: `git clone https://github.com/juanes0224/dressapp-java.git`
3. **Compilar:** en la carpeta del proyecto, doble clic en **`compilar.bat`**. Debe decir
   `Compilacion exitosa`. Cierra esa ventana.
4. **Preparar la ventana:** doble clic en **`ejecutar.bat`**, maximiza la ventana y sube la letra
   (clic derecho en la barra de título → *Propiedades* → *Fuente* → tamaño 18 a 20).
   Si las tildes salen raras, no pasa nada grave, pero `ejecutar.bat` ya activa UTF-8.
5. **Grabador:** `Win + Alt + R` inicia y detiene la grabación con la barra de juegos de Windows.
   Si no graba la consola, usa OBS Studio (captura de ventana).
6. Cierra `ejecutar.bat` y ábrelo de nuevo justo antes de grabar, para que el closet arranque limpio.

**Ritmo:** escribe despacio y, cuando aparezca un resultado, **espera 3 o 4 segundos** sin tocar
nada. Si el resultado no cabe en pantalla, sube con la rueda del mouse para mostrar el comienzo.

## Guion

### Parte 1 — El closet (≈ 40 s)

| Paso | Escribe | Debe verse | Texto en pantalla (opcional) |
|---|---|---|---|
| 1 | *(abrir `ejecutar.bat`)* | Título "DressApp" y el menú con 15 opciones | DressApp: calculador de outfits por puntaje |
| 2 | `2` | Tabla con las **31 prendas** del closet de ejemplo | Closet de ejemplo: 31 prendas |
| 3 | `6` → `PANTALON` | Las 3 prendas "Pantalón …" (las encuentra aunque se escriba sin tilde y en mayúsculas) | Búsqueda con Trie: ignora tildes y mayúsculas |

### Parte 2 — Agregar una prenda (≈ 30 s)

| Paso | Escribe | Debe verse | Texto en pantalla (opcional) |
|---|---|---|---|
| 4 | `3` | Formulario "Agregar prenda" | |
| 5 | `Chaqueta de jean` → `2` → `1` → `azul` → `#4A78C2` → `2` | Elige capa *exterior*, subtipo *chaqueta*, fit *regular* | |
| 6 | `7` | **"Escribe un número entre 1 y 5."** (error a propósito) | Validación de datos |
| 7 | `2` → `3` → *Enter vacío* | `Prenda agregada: #32 Chaqueta de jean` | Enter acepta los eventos sugeridos (*) |

### Parte 3 — Recomendaciones (≈ 45 s)

| Paso | Escribe | Debe verse | Texto en pantalla (opcional) |
|---|---|---|---|
| 8 | `8` → `3` → `n` | **3 outfits para un plan nocturno**, cada uno con puntaje G, desglose y "Por qué" | Grafo por capas + Dijkstra + Yen |
| 9 | `9` → `12` → `5` → `s` → `5` → `n` | Outfits **con el Jean azul** a 5 °C: todos traen **capa exterior**, la opción 1 usa la **Chaqueta de jean** recién agregada, y al final "Backtracking: … nodos explorados, … ramas podadas" | Completar outfit: backtracking con poda |

### Parte 4 — Demostración guiada (≈ 60 s)

| Paso | Escribe | Debe verse | Texto en pantalla (opcional) |
|---|---|---|---|
| 10 | `15` | "Demo 1/6 - Perfil inicial" | |
| 11 | *Enter* | Demo 2/6: 3 outfits casuales | |
| 12 | *Enter* | Demo 3/6: medio formal con 8 °C y lluvia → exterior obligatorio, calzado cerrado | El clima cambia la recomendación |
| 13 | *Enter* | Demo 4/6: tabla de pesos con `w = 0,168`, `n = 5` | Aprendizaje: calificó con 1 cinco veces |
| 14 | *Enter* | Demo 5/6: nueva recomendación y la frase **"El outfit rechazado ya no aparece en el top 3"** | La recomendación aprendió |
| 15 | *Enter* | Demo 6/6: outfits con la blusa roja; "Con poda: 28 nodos … Sin poda: 40 nodos … Mismos puntajes" | La poda no pierde soluciones |

### Parte 5 — Estructuras y pruebas (≈ 25 s)

| Paso | Escribe | Debe verse | Texto en pantalla (opcional) |
|---|---|---|---|
| 16 | `13` | Árbol de categorías (prenda → 5 categorías → 23 subtipos) | Árbol de categorías (DFS) |
| 17 | `14` | Lista de chequeos `[ OK ]` y al final **`60/60 chequeos superados.`** (subir un poco para mostrar algunos) | 60/60 pruebas superadas |
| 18 | `0` | "Hasta luego." Detener la grabación | |

## Si algo sale distinto

- **`'javac' no se reconoce…`**: falta instalar el JDK (paso 1 de "Antes de grabar").
- **`Primero ejecuta compilar.bat`**: aún no se compiló; haz doble clic en `compilar.bat`.
- **La prenda nueva no es la #32**: el closet no estaba limpio. Cierra y abre `ejecutar.bat`
  y repite desde el paso 1. En el paso 9 se usa la prenda **#12** (Jean azul), que siempre existe.
- **Te equivocaste de tecla**: el programa no se cierra; vuelve a pedir el dato. Si quedaste en
  otra pantalla, termina ese paso y sigue con el siguiente; en la edición puedes recortarlo.
- **Sin terminal**: también se puede abrir la carpeta en IntelliJ y ejecutar
  `src/com/dressapp/app/Main.java` (botón ▶). Las teclas son las mismas.
