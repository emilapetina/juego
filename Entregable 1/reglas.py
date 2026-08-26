"""
reglas.py
Funciones puras (y las pocas impuras necesarias) para mover jugadores
y manejar el orden de turnos. Los jugadores son diccionarios; nunca se
mutan in-place, siempre se devuelve un diccionario nuevo.

Entregable 1 - Programación Funcional - Programación Avanzada 2026
"""

import random
from typing import Dict, Tuple

from modelo import TABLERO, TOTAL_CASILLAS


# ---------------------------------------------------------------------------
# Dado (impuro: depende de random, no se puede evitar)
# ---------------------------------------------------------------------------

def tirar_dado() -> int:
    """Tira un dado de 6 caras. Impura a propósito: un dado real no es puro."""
    return random.randint(1, 6)


# ---------------------------------------------------------------------------
# Movimiento (puro)
# ---------------------------------------------------------------------------

def avanzar_jugador(jugador: Dict, pasos: int) -> Dict:
    """
    Devuelve un NUEVO diccionario-jugador con la posición avanzada `pasos`
    casillas. Regla de "no hay rebote": si el movimiento supera la casilla
    FIN, el jugador llega igual a FIN (no se pasa ni rebota).
    No muta el diccionario original.
    """
    nueva_posicion = min(jugador["posicion"] + pasos, TOTAL_CASILLAS - 1)
    return {**jugador, "posicion": nueva_posicion}


def retroceder_jugador(jugador: Dict, pasos: int) -> Dict:
    """
    Devuelve un NUEVO diccionario-jugador con la posición retrocedida
    `pasos` casillas. No puede retroceder antes de INICIO (índice 0).
    """
    nueva_posicion = max(jugador["posicion"] - pasos, 0)
    return {**jugador, "posicion": nueva_posicion}


def obtener_casilla(indice: int) -> Dict:
    """Devuelve el diccionario-casilla del tablero en el índice dado (lookup puro)."""
    return TABLERO[indice]

def reemplazar_jugador(jugadores: Tuple[Dict, ...], nuevo: Dict) -> Tuple[Dict, ...]:
    """Devuelve una NUEVA tupla de jugadores con `nuevo` reemplazando al de su mismo color."""
    return tuple(nuevo if j["color"] == nuevo["color"] else j for j in jugadores)

# ---------------------------------------------------------------------------
# Orden de turnos (recursivo, puro)
# ---------------------------------------------------------------------------

def siguiente_jugador(
    jugadores: Tuple[Dict, ...], indice_actual: int
) -> Tuple[Tuple[Dict, ...], int]:
    """
    Calcula quién juega el próximo turno, en orden rotativo.
    Si el próximo jugador tiene pierde_turno=True, se lo saltea:
    se le resetea el flag (en una copia nueva de la tupla, sin mutar
    nada) y se vuelve a llamar recursivamente para el que sigue.

    Devuelve (jugadores_actualizados, indice_del_proximo_que_juega).
    """
    siguiente_indice = (indice_actual + 1) % len(jugadores)
    jugador = jugadores[siguiente_indice]

    if jugador["pierde_turno"]:
        jugador_actualizado = {**jugador, "pierde_turno": False}
        jugadores_actualizados = (
            jugadores[:siguiente_indice]
            + (jugador_actualizado,)
            + jugadores[siguiente_indice + 1:]
        )
        return siguiente_jugador(jugadores_actualizados, siguiente_indice)

    return jugadores, siguiente_indice


if __name__ == "__main__":
    # Prueba rápida por consola, sin GUI todavía.
    from modelo import crear_jugador

    jugadores_demo = (
        crear_jugador(nombre="Ana", color="rojo", posicion=0),
        crear_jugador(nombre="Beto", color="azul", posicion=0, pierde_turno=True),
    )

    dado = tirar_dado()
    print(f"Dado: {dado}")

    ana = avanzar_jugador(jugadores_demo[0], dado)
    print(f"Ana avanza a la casilla {ana['posicion']}: {obtener_casilla(ana['posicion'])}")

    jugadores_actualizados, indice_proximo = siguiente_jugador(jugadores_demo, 0)
    print(f"Próximo en jugar: índice {indice_proximo} -> {jugadores_actualizados[indice_proximo]['nombre']}")
    print(f"Estado de Beto luego de saltear: {jugadores_actualizados[1]}")