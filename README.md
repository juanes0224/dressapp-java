# DressApp — calculador de outfits por puntaje (Java, terminal)

**Proyecto final — Análisis y Diseño de Algoritmos**

**Integrantes:** Mateo Duque · Sofía Rojo · Juan Esteban Grisales

Implementación en Java puro de la lógica de DressApp (`DressApp_Plan_de_trabajo.pdf`):
el usuario registra las prendas de su closet y, al elegir un evento, recibe los outfits con
mejor puntaje. **Sin IA y sin interfaz gráfica**: la decisión la toma un motor propio y
determinista (estructuras de datos, puntuación por reglas, grafos y aprendizaje por
calificaciones), y todo se usa desde un menú en la terminal.

Solo usa la biblioteca estándar de Java (compatible desde Java 8). No hay base de datos: el
closet vive en memoria y arranca con un closet de ejemplo de 31 prendas.

## Cómo ejecutarlo

Con un JDK instalado (`javac -version` debe funcionar, o `JAVA_HOME` apuntar al JDK), desde
esta carpeta:

```bash
compilar.bat
```

```bash
ejecutar.bat
```

O a mano:

```bash
javac -source 8 -target 8 -d out -encoding UTF-8 -sourcepath src src\com\dressapp\app\Main.java
```

```bash
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out com.dressapp.app.Main
```

`compilar.bat` genera bytecode de Java 8 para que el programa corra con cualquier `java` 8 o
superior del PATH, aunque el JDK que compila sea más nuevo.

Si las tildes salen como `�`, ejecuta `chcp 65001` antes (ya lo hace `ejecutar.bat`).
En IntelliJ: marca `src` como *Sources Root* y ejecuta `com.dressapp.app.Main`.

**Para presentar:** la opción **15 (demostración guiada)** recorre el flujo completo en
6 pasos, y la **14 (autoverificación)** corre 60 chequeos de correctitud en un par de segundos.

## Menú

| # | Opción | Qué muestra del proyecto |
|---|--------|--------------------------|
| 1 | Configurar perfil | cuestionario inicial; alimenta `perfil(p)` |
| 2–5 | Ver / agregar / editar / eliminar prenda | CRUD con validación (rango 1–5, hex, subtipo según el árbol) |
| 6 | Buscar por nombre | **Trie**: autocompletado sin importar tildes ni mayúsculas |
| 7 | Filtrar | **tablas hash** por capa, color, fit y evento (intersección) |
| 8 | Recomendar outfits | **grafo por capas + Dijkstra + Yen + re-ranking con MinHeap + diversidad** |
| 9 | Completar a partir de una prenda | **backtracking con poda** por cota optimista |
| 10 | Calificar | **aprendizaje**: actualiza los pesos `w` de cada par |
| 11 | Favoritos | marcar, ver y quitar |
| 12 | Ver pesos aprendidos | tabla `w`, `n`, `λ` por par y evento |
| 13 | Ver árbol de categorías | **recorrido DFS** del árbol categoría → subtipo |
| 14 | Autoverificación | 60 chequeos: casos borde, propiedades y equivalencias con fuerza bruta |
| 15 | Demostración guiada | perfil → dos eventos → calificación → cambio en la recomendación |

## Estructura del código

```
src/com/dressapp/
  util/         Texto            normalización (minúsculas, sin tildes) compartida
  modelo/       Prenda, Perfil, Evento, Categoria, Fit, Outfit, PesoAprendido,
                ContextoClima, CapaVaciaException
  estructuras/  Trie, MinHeap, ArbolCategorias, Grafo (Dijkstra)      <- sin librerías
  motor/        Config           todas las constantes del modelo
                Colores          rueda HSL y relación de color
                Puntuacion       u(p,e), c(p,q), G(outfit) con desglose
                Recomendador     grafo por capas, Dijkstra, Yen, diversidad, backtracking
                Aprendizaje      actualización de pesos por calificación
                Explicador       explicación por plantilla (sin LLM)
  datos/        Repositorio      closet + trie + índices hash
                ClosetEjemplo    31 prendas de muestra
  app/          Main (menú), Consola (entrada validada), Vista (salida),
                Demo (guion), Pruebas (autoverificación)
```

## Modelo de puntuación

Todas las puntuaciones están en [0, 1]. Las constantes están en `motor/Config.java`.

**Idoneidad de una prenda para el evento**

```
u(p,e)    = 0.6·form + 0.2·clima + 0.2·perfil
form      = 1 − |formalidad(p) − formalidad_objetivo(e)| / 4
clima     = 1 − |abrigo(p) − abrigo_requerido(T)| / 4          (1.0 sin clima)
            con lluvia ≥ 60 %: sandalia −0.5, calzado cerrado +0.1
perfil    = 0.5 + 0.5·[color preferido] − 0.5·[color evitado] + 0.25·[fit preferido]
abrigo_requerido(T): ≥24 °C→1 | 18–24→2 | 12–18→3 | 6–12→4 | <6→5
```

Filtro duro: se descarta la prenda si `form < 0.5` (más de 2 niveles de diferencia) o si el
evento no está entre sus eventos aptos. Si una capa obligatoria queda vacía se informa cuál.

**Compatibilidad entre dos prendas**

```
c_reglas = 0.6·color + 0.4·fit
color    : neutro (negro, blanco, gris, beige, azul marino) 1.00 | mismo matiz (≤15°) 0.80
           análogos (≤45°) 0.85 | complementarios (|Δh−180°| ≤ 20°) 0.70 | resto 0.40
fit      : solo entre superior e inferior (en otro par 1.0):
           ambos slim 0.8 | ambos ≥ baggy: 0.9 en eventos relajados, 0.4 en formales
           |Δvolumen| ≤ 1 → 1.0 | otro caso 0.6
c        = (1 − λ)·c_reglas + λ·w ,   λ = n / (n + 3)
```

**Puntaje global**: `G(o) = 0.5·promedio(u) + 0.5·promedio(c)` sobre **todos** los pares.

**Aprendizaje**: `r = (valor − 1)/4`; para cada par del outfit calificado,
`w ← 0.7·w + 0.3·r` (w inicial = `c_reglas`) y `n ← n + 1`.

### Casos calculados a mano (y verificados en la opción 14)

| Outfit en evento muy formal | u de cada prenda | c del par | G |
|---|---|---|---|
| camisa blanca slim (form 5) + pantalón azul marino slim (form 5) | 0.6·1 + 0.2·1 + 0.2·0.5 = **0.90** | 0.6·1.0 + 0.4·0.8 = **0.92** | **0.91** |
| buzo gris ultra baggy (form 1) + sudadera gris ultra baggy (form 1) | 0.6·0 + 0.2 + 0.1 = **0.30** | 0.6·1.0 + 0.4·0.4 = **0.76** | **0.53** |

Evolución de `w` con calificaciones de 5 partiendo de `c_reglas = 0.5`:
0.65 → 0.755 → 0.8285 → 0.87995 → 0.915965, con λ = 1/4, 2/5, 1/2, 4/7, 5/8.

## Algoritmo de recomendación

**Grafo por capas**: `INICIO → superior → [exterior] → inferior → calzado → [accesorio] → FIN`.

```
costo(INICIO → q)     = β·(1 − u(q))
costo(p → q)          = α·(1 − c(p,q)) + β·(1 − u(q))      (incluye el salto superior → inferior)
costo(accesorio → FIN) = 0
costo(calzado → FIN)   = γ                                   (outfit sin accesorio)
α = β = 0.5, γ = 0.10
```

Las capas opcionales se omiten con aristas de salto, no con nodos "ninguno", para que el costo
de cada arista dependa solo de sus dos extremos. Con frío (abrigo requerido ≥ 4) no se crean
saltos sobre la capa exterior y se vuelve obligatoria.

**Flujo de `recomendar`**:
1. **Yen** obtiene los K = 10 caminos de menor costo (cada desvío se busca con el Dijkstra propio).
2. **Re-ranking**: cada candidato se puntúa con G (todos los pares) y se ordena con el MinHeap.
3. **Diversidad**: se omite un candidato que comparta más de 2 prendas con uno ya elegido.

**Completar alrededor de una prenda fija** (backtracking): recorre las capas eligiendo una
prenda por capa (o ninguna, si la capa es opcional) y poda la rama cuando su cota optimista no
supera al peor del top 3. La cota supone que todo lo que falta vale 1:
`G ≤ 0.5·(ΣU + r)/(k + r) + 0.5·(ΣC + pares nuevos)/pares totales`, maximizada sobre la cantidad
`r` de prendas que todavía pueden entrar.

## Complejidad

| Operación | Complejidad |
|---|---|
| Trie: insertar / eliminar | O(L log σ) (hijos en `TreeMap` para salir en orden alfabético) |
| Trie: buscar prefijo | O(L log σ + nodos recorridos hasta llegar a k resultados) |
| MinHeap: push / pop / peek | O(log n) / O(log n) / O(1) |
| Árbol: esSubtipoDe / DFS | O(1) por nivel / O(nodos) |
| Repositorio: obtener / filtrar | O(1) / O(tamaño del closet) |
| Dijkstra | O((V + E) log V) |
| Grafo por capas con m prendas por capa | V ≈ 5m, E ≈ m² por par de capas conectadas |
| Yen (K caminos) | O(K · V · (V + E) log V) |
| Backtracking | O(∏ opciones por capa) en el peor caso; la poda recorta ramas |

## Respuestas para la sustentación

- **¿Por qué Dijkstra es válido en este grafo?** Todos los costos son ≥ 0 (`u` y `c` están en
  [0, 1] y α, β, γ ≥ 0), y `Grafo.agregarArista` rechaza cualquier peso negativo.
- **¿Qué no captura el camino más corto?** Solo suma pares consecutivos (no mira, por ejemplo,
  superior con calzado). Por eso se sacan K candidatos con Yen y se re-rankean con G, que mira
  todos los pares.
- **¿Cómo se resuelve el arranque en frío?** `λ = n/(n+3)`: con n = 0 la compatibilidad es solo
  la regla; con más calificaciones pesa más lo aprendido.
- **¿Cómo se sabe que el recomendador es correcto?** La autoverificación compara Dijkstra contra
  la enumeración de todos los caminos, Yen contra los k menores costos exhaustivos y el
  backtracking con poda contra el backtracking sin poda, en decenas de closets aleatorios.
- **¿Por qué no se usa IA?** El resultado es determinista, explicable y verificable con pruebas;
  la explicación se arma con plantillas a partir del desglose.

## Decisiones de diseño

El PDF tenía partes de las fórmulas ilegibles por la extracción del texto, y algunas reglas no
fijaban todos los casos. Esto es lo que se decidió, y dónde está en el código:

1. **Costo de salir de INICIO**: se usa `β·(1 − u)`, el mismo término de idoneidad que en el
   resto de aristas. Con α = β = 0.5 el valor es idéntico al que daría α.
2. **Saltos**: el salto superior → inferior cuesta lo mismo que una arista normal; el único salto
   penalizado es calzado → FIN (outfit sin accesorio) con γ = 0.10. (`Recomendador.construirGrafo`)
3. **Orden de las reglas de fit**: los casos "ambos slim" y "ambos holgados" se evalúan antes que
   "|Δvolumen| ≤ 1"; en el orden literal nunca se aplicarían. Los casos no cubiertos valen 0.6.
   (`Puntuacion.fitPar`)
4. **G**: δ = ε = 0.5 (el plan no fijaba los valores).
5. **Aprendizaje sobre todos los pares** del outfit, igual que G. (`Aprendizaje`)
6. **Neutros**: además de la lista de nombres, un color con saturación < 10 % (p. ej. plateado)
   cuenta como neutro, porque su matiz no significa nada. (`Colores.esNeutro`)
7. **Lluvia**: en `clima(p)`, sandalia −0.5 y calzado cerrado +0.1, recortado a [0, 1].
8. **Diversidad**: si con K = 10 quedan menos de 3 outfits distintos, se duplica K (hasta 80)
   mientras el grafo tenga más caminos. (`Recomendador.recomendar`)
9. **Backtracking**: permite omitir capas opcionales, igual que el grafo, y la cota se calcula
   para cada cantidad posible de prendas restantes, así sigue siendo una cota válida.
10. **Completar outfit**: la prenda fija se usa aunque no pase el filtro duro (el usuario la
    eligió); el menú avisa.

## Fuera de alcance

- Clima real: la temperatura y la lluvia las escribe el usuario; no se llama a ninguna API.
- Clasificación de prendas por foto y explicaciones con LLM (eran las partes de IA del plan).
- Persistencia: al salir se pierde lo agregado en la sesión; el closet de ejemplo se recarga.
- Varios usuarios, interfaz web o móvil.
