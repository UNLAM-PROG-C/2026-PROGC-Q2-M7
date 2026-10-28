# Zip - un jugador local

Primera versión jugable de [Zip de LinkedIn](https://www.linkedin.com/help/linkedin/answer/a7445030)
para la materia Programación Concurrente de la UNLaM. El objetivo es conectar
las celdas numeradas en orden y recorrer exactamente una vez todas las celdas.

## Ejecución local

Requisitos: **JDK 17 o superior** (`java`, `javac`, `jar`), Bash y un escritorio
gráfico. No hay dependencias externas de ejecución, servidor ni base de datos.

Desde `TP-Integrador`:

```bash
bash scripts/run.sh
```

Este comando compila las fuentes con `--release 17`, trata las advertencias del
compilador como errores, crea `build/app.jar` y abre el juego. Para salir se
puede cerrar la ventana o presionar Ctrl+C en la terminal.

La configuración existente de SDKMAN selecciona Java 25. Es opcional si ya hay
un JDK compatible instalado. El contenedor de desarrollo puede usarse para
editar y compilar, pero la ventana Swing debe ejecutarse desde un escritorio
gráfico y no desde un contenedor sin interfaz.

## Cómo jugar

1. Mantené seleccionado **Un jugador**. **Multijugador** abre por ahora una
   pantalla informativa de próximamente y no inicia una sesión de red.
2. Elegí **Flechas** o **W A S D**.
3. Elegí una dificultad y presioná **Jugar**:

   | Dificultad | Tablero | Puntos numerados |
   | --- | --- | --- |
   | Fácil | 5 x 5 | 5 |
   | Media | 6 x 6 | 6 |
   | Difícil | 8 x 8 | 8 |

4. Empezá en el punto 1. Cada tecla mueve una celda en horizontal o vertical.
5. Visitá las celdas numeradas en orden ascendente. No se puede atravesar un
   número futuro, salir del tablero ni repetir una celda del recorrido.
6. Alcanzá el último número después de visitar todas las celdas del tablero.

| Acción | Control |
| --- | --- |
| Mover | Flechas o W A S D, según la selección |
| Deshacer el último movimiento | Backspace o **Deshacer** |
| Reintentar el mismo tablero | **Reiniciar** |
| Volver a la configuración y crear otro tablero | **Volver** |

Los números conectados usan círculos índigo, el próximo número tiene un anillo
índigo y los futuros usan círculos oscuros. Las celdas visitadas y la línea
índigo muestran el recorrido; la celda actual usa el color de acento. Los
movimientos inválidos no alteran el recorrido y muestran un mensaje.

Deshacer también restaura el progreso de los puntos numerados. No hay detección
automática de caminos sin salida: si el recorrido se bloquea, hay que deshacer
o reiniciar. Después de ganar se deshabilitan el movimiento y deshacer. Una
capa de victoria bloqueante ofrece **Jugar de nuevo** o **Volver al menú**.

### Interfaz y accesibilidad

Las pantallas de configuración y juego comparten un tema visual dibujado con
Swing/AWT estándar, sin dependencias externas de apariencia. Las tarjetas y los
botones admiten mouse, Tab, Espacio y Enter, con un anillo de foco visible. El
movimiento y Backspace usan vinculaciones de teclas de Swing a nivel de ventana
en lugar de `KeyListener`.

La pantalla de juego muestra el estado mediante indicadores y una barra de
progreso. Las celdas numeradas distinguen visualmente los puntos conectados, el
próximo y los pendientes; la celda actual, el recorrido y las celdas visitadas
usan los mismos colores que el menú.

### Generación de niveles

La dificultad es la única fuente de verdad para el tamaño del tablero y la
cantidad de puntos numerados. Un recorrido completo visita 25, 36 o 64 celdas.
La dificultad no agrega diagonales, celdas repetidas ni un límite de movimientos.

El generador construye recorridos completos en serpiente o espiral para el
tamaño seleccionado y ubica los puntos numerados sobre ellos. Por construcción,
cada nivel tiene una solución. Las rotaciones, reflexiones e inversiones aportan
variación, pero el generador no calibra la dificultad y las soluciones no tienen
por qué ser únicas. Gana cualquier recorrido que cumpla las reglas, no solamente
el usado para construir el nivel.

## Arquitectura y concurrencia

Todo se ejecuta en **una JVM**. El motor del juego es local, pero no depende de
Swing ni de la implementación de hilos de la sesión.

| Paquete | Responsabilidad |
| --- | --- |
| [`zip.dominio`](src/zip/dominio/) | Niveles y estados inmutables, generación y reglas |
| [`zip.aplicacion`](src/zip/aplicacion/) | Contratos de sesión y fábrica, modos y actualizaciones |
| [`zip.infraestructura`](src/zip/infraestructura/) | Sesión local con ejecutor acotado y ciclo de vida |
| [`zip.interfaz`](src/zip/interfaz/) | Tema, controles, pantallas, victoria y teclado |
| [`Principal`](src/Principal.java) | Raíz de composición e inyección de la fábrica de sesiones |

```text
EDT de Swing -> acción aceptada -> cola FIFO acotada -> trabajador de sesión / motor
EDT de Swing <- invokeLater     <- estado inmutable   <- estado resultante del juego
```

- El **EDT** es dueño de los componentes Swing y de la presentación. Nunca
  espera un resultado del trabajador ni inserta de forma bloqueante en la cola.
- Un **trabajador de sesión** es dueño del motor mutable. Genera niveles y
  procesa secuencialmente movimientos, deshacer y reiniciar.
- La cola del ejecutor tiene capacidad **64**. Una sesión llena o cerrada
  rechaza el envío explícitamente y la interfaz lo informa. No descarta en
  silencio movimientos aceptados ni ejecuta el dominio en el EDT como respaldo.
- El motor está confinado a su trabajador. No hace falta un mutex adicional
  para la grilla: la coordinación surge de la cola y de publicar estados
  inmutables.
- Los rechazos del dominio aparecen como mensajes del juego. Los fallos
  inesperados se registran con su excepción y se entregan como actualizaciones
  fallidas; el trabajador puede procesar acciones posteriores.
- Volver a la configuración o cerrar la ventana cierra la sesión, cancela lo
  encolado y solicita una interrupción. Las devoluciones ya encoladas para la
  interfaz comprueban el cierre y no pueden actualizar una sesión posterior.
- Los cálculos incluidos son finitos y no realizan entrada/salida bloqueante.
  La interrupción es cooperativa: no se puede detener de forma segura y forzada
  un generador de reemplazo arbitrario que ignore interrupciones.
- `awaitTermination` existe para verificar el ciclo de vida, no para bloquear
  el EDT.

Esto demuestra comunicación, propiedad y coordinación; no afirma que usar hilos
acelere la validación pequeña de un movimiento.

### Futuro cliente-servidor

La interfaz depende de `SesionJuego`, no del trabajador local concreto. Una
sesión remota futura podrá enviar órdenes a un servidor que ejecute el mismo
motor de dominio. Una única bandera deshabilitada controla la tarjeta de
multijugador, que actualmente abre solo una pantalla informativa: no crea
sesiones, trabajadores ni sockets.

Todavía falta implementar red, serialización, estado multijugador autoritativo,
tiempos de espera y desconexiones. Esta versión **no contiene sockets, salas,
códigos ni clases de servidor simuladas**.

## Pruebas

No hace falta descargar un framework de pruebas. Las verificaciones lanzan una
excepción al fallar y no dependen de la bandera opcional `-ea` de Java.

```bash
# Compilar y probar dominio y sesión sin abrir ventanas:
bash scripts/test.sh

# Abrir ventanas Swing reales y probar ambos esquemas de teclado:
bash scripts/test.sh --ui
```

- Dominio: límites de puntos, transformaciones de rutas, estado inmutable,
  movimientos, deshacer, reiniciar, victoria y soluciones alternativas.
- Sesión: FIFO, estados anteriores inmutables, saturación de cola, devoluciones
  luego del cierre, errores explícitos, recuperación y respuesta del EDT.
- Interfaz: mouse, Espacio y Enter, navegación de modos, tres dificultades,
  ambos controles, movimientos inválidos, deshacer, reiniciar, partidas
  completas, foco bloqueante de victoria y cierre de ventana/trabajador. También
  comprueba que la pantalla multijugador no cree una sesión de juego.

La suite de interfaz necesita un escritorio y **falla**, en lugar de omitirse
silenciosamente, si el entorno no es gráfico. Escribe `build/ui-menu.png`,
`build/ui-proximamente.png`, `build/ui-jugando.png` y `build/ui-victoria.png`.
Son pruebas automatizadas de interacción, no evidencia de una partida humana.

La suite de sesión registra deliberadamente advertencias y errores para los
casos de fallo inyectado. El código de salida y los resúmenes finales determinan
el resultado real.

## Verificación de estilo

```bash
bash scripts/check.sh
```

En el primer uso se descarga con `curl` el JAR oficial de Checkstyle **10.26.1**,
se verifica su suma SHA-256 fijada y se almacena en `build/tools`, que está
ignorado. Las ejecuciones posteriores también verifican el archivo almacenado.
Se requieren `curl` y `shasum`; la red solo es necesaria para la primera
verificación de estilo.

La [configuración](config/checkstyle.xml) exige métodos cortos, constantes
numéricas con nombre, imports, llaves, nombres básicos y longitud de línea. Es
un control acotado y adaptado al formato con tabulaciones del proyecto, no una
afirmación de conformidad completa con Google Java Style.

La [configuración de pre-commit](../.pre-commit-config.yaml) ejecuta las pruebas
sin interfaz y Checkstyle cuando cambian fuentes, pruebas, scripts o
configuración del integrador. No modifica hooks globales ni verifica otros TP.
La suite gráfica sigue siendo una comprobación de escritorio explícita.

## Alcance

Implementado: un jugador local, tarjetas accesibles de modo, controles y
dificultad, niveles 5 x 5, 6 x 6 y 8 x 8, recorridos válidos,
deshacer/reiniciar, victoria, sesión en segundo plano, pantalla informativa de
multijugador y pruebas automatizadas.

No implementado: multijugador, red, ataques o efectos, GPU, pistas, ranking,
persistencia, tamaños definidos por el usuario, audio ni garantía de solución
única.

## Equipo de desarrollo

- [Farias Maira](https://github.com/maifarias)
- [Hoz Lucas](https://github.com/hozlucas28)
- [Loiero Federico](https://github.com/FNLoiero)
- [Massa Valentin](https://github.com/ValentinMassa)
- [Niedfeld Kevin](https://github.com/KevinNied)
- [Zapata Santiago](https://github.com/Santiago01011)
