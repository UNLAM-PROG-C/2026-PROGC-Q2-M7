# Zip - un jugador local

Primera versión jugable de [Zip de LinkedIn](https://www.linkedin.com/help/linkedin/answer/a7445030)
para la materia Programación Concurrente de la UNLaM. El objetivo es conectar
las celdas numeradas en orden y recorrer cada celda exactamente una vez.

## Ejecución local

Requisitos: **JDK 17 o posterior** (`java`, `javac`, `jar`), Bash y un escritorio
gráfico. No se necesitan dependencias de ejecución externas, servidor ni base
de datos.

Desde `TP-Integrador`:

```bash
bash scripts/run.sh
```

Este comando compila las fuentes con `--release 17`, trata las advertencias del
compilador como errores, crea `build/app.jar` y abre el juego. Cerrá la ventana
para salir; Ctrl+C en la terminal también detiene la aplicación.

La configuración existente de SDKMAN selecciona Java 25. Es opcional si ya hay
instalado un JDK compatible. El contenedor de desarrollo sirve para editar y
compilar, pero la ventana Swing debe ejecutarse desde un escritorio gráfico, no
desde un contenedor sin interfaz.

## Cómo jugar

1. Elegí **Flechas** o **WASD**.
2. Elegí entre **2 y 36 puntos numerados** y presioná **Jugar**.
3. Comenzá en el punto 1 y movete una celda horizontal o vertical por pulsación.
4. Visitá los números en orden ascendente. No podés atravesar un número futuro,
   salir del tablero ni repetir una celda del recorrido.
5. Alcanzá el último número solamente después de visitar las **36 celdas**.

| Acción | Control |
| --- | --- |
| Mover | Flechas elegidas o WASD |
| Deshacer el último movimiento | Backspace o **Deshacer** |
| Reintentar el mismo tablero | **Reiniciar** |
| Volver a la configuración y crear otro tablero | **Volver** |

La celda verde marca el inicio, la violeta el número final, la línea azul el
recorrido y el anillo azul la posición actual. La pantalla muestra las celdas
visitadas y el próximo número requerido. Los movimientos inválidos no alteran
el recorrido y muestran un mensaje.

Deshacer también restaura el progreso de los puntos numerados. No hay detección
automática de caminos sin salida: deshacé o reiniciá si te trabás. Después de
ganar, el movimiento y la acción de deshacer quedan deshabilitados; reiniciá o
volvé a la configuración para jugar otra vez.

### Generación de niveles

El tablero siempre es de **6 x 6**. El selector cambia la cantidad de puntos
numerados, no el tamaño del tablero ni un límite de movimientos. Un recorrido
completo tiene 36 celdas visitadas y 35 movimientos; más puntos numerados no
implican necesariamente una dificultad mayor.

El generador transforma recorridos completos con forma de serpiente o espiral y
ubica sobre ellos los puntos solicitados. Por eso, cada nivel tiene una solución
por construcción. Las rotaciones, reflexiones e inversiones aportan variedad,
pero el generador no calibra la dificultad ni garantiza una solución única.
Cualquier recorrido que respete las reglas permite ganar, no solo el utilizado
durante la construcción.

## Arquitectura y concurrencia

Todo se ejecuta en **una JVM**. El motor es local, pero no depende de Swing ni
de la implementación de hilos de la sesión.

| Paquete | Responsabilidad |
| --- | --- |
| [`zip.dominio`](src/zip/dominio/) | Niveles y estados inmutables, generación y reglas |
| [`zip.aplicacion`](src/zip/aplicacion/) | Contratos de sesión, fábrica y mensajes de actualización |
| [`zip.infraestructura`](src/zip/infraestructura/) | Sesión local con ejecutor acotado y ciclo de vida |
| [`zip.interfaz`](src/zip/interfaz/) | Configuración, dibujo y atajos de teclado de la ventana |
| [`Principal`](src/Principal.java) | Raíz de composición que crea e inyecta la sesión en la interfaz |

```text
EDT de Swing -> acción aceptada -> cola FIFO acotada -> trabajador de sesión / motor
EDT de Swing <- invokeLater     <- estado inmutable   <- estado resultante del juego
```

- El **EDT** es dueño de los componentes Swing y la presentación. Nunca espera
  un resultado del trabajador ni inserta de forma bloqueante en la cola.
- Un único **trabajador de sesión** es dueño del motor mutable. Genera niveles
  y procesa movimiento, deshacer y reinicio en forma secuencial.
- La cola del ejecutor tiene capacidad **64**. Una sesión cerrada o con la cola
  llena rechaza la acción explícitamente y la interfaz lo informa. Nunca
  descarta silenciosamente un movimiento aceptado ni ejecuta dominio en el EDT
  como alternativa.
- El motor está confinado a su trabajador. No se necesita otro mutex para la
  grilla: la cola coordina el acceso y los estados publicados son inmutables.
- Los rechazos de dominio producen mensajes visibles. Los fallos inesperados se
  registran con su excepción y se entregan como actualizaciones fallidas; el
  trabajador puede procesar acciones posteriores.
- Volver a la configuración o cerrar la ventana cierra la sesión, cancela las
  tareas encoladas y solicita una interrupción. Las devoluciones ya enviadas al
  EDT verifican el cierre y no pueden actualizar una sesión posterior.
- Los cálculos incluidos son finitos y no hacen entrada/salida bloqueante. La
  interrupción es cooperativa; no es seguro detener por la fuerza un generador
  de reemplazo que ignore interrupciones.
- `esperarFinalizacion` existe para verificar el ciclo de vida, no para bloquear
  el EDT.

Esto demuestra comunicación, propiedad y coordinación; no afirma que los hilos
aceleren una validación de movimiento pequeña.

### Futura iteración cliente-servidor

La interfaz depende de `SesionJuego`, no del trabajador local concreto. Una
sesión remota futura podrá enviar comandos a un servidor que ejecute el mismo
motor de dominio. Todavía faltan red, serialización, estado multijugador
autoritativo, tiempos máximos y manejo de desconexiones. Esta versión no
contiene sockets ni clases de servidor ficticias.

## Pruebas

No se descarga ningún framework de pruebas. Las verificaciones lanzan una
excepción ante fallos y no dependen del indicador opcional `-ea` de Java.

```bash
# Compilar y probar dominio y sesión sin abrir ventanas:
bash scripts/test.sh

# Además, abrir ventanas Swing reales y probar ambos esquemas de teclado:
bash scripts/test.sh --ui
```

- Dominio: límites de puntos, transformaciones, estado inmutable, reglas de
  movimiento, deshacer, reinicio, victoria y soluciones alternativas válidas.
- Sesión: FIFO, estados anteriores inmutables, saturación de cola, devoluciones
  de sesiones cerradas, errores explícitos con recuperación y respuesta del EDT
  mientras el trabajador está bloqueado.
- Interfaz: ambos controles, eventos enviados al componente enfocado,
  movimientos inválidos, deshacer, reinicio, partidas completas, victoria y
  cierre de ventana y trabajador.

La suite de interfaz requiere un escritorio y falla, en lugar de omitirse en
silencio, si se ejecuta sin entorno gráfico. Escribe `build/ui-jugando.png` y
`build/ui-victoria.png`. Son pruebas automáticas de interacción, no evidencia
de una partida manual. La suite de sesión registra advertencias y errores
esperados para fallos inyectados; el código de salida y los resúmenes finales
determinan el resultado.

## Verificación de estilo

```bash
bash scripts/check.sh
```

El primer uso descarga el JAR oficial de Checkstyle **10.26.1** mediante `curl`,
verifica su suma SHA-256 fijada y lo guarda en `build/tools`, que está ignorado.
Las ejecuciones posteriores también verifican la suma guardada. Se requieren
`curl` y `shasum`; la red solo es necesaria en la primera ejecución.

La [configuración](config/checkstyle.xml) exige métodos breves, constantes con
nombre, importaciones ordenadas, llaves, convenciones básicas de nombres y
límites de línea. Es una verificación enfocada en el estilo de la materia y
adaptada al formato existente con tabulaciones, no una implementación completa
del estilo de Google para Java.

La [configuración de pre-commit](../.pre-commit-config.yaml) ejecuta las pruebas
sin interfaz y Checkstyle cuando cambian las fuentes, pruebas, scripts o su
configuración. No modifica los hooks globales ni revisa otros trabajos
prácticos. La suite gráfica sigue siendo una verificación explícita de
escritorio.

## Alcance

Implementado: un jugador local, controles y puntos configurables, niveles
válidos, deshacer, reiniciar, victoria, sesión en segundo plano y pruebas
automáticas.

No implementado: multijugador, red, ataques o efectos, GPU, pistas, ranking,
persistencia, tamaños arbitrarios, audio ni garantía de solución única.

## Equipo de desarrollo

- [Farias Maira](https://github.com/maifarias)
- [Hoz Lucas](https://github.com/hozlucas28)
- [Loiero Federico](https://github.com/FNLoiero)
- [Massa Valentin](https://github.com/ValentinMassa)
- [Niedfeld Kevin](https://github.com/KevinNied)
- [Zapata Santiago](https://github.com/Santiago01011)
