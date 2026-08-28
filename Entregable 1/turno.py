"""
turno.py
Ejecuta un turno completo de un jugador: tira el dado, mueve, y resuelve
la llegada (competencia + premios/castigos vía resolver_llegada). Acá
vive el decorador de log y el uso de map/reduce.

Entregable 1 - Programación Funcional - Programación Avanzada 2026
"""

import functools
from typing import Callable, Dict, Optional, Tuple

from modelo import TOTAL_CASILLAS
from reglas import tirar_dado, avanzar_jugador, obtener_casilla, reemplazar_jugador
from efectos import ElegirColorCastigado
from competencia import resolver_llegada


def log_turno(func):
    """
    Decorador que loguea cada turno jugado: quién jugó, en qué casilla
    empezó, qué salió en el dado, qué premios/castigos se activaron y
    en qué casilla terminó. No modifica el comportamiento de la
    función que decora, solo agrega la traza.
    """
    @functools.wraps(func)
    def envoltorio(jugador, jugadores, *args, **kwargs):
        print(f"--- Turno de {jugador['nombre']} ({jugador['color']}), arranca en casilla {jugador['posicion']} ---")
        jugador_final, jugadores_actualizados, dado, eventos = func(jugador, jugadores, *args, **kwargs)
        print(f"    Salió {dado} en el dado.")
        for evento in eventos:
            print(f"    {evento}")
        casilla_final = obtener_casilla(jugador_final["posicion"])
        print(f"    {jugador['nombre']} termina en casilla {jugador_final['posicion']} ({casilla_final['tipo']})")
        return jugador_final, jugadores_actualizados, dado, eventos
    return envoltorio


@log_turno
def jugar_turno(
    jugador: Dict,
    jugadores: Tuple[Dict, ...],
    elegir_color_castigado: ElegirColorCastigado,
    tirar_dado_fn: Callable[[], int] = tirar_dado,
) -> Tuple[Dict, Tuple[Dict, ...], int, Tuple[str, ...]]:
    """
    Juega un turno completo para `jugador` (se asume que ya le toca a
    él y que no tiene pierde_turno activo): tira el dado, avanza, y
    resuelve todo lo que implica llegar a esa casilla (competencia +
    premio/castigo, encadenado si corresponde). Devuelve
    (jugador_final, jugadores_actualizados, dado, eventos), donde
    `eventos` es una tupla de mensajes describiendo lo que pasó.
    """
    dado = tirar_dado_fn()
    jugador_movido = avanzar_jugador(jugador, dado)
    jugadores_actualizados = reemplazar_jugador(jugadores, jugador_movido)
    jugador_final, jugadores_finales, eventos = resolver_llegada(
        jugador_movido, jugadores_actualizados, elegir_color_castigado, tirar_dado_fn
    )
    return jugador_final, jugadores_finales, dado, eventos


def hay_ganador(jugadores: Tuple[Dict, ...]) -> Optional[Dict]:
    """
    Devuelve el jugador que llegó a FIN, si existe. Usa `reduce` para
    encontrar quién tiene la mayor posición entre todos, y chequea si
    esa posición es la casilla FIN. Devuelve None si nadie ganó aún.
    """
    lider = functools.reduce(
        lambda mejor, actual: actual if actual["posicion"] > mejor["posicion"] else mejor,
        jugadores,
    )
    return lider if lider["posicion"] == TOTAL_CASILLAS - 1 else None


def posiciones_actuales(jugadores: Tuple[Dict, ...]) -> Tuple[int, ...]:
    """Devuelve las posiciones de todos los jugadores, en el mismo orden. Usa `map`."""
    return tuple(map(lambda j: j["posicion"], jugadores))


if __name__ == "__main__":
    import itertools
    from modelo import crear_jugador
    from efectos import elegir_color_al_azar

    # Mini-partida de prueba con dados fijos (deterministas) para ver
    # el log y confirmar que hay_ganador/posiciones_actuales funcionan.
    dados_fijos = itertools.cycle([6, 6, 6, 6, 6, 6, 6, 6, 6])  # Ana avanza rápido
    dados_beto = itertools.cycle([1])

    jugadores = (
        crear_jugador("Ana", "rojo"),
        crear_jugador("Beto", "azul"),
    )

    turno_de = 0
    while hay_ganador(jugadores) is None:
        jugador_actual = jugadores[turno_de]
        dado_fn = (lambda: next(dados_fijos)) if jugador_actual["color"] == "rojo" else (lambda: next(dados_beto))
        _, jugadores, _, _ = jugar_turno(jugador_actual, jugadores, elegir_color_al_azar, dado_fn)
        print(f"    Posiciones actuales: {posiciones_actuales(jugadores)}")
        turno_de = (turno_de + 1) % len(jugadores)

    ganador = hay_ganador(jugadores)
    print(f"\n¡Ganó {ganador['nombre']}!")