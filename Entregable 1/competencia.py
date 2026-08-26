"""
competencia.py
Resuelve qué pasa cuando un jugador cae en una casilla ocupada por otro
(regla 4 de la letra): compiten tirando el dado, el que saca menos
retrocede 2 casilleros (y 1 más si cae en otra casilla ocupada).

También junta todo en `resolver_llegada`, que es la función que se va
a llamar después de cada movimiento: primero chequea competencia, y
recién después (si corresponde) aplica el premio/castigo de la
casilla. Es la pieza que compone reglas.py + efectos.py.

Entregable 1 - Programación Funcional - Programación Avanzada 2026
"""

from typing import Callable, Dict, Optional, Tuple

from modelo import TOTAL_CASILLAS
from reglas import retroceder_jugador, tirar_dado, reemplazar_jugador
from efectos import aplicar_efecto, ElegirColorCastigado


def buscar_ocupante(
    jugadores: Tuple[Dict, ...], posicion: int, colores_a_excluir
) -> Optional[Dict]:
    """
    Busca si hay algún jugador (que no sea de los colores excluidos)
    parado en `posicion`. Usa `filter` (uno de los requisitos) en vez
    de un for explícito.
    """
    candidatos = filter(
        lambda j: j["posicion"] == posicion and j["color"] not in colores_a_excluir,
        jugadores,
    )
    return next(candidatos, None)


def resolver_competencia(
    mover: Dict,
    defensor: Dict,
    jugadores: Tuple[Dict, ...],
    tirar_dado_fn: Callable[[], int] = tirar_dado,
) -> Tuple[Dict, Dict, Tuple[Dict, ...]]:
    """
    Resuelve la competencia por una casilla entre `mover` (el que
    acaba de llegar) y `defensor` (el que ya estaba ahí). Tiran el
    dado cada uno; el que saca más se queda, el otro retrocede 2
    (y 1 más si esa nueva casilla también está ocupada por un tercero).

    En caso de empate se vuelve a tirar (recursión) hasta que haya
    un ganador.

    Devuelve (ganador, perdedor_ya_retrocedido, jugadores_actualizados).
    """
    valor_mover = tirar_dado_fn()
    valor_defensor = tirar_dado_fn()

    if valor_mover == valor_defensor:
        return resolver_competencia(mover, defensor, jugadores, tirar_dado_fn)

    ganador, perdedor = (mover, defensor) if valor_mover > valor_defensor else (defensor, mover)

    perdedor_retrocedido = retroceder_jugador(perdedor, 2)

    # Si el perdedor cae en otra casilla ocupada por un tercer jugador,
    # retrocede una más (regla 4, segunda parte).
    hay_extremos = perdedor_retrocedido["posicion"] in (0, TOTAL_CASILLAS - 1)
    if not hay_extremos:
        tercero = buscar_ocupante(
            jugadores, perdedor_retrocedido["posicion"], {ganador["color"], perdedor["color"]}
        )
        if tercero is not None:
            perdedor_retrocedido = retroceder_jugador(perdedor_retrocedido, 1)

    jugadores_actualizados = reemplazar_jugador(jugadores, ganador)
    jugadores_actualizados = reemplazar_jugador(jugadores_actualizados, perdedor_retrocedido)

    return ganador, perdedor_retrocedido, jugadores_actualizados


def resolver_llegada(
    jugador_recien_movido: Dict,
    jugadores: Tuple[Dict, ...],
    elegir_color_castigado: ElegirColorCastigado,
    tirar_dado_fn: Callable[[], int] = tirar_dado,
) -> Tuple[Dict, Tuple[Dict, ...]]:
    """
    Función de composición: junta la detección de competencia con la
    aplicación de premios/castigos. Se llama una sola vez después de
    cada movimiento por dado:

      1. Si la casilla de llegada está ocupada por otro jugador (y no
         es INICIO ni FIN), se resuelve la competencia.
      2. Si el que se movió ganó (o no hubo competencia), se le aplica
         el efecto de la casilla (P1/P2/P3/C1/C2).
      3. Si el que se movió perdió la competencia, NO se le aplica
         ningún efecto de casilla (el retroceso no cuenta como "caer").
    """
    posicion = jugador_recien_movido["posicion"]
    es_extremo = posicion in (0, TOTAL_CASILLAS - 1)

    defensor = None if es_extremo else buscar_ocupante(jugadores, posicion, {jugador_recien_movido["color"]})

    if defensor is None:
        return aplicar_efecto(jugador_recien_movido, jugadores, elegir_color_castigado)

    ganador, _perdedor, jugadores_actualizados = resolver_competencia(
        jugador_recien_movido, defensor, jugadores, tirar_dado_fn
    )

    if ganador["color"] == jugador_recien_movido["color"]:
        return aplicar_efecto(ganador, jugadores_actualizados, elegir_color_castigado)

    jugador_final = next(j for j in jugadores_actualizados if j["color"] == jugador_recien_movido["color"])
    return jugador_final, jugadores_actualizados


if __name__ == "__main__":
    from modelo import crear_jugador
    from efectos import elegir_color_al_azar

    print("--- Competencia: mover gana (6 vs 3) ---")
    mover = crear_jugador("Ana", "rojo", posicion=10)
    defensor = crear_jugador("Beto", "azul", posicion=10)
    secuencia = iter([6, 3])
    ganador, perdedor, jugadores_actualizados = resolver_competencia(
        mover, defensor, (mover, defensor), lambda: next(secuencia)
    )
    print(f"Gana {ganador['nombre']} en posición {ganador['posicion']} (esperado 10)")
    print(f"Pierde {perdedor['nombre']} y retrocede a posición {perdedor['posicion']} (esperado 8)")

    print("\n--- Competencia con empate (4 vs 4, luego 2 vs 5) ---")
    mover = crear_jugador("Ana", "rojo", posicion=10)
    defensor = crear_jugador("Beto", "azul", posicion=10)
    secuencia = iter([4, 4, 2, 5])
    ganador, perdedor, _ = resolver_competencia(mover, defensor, (mover, defensor), lambda: next(secuencia))
    print(f"Gana {ganador['nombre']} (esperado Beto, porque empató y en el segundo tiro sacó más)")

    print("\n--- Perdedor cae en casilla ocupada por un tercero (retrocede 1 más) ---")
    mover = crear_jugador("Ana", "rojo", posicion=10)
    defensor = crear_jugador("Beto", "azul", posicion=10)
    tercero = crear_jugador("Caro", "verde", posicion=8)  # justo 2 casillas antes
    secuencia = iter([2, 6])  # Ana pierde contra Beto
    _, perdedor, jugadores_actualizados = resolver_competencia(
        mover, defensor, (mover, defensor, tercero), lambda: next(secuencia)
    )
    print(f"Ana retrocede a posición {perdedor['posicion']} (esperado 7: -2 por perder, -1 más por Caro)")

    print("\n--- resolver_llegada: sin colisión, cae en C1 ---")
    jugador = crear_jugador("Ana", "rojo", posicion=16)  # casilla C1
    otros = crear_jugador("Beto", "azul", posicion=0)
    jugador_final, _ = resolver_llegada(jugador, (jugador, otros), elegir_color_al_azar)
    print(f"Ana pierde_turno = {jugador_final['pierde_turno']} (esperado True)")