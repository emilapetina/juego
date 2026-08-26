"""
gui.py
Interfaz gráfica en Tkinter. Dibuja el tablero y las fichas, pide modo
y jugadores, y conecta los botones con la lógica del juego que ya
teníamos (turno.py, efectos.py, competencia.py, reglas.py) SIN
modificar nada de esa lógica: solo se reemplazan los `input()` de
partida.py por widgets.

Nota de diseño: `estado_app` es un diccionario mutable que actúa como
"celda de referencia" -- Tkinter necesita algún lugar donde guardar
"cuál es el estado actual" entre un evento (click, timer) y el
siguiente, porque cada callback se ejecuta por separado. Lo único que
se muta es la referencia guardada ahí adentro; los valores en sí
(las tuplas de jugadores) siguen siendo inmutables y se siguen
generando con las mismas funciones puras de siempre.

Entregable 1 - Programación Funcional - Programación Avanzada 2026
"""

import math
from collections import defaultdict

import tkinter as tk

from modelo import TABLERO, FILAS, COLUMNAS, crear_jugador
from reglas import tirar_dado, siguiente_jugador
from efectos import elegir_color_al_azar
from turno import jugar_turno, hay_ganador

CELL_SIZE = 55
MARGIN = 20

COLORES_POR_TIPO = {
    "INICIO": "#8BC34A",
    "FIN": "#4FC3F7",
    "P1": "#FFB74D",
    "P2": "#FFB74D",
    "P3": "#FFB74D",
    "C1": "#E57373",
    "C2": "#E57373",
    "NORMAL": "#FFFFFF",
}
COLOR_INTERIOR = "#BDBDBD"

COLORES_DISPONIBLES = ("rojo", "azul", "verde", "amarillo")
COLORES_HEX = {"rojo": "#E53935", "azul": "#1E88E5", "verde": "#43A047", "amarillo": "#FDD835"}


# ---------------------------------------------------------------------------
# Dibujo (funciones puras de cálculo + funciones de dibujo sobre el canvas)
# ---------------------------------------------------------------------------

def casilla_a_pixel(fila: int, columna: int):
    """Convierte (fila, columna) en base 1 a un rectángulo de píxeles. Pura."""
    x1 = MARGIN + (columna - 1) * CELL_SIZE
    y1 = MARGIN + (fila - 1) * CELL_SIZE
    return x1, y1, x1 + CELL_SIZE, y1 + CELL_SIZE


def centro_con_offset(indice_casilla: int, posicion_en_grupo: int, tamanio_grupo: int):
    """
    Centro (x, y) de una casilla, desplazado en círculo si hay más de
    una ficha compartiendo la casilla, para que no se tapen. Pura.
    """
    casilla = TABLERO[indice_casilla]
    x1, y1, x2, y2 = casilla_a_pixel(casilla["fila"], casilla["columna"])
    cx, cy = (x1 + x2) / 2, (y1 + y2) / 2
    if tamanio_grupo <= 1:
        return cx, cy
    angulo = 2 * math.pi * posicion_en_grupo / tamanio_grupo
    radio = CELL_SIZE * 0.22
    return cx + radio * math.cos(angulo), cy + radio * math.sin(angulo)


def dibujar_tablero(canvas: tk.Canvas) -> None:
    for fila in range(1, FILAS + 1):
        for columna in range(1, COLUMNAS + 1):
            x1, y1, x2, y2 = casilla_a_pixel(fila, columna)
            canvas.create_rectangle(x1, y1, x2, y2, fill=COLOR_INTERIOR, outline="#9E9E9E")

    for casilla in TABLERO:
        x1, y1, x2, y2 = casilla_a_pixel(casilla["fila"], casilla["columna"])
        color = COLORES_POR_TIPO[casilla["tipo"]]
        canvas.create_rectangle(x1, y1, x2, y2, fill=color, outline="black", width=2)
        etiqueta = casilla["tipo"] if casilla["tipo"] != "NORMAL" else str(casilla["indice"])
        canvas.create_text((x1 + x2) // 2, (y1 + y2) // 2, text=etiqueta, font=("Arial", 9, "bold"))


def dibujar_fichas(canvas: tk.Canvas, jugadores) -> None:
    """Borra las fichas anteriores y redibuja una por jugador en su casilla actual."""
    canvas.delete("ficha")
    agrupadas = defaultdict(list)
    for jugador in jugadores:
        agrupadas[jugador["posicion"]].append(jugador)

    for posicion, grupo in agrupadas.items():
        for i, jugador in enumerate(grupo):
            x, y = centro_con_offset(posicion, i, len(grupo))
            r = CELL_SIZE * 0.26
            canvas.create_oval(
                x - r, y - r, x + r, y + r,
                fill=COLORES_HEX[jugador["color"]], outline="black", width=2, tags="ficha",
            )


# ---------------------------------------------------------------------------
# Diálogo modal para elegir a quién castigar en P1 (modo interactivo)
# ---------------------------------------------------------------------------

def crear_elegir_color_gui(ventana: tk.Tk):
    """
    Devuelve una función con la misma firma que ElegirColorCastigado
    (jugadores, color_propio) -> color, implementada con un diálogo
    modal de Tkinter en vez de input(). No cambia nada de efectos.py.
    """
    def elegir(jugadores, color_propio):
        oponentes = [j["color"] for j in jugadores if j["color"] != color_propio]
        resultado = {"color": oponentes[0]}

        dialogo = tk.Toplevel(ventana)
        dialogo.title("Casilla P1")
        dialogo.grab_set()
        tk.Label(dialogo, text="Elegí a qué color hacer perder un turno:", padx=10, pady=10).pack()

        marco = tk.Frame(dialogo)
        marco.pack(padx=10, pady=10)

        def elegir_y_cerrar(color):
            resultado["color"] = color
            dialogo.destroy()

        for color in oponentes:
            tk.Button(
                marco, text=color, bg=COLORES_HEX[color], width=10,
                command=lambda c=color: elegir_y_cerrar(c),
            ).pack(side="left", padx=5)

        dialogo.wait_window()
        return resultado["color"]

    return elegir


# ---------------------------------------------------------------------------
# Un paso de turno (reutiliza siguiente_jugador + jugar_turno tal cual)
# ---------------------------------------------------------------------------

def ejecutar_un_turno(estado_app, elegir_color_castigado, dado_fn=tirar_dado):
    jugadores, indice_actual = siguiente_jugador(estado_app["jugadores"], estado_app["indice_actual"])
    jugador_actual = jugadores[indice_actual]
    jugador_final, jugadores = jugar_turno(jugador_actual, jugadores, elegir_color_castigado, dado_fn)
    estado_app["jugadores"] = jugadores
    estado_app["indice_actual"] = indice_actual
    return jugador_actual, jugador_final


# ---------------------------------------------------------------------------
# App: asistente de configuración + pantalla de juego
# ---------------------------------------------------------------------------

def iniciar_app() -> tk.Tk:
    ventana = tk.Tk()
    ventana.title("Juego de tablero - Programación Funcional")

    contenedor = tk.Frame(ventana, padx=10, pady=10)
    contenedor.pack()

    estado_app = {"jugadores": (), "indice_actual": -1, "terminado": False, "modo": None}

    def limpiar():
        for widget in contenedor.winfo_children():
            widget.destroy()

    # --- Paso 1: modo -----------------------------------------------------
    def paso_modo():
        limpiar()
        tk.Label(contenedor, text="¿Cómo querés jugar?", font=("Arial", 14)).pack(pady=10)
        tk.Button(contenedor, text="Modo Simulación", width=25,
                  command=lambda: paso_cantidad("simulacion")).pack(pady=5)
        tk.Button(contenedor, text="Modo Interactivo", width=25,
                  command=lambda: paso_cantidad("interactivo")).pack(pady=5)

    # --- Paso 2: cantidad de jugadores ------------------------------------
    def paso_cantidad(modo):
        limpiar()
        estado_app["modo"] = modo
        tk.Label(contenedor, text="¿Cuántos jugadores? (2 a 4)", font=("Arial", 14)).pack(pady=10)
        marco = tk.Frame(contenedor)
        marco.pack()
        for cantidad in (2, 3, 4):
            tk.Button(marco, text=str(cantidad), width=6,
                      command=lambda c=cantidad: paso_siguiente(modo, c)).pack(side="left", padx=5)

    def paso_siguiente(modo, cantidad):
        if modo == "interactivo":
            paso_nombres(cantidad)
        else:
            jugadores = tuple(
                crear_jugador(f"CPU {i + 1}", COLORES_DISPONIBLES[i]) for i in range(cantidad)
            )
            comenzar(jugadores)

    # --- Paso 3 (solo interactivo): nombres -------------------------------
    def paso_nombres(cantidad):
        limpiar()
        tk.Label(contenedor, text="Nombres de los jugadores", font=("Arial", 14)).pack(pady=10)
        entradas = []
        for i in range(cantidad):
            color = COLORES_DISPONIBLES[i]
            fila = tk.Frame(contenedor)
            fila.pack(fill="x", pady=2)
            tk.Label(fila, text=color, width=10, fg=COLORES_HEX[color], anchor="w").pack(side="left")
            entrada = tk.Entry(fila)
            entrada.pack(side="left")
            entradas.append((color, entrada))

        def confirmar():
            jugadores = tuple(
                crear_jugador(entrada.get().strip() or f"Jugador {i + 1}", color)
                for i, (color, entrada) in enumerate(entradas)
            )
            comenzar(jugadores)

        tk.Button(contenedor, text="Comenzar", command=confirmar).pack(pady=10)

    # --- Pantalla de juego --------------------------------------------------
    def comenzar(jugadores):
        estado_app["jugadores"] = jugadores
        estado_app["indice_actual"] = -1
        estado_app["terminado"] = False
        limpiar()

        ancho = MARGIN * 2 + COLUMNAS * CELL_SIZE
        alto = MARGIN * 2 + FILAS * CELL_SIZE
        canvas = tk.Canvas(contenedor, width=ancho, height=alto, bg="white")
        canvas.pack()
        dibujar_tablero(canvas)
        dibujar_fichas(canvas, jugadores)

        etiqueta_estado = tk.Label(contenedor, text="¡Arrancamos!", font=("Arial", 12), pady=10)
        etiqueta_estado.pack()

        elegir_color_gui = crear_elegir_color_gui(ventana)

        def refrescar_vista(jugador_inicial, jugador_final):
            dibujar_fichas(canvas, estado_app["jugadores"])
            casilla = TABLERO[jugador_final["posicion"]]
            etiqueta_estado.config(
                text=f"{jugador_final['nombre']} ({jugador_final['color']}): "
                     f"casilla {jugador_inicial['posicion']} -> {jugador_final['posicion']} [{casilla['tipo']}]"
            )

        def terminar(ganador):
            estado_app["terminado"] = True
            etiqueta_estado.config(text=f"¡Ganó {ganador['nombre']}!")

            dialogo = tk.Toplevel(ventana)
            dialogo.title("Fin del juego")
            dialogo.grab_set()
            tk.Label(
                dialogo,
                text=f"¡Felicitaciones {ganador['nombre']}, ganaste!",
                padx=20, pady=15, font=("Arial", 12),
            ).pack()

            marco_botones = tk.Frame(dialogo, pady=10)
            marco_botones.pack()

            def volver_a_jugar():
                dialogo.destroy()
                paso_modo()

            def salir():
                ventana.destroy()

            tk.Button(marco_botones, text="Volver a jugar", width=14, command=volver_a_jugar).pack(side="left", padx=5)
            tk.Button(marco_botones, text="Salir", width=14, command=salir).pack(side="left", padx=5)

        if estado_app["modo"] == "interactivo":
            def al_tirar_dado(event=None):
                if estado_app["terminado"]:
                    return
                jugador_inicial, jugador_final = ejecutar_un_turno(estado_app, elegir_color_gui, tirar_dado)
                refrescar_vista(jugador_inicial, jugador_final)
                ganador = hay_ganador(estado_app["jugadores"])
                if ganador:
                    terminar(ganador)

            tk.Button(contenedor, text="Tirar dado (o presioná ESPACIO)", command=al_tirar_dado).pack(pady=5)
            ventana.bind("<space>", al_tirar_dado)
        else:
            def paso_automatico():
                if estado_app["terminado"]:
                    return
                jugador_inicial, jugador_final = ejecutar_un_turno(estado_app, elegir_color_al_azar, tirar_dado)
                refrescar_vista(jugador_inicial, jugador_final)
                ganador = hay_ganador(estado_app["jugadores"])
                if ganador:
                    terminar(ganador)
                else:
                    ventana.after(900, paso_automatico)

            ventana.after(500, paso_automatico)

    paso_modo()
    return ventana


if __name__ == "__main__":
    iniciar_app().mainloop()