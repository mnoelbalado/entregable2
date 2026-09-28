# Entregable 2 - Restaurante concurrente
### Maria Noel Balado y Julieta Romero

Requiere **JDK 21** y Maven. En IntelliJ, configurar tanto el SDK del proyecto
como el JDK de Maven en 21. El `java` del PATH puede corresponder a otra versión.

## Compilar, probar y ejecutar

```text
mvn test
java -cp target/classes restaurante.Main
```

También se puede ejecutar `restaurante.Main` directamente desde IntelliJ.
La consola muestra cada evento y una copia del estado completo. El archivo
predeterminado es `output/simulacion.log`; se sobrescribe al iniciar otra ejecución.
Para elegir otra ruta:

```text
java -cp target/classes restaurante.Main output/otra-simulacion.log
```

`Configuracion.java` contiene las constantes, en milisegundos, y la carta con
rangos de cocción diferentes. `T = 60000` cierra la admisión a los 60 segundos;
la atención de pedidos aceptados, el cobro y la limpieza pueden continuar después.
Las pruebas usan `Configuracion.Parametros` para ejecutar escenarios rápidos
sin cambiar las constantes de la simulación normal.

## Organización y decisiones

Los paquetes del proyecto son: actores, config, coordinacion, display, mensajes, 
modelo y util.

- Un pedido queda aceptado cuando el mozo termina de anotarlo, antes de enviarlo
  a cocina. Aceptación y cierre comparten monitor. Si el cierre gana, se cancela.
- Una mesa se reutiliza únicamente después de que salgan todos y termine la limpieza.
- La cocina asigna primero todos los platos pendientes del pedido más antiguo.
  No necesita esperar su cocción completa para asignar platos del siguiente.
- Se sirve el pedido completo antes de liberar a los clientes para comer.
- Hay una única cola FIFO de cobro; varios cajeros pueden cobrar simultáneamente.
- Se interpreta `TXmax` en la descripción del mozo como `TZmax`.

Herramientas obligatorias: `synchronized`, `AtomicReference.compareAndSet`,
`CountDownLatch`, `LinkedBlockingQueue`, pools de `Executors` y `wait/notifyAll`.

Patrón concurrente: productor-consumidor. Clientes y cocineros producen tareas
para los mozos; los clientes producen cobros y todos los roles producen eventos
para un único display. Las colas desacoplan a los productores de los consumidores.

## Para generar resumenes de eventos
```text
Get-Content output/simulacion.log |
Where-Object { $_ -match '^\[\d+\]' } |
Set-Content output/eventos-resumen.log
```

## Validación

JUnit 5: 20 ejecuciones de pruebas, incluidas cinco repeticiones de la simulación
completa. Cubren aforo, grupos incompletos, limpieza antes de reutilizar, cierre,
interrupción de espera, orden en cocina, no duplicación de platos y avisos,
entrega completa, cobro, FIFO de caja, snapshots independientes, validaciones y
fallo de escritura del log. Los reportes están en `target/surefire-reports`.
