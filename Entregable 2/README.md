# Entregable 2 - Concurrencia (Simulación de restaurante)

Simulación concurrente en Java de un restaurante: clientes, mozos, cocineros
y cajeros, usando las herramientas de concurrencia vistas en clase.

## Compilar y ejecutar

Requiere Java 17 o superior. Desde esta carpeta:

```bash
javac -d out src/*.java
cd out
java Main        # T = 30 segundos (Config.T)
java Main 60     # T = 60 segundos
```

Cada cambio se muestra en pantalla con la foto completa del restaurante y se
guarda en `restaurante.log` (en la carpeta desde donde se ejecuta).

## Contenido

- `src/` código fuente (constantes en `Config.java`, menús en `Menu.java`).
- `Documentacion.pdf` informe técnico (fuente en `docs/Documentacion.html`).
- `ejemplo_restaurante.log` log de una corrida completa con la configuración por defecto.
