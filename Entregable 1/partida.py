"""
partida.py
Junta todo: selección de modo, alta de jugadores, y el loop completo
de una partida (por consola, todavía sin GUI).

Entregable 1 - Programación Funcional - Programación Avanzada 2026
"""

import time
from typing import Callable, Dict, Tuple

from modelo import crear_jugador
from reglas import tirar_dado, siguiente_jugador
from efectos import elegir_color_al_azar
from turno import jugar_turno, hay_ganador

COLORES_DISPONIBLES = ("rojo", "azul", "verde", "amarillo")


# ---------------------------------------------------------------------------
# Entrada de datos (impura: usa input()). Separada de la lógica del juego.
# ---------------------------------------------------------------------------

def elegir_modo() -> str:
    while True:
        respuesta = input("Elegí el modo (simulacion/interactivo): ").strip().lower()
        if respuesta in ("simulacion", "interactivo"):
            return respuesta
        print("Opción inválida, escribí 'simulacion' o 'interactivo'.")


def pedir_cantidad_jugadores() -> int:
    while True:
        respuesta = input("¿Cuántos jugadores van a jugar (2-4)? ").strip()
        if respuesta.isdigit() and 2 <= int(respuesta) <= 4:
            return int(respuesta)
        print("Tiene que ser un número entre 2 y 4.")


def crear_jugadores_interactivo(cantidad: int) -> Tuple[Dict, ...]:
    jugadores = []
    for i in range(cantidad):
        color = COLORES_DISPONIBLES[i]
        nombre = input(f"Nombre del jugador {i + 1} ({color}): ").strip() or f"Jugador {i + 1}"
        jugadores.append(crear_jugador(nombre, color))
    return tuple(jugadores)


def crear_jugadores_simulacion(cantidad: int) -> Tuple[Dict, ...]:
    return tuple(
        crear_jugador(f"CPU {i + 1}", COLORES_DISPONIBLES[i])
        for i in range(cantidad)
    )


def pedir_color_castigado_interactivo(jugadores: Tuple[Dict, ...], color_propio: str) -> str:
    """Versión interactiva de ElegirColorCastigado: le pregunta al jugador de turno."""
    oponentes = [j["color"] for j in jugadores if j["color"] != color_propio]
    print(f"Casilla P1: elegí a qué color hacer perder un turno ({', '.join(oponentes)})")
    while True:
        respuesta = input("Color: ").strip().lower()
        if respuesta in oponentes:
            return respuesta
        print("Ese color no es válido (o es el tuyo propio).")


def tirar_dado_interactivo() -> int:
    """Versión interactiva del dado: pide presionar una tecla antes de tirar."""
    input("Presioná ENTER para tirar el dado...")
    valor = tirar_dado()
    print(f"    Salió {valor}")
    return valor


# ---------------------------------------------------------------------------
# Loop de la partida (usa siguiente_jugador para el orden y los saltos)
# ---------------------------------------------------------------------------

def jugar_partida(
    jugadores_iniciales: Tuple[Dict, ...],
    elegir_color_castigado: Callable[[Tuple[Dict, ...], str], str],
    tirar_dado_fn: Callable[[], int] = tirar_dado,
    pausa_segundos: float = 0.0,
) -> Dict:
    """
    Juega una partida completa hasta que alguien llega a FIN.
    Devuelve el jugador ganador.
    """
    jugadores = jugadores_iniciales
    indice_actual = -1  # siguiente_jugador(..., -1) arranca en el jugador 0

    while hay_ganador(jugadores) is None:
        jugadores, indice_actual = siguiente_jugador(jugadores, indice_actual)
        jugador_actual = jugadores[indice_actual]
        _, jugadores = jugar_turno(jugador_actual, jugadores, elegir_color_castigado, tirar_dado_fn)
        if pausa_segundos:
            time.sleep(pausa_segundos)

    return hay_ganador(jugadores)


def main():
    modo = elegir_modo()
    cantidad = pedir_cantidad_jugadores()

    if modo == "interactivo":
        jugadores = crear_jugadores_interactivo(cantidad)
        elegir_color_castigado = pedir_color_castigado_interactivo
        dado_fn = tirar_dado_interactivo
        pausa = 0.0
    else:
        jugadores = crear_jugadores_simulacion(cantidad)
        elegir_color_castigado = elegir_color_al_azar
        dado_fn = tirar_dado
        pausa = 1.0

    print("\nJugadores:")
    for j in jugadores:
        print(f"  - {j['nombre']} ({j['color']})")
    print()

    ganador = jugar_partida(jugadores, elegir_color_castigado, dado_fn, pausa)
    print(f"\n¡Felicitaciones {ganador['nombre']}, ganaste!")


if __name__ == "__main__":
    main()