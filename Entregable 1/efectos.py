"""
efectos.py
Aplica el efecto de la casilla en la que cae un jugador (premios P1/P2/P3
y castigos C1/C2). La "competencia" por casillas ocupadas se resuelve
en otro módulo aparte (regla independiente). Todo con diccionarios,
sin clases; nunca se muta un jugador, siempre se crea uno nuevo.

Entregable 1 - Programación Funcional - Programación Avanzada 2026
"""

import random
from typing import Callable, Dict, Tuple

from reglas import avanzar_jugador, retroceder_jugador, obtener_casilla, tirar_dado, reemplazar_jugador

# Firma de la función que decide a qué color castigar en P1.
# Se inyecta desde afuera para no mezclar la lógica del juego con la
# entrada de datos: en modo interactivo pregunta al usuario, en modo
# simulación elige al azar. Así este módulo no sabe nada de GUI ni de
# consola, y sigue siendo fácil de probar.
ElegirColorCastigado = Callable[[Tuple[Dict, ...], str], str]



def aplicar_efecto(
    jugador: Dict,
    jugadores: Tuple[Dict, ...],
    elegir_color_castigado: ElegirColorCastigado,
) -> Tuple[Dict, Tuple[Dict, ...], Tuple[str, ...]]:
    """
    Aplica el efecto de la casilla donde quedó parado `jugador`.
    Devuelve (jugador_actualizado, jugadores_actualizados, eventos), los
    dos primeros NUEVOS (no se muta nada) y `eventos` una tupla de
    mensajes describiendo qué pasó (para mostrarle al usuario). Si el
    efecto implica moverse de nuevo (P2, P3) y la nueva casilla también
    tiene efecto, se resuelve en cadena mediante recursión, hasta llegar
    a una casilla sin efecto especial, y los mensajes se van acumulando.
    """
    casilla = obtener_casilla(jugador["posicion"])
    tipo = casilla["tipo"]

    if tipo == "P1":
        # Elige un color para que pierda un turno.
        color_elegido = elegir_color_castigado(jugadores, jugador["color"])
        jugador_castigado = next(j for j in jugadores if j["color"] == color_elegido)
        jugadores_actualizados = tuple(
            {**j, "pierde_turno": True} if j["color"] == color_elegido else j
            for j in jugadores
        )
        jugador_actualizado = next(j for j in jugadores_actualizados if j["color"] == jugador["color"])
        evento = (
            f"Premio (P1): {jugador['nombre']} hace que {jugador_castigado['nombre']} "
            f"({color_elegido}) pierda su próximo turno."
        )
        return jugador_actualizado, jugadores_actualizados, (evento,)

    if tipo == "P2":
        # Tira el dado nuevamente y avanza.
        dado = tirar_dado()
        nuevo_jugador = avanzar_jugador(jugador, dado)
        jugadores_actualizados = reemplazar_jugador(jugadores, nuevo_jugador)
        evento = (
            f"Premio (P2): {jugador['nombre']} tira el dado de nuevo, salió {dado} "
            f"y avanza a la casilla {nuevo_jugador['posicion']}."
        )
        # la casilla nueva puede tener efecto propio -> se encadena (recursión)
        jugador_final, jugadores_finales, eventos_siguientes = aplicar_efecto(
            nuevo_jugador, jugadores_actualizados, elegir_color_castigado
        )
        return jugador_final, jugadores_finales, (evento,) + eventos_siguientes

    if tipo == "P3":
        # Avanza 2 casillas.
        nuevo_jugador = avanzar_jugador(jugador, 2)
        jugadores_actualizados = reemplazar_jugador(jugadores, nuevo_jugador)
        evento = (
            f"Premio (P3): {jugador['nombre']} avanza 2 casillas más, "
            f"hasta la casilla {nuevo_jugador['posicion']}."
        )
        jugador_final, jugadores_finales, eventos_siguientes = aplicar_efecto(
            nuevo_jugador, jugadores_actualizados, elegir_color_castigado
        )
        return jugador_final, jugadores_finales, (evento,) + eventos_siguientes

    if tipo == "C1":
        # Pierde 1 turno.
        nuevo_jugador = {**jugador, "pierde_turno": True}
        jugadores_actualizados = reemplazar_jugador(jugadores, nuevo_jugador)
        evento = f"Castigo (C1): {jugador['nombre']} pierde su próximo turno."
        return nuevo_jugador, jugadores_actualizados, (evento,)

    if tipo == "C2":
        # Retrocede 3 casillas (sin pasar antes de INICIO).
        nuevo_jugador = retroceder_jugador(jugador, 3)
        jugadores_actualizados = reemplazar_jugador(jugadores, nuevo_jugador)
        evento = (
            f"Castigo (C2): {jugador['nombre']} retrocede 3 casillas, "
            f"hasta la casilla {nuevo_jugador['posicion']}."
        )
        return nuevo_jugador, jugadores_actualizados, (evento,)

    # NORMAL, INICIO o FIN: no hay efecto especial.
    return jugador, jugadores, ()


def elegir_color_al_azar(jugadores: Tuple[Dict, ...], color_propio: str) -> str:
    """Estrategia para modo simulación: castiga a un oponente al azar (nunca a sí mismo)."""
    oponentes = [j["color"] for j in jugadores if j["color"] != color_propio]
    return random.choice(oponentes)


if __name__ == "__main__":
    # Pruebas rápidas por consola de cada efecto, sin GUI.
    from modelo import crear_jugador

    jugadores_demo = (
        crear_jugador(nombre="Ana", color="rojo", posicion=0),
        crear_jugador(nombre="Beto", color="azul", posicion=0),
    )

    print("--- C2 (retrocede 3) ---")
    ana_en_c2 = {**jugadores_demo[0], "posicion": 29}  # casilla C2
    jugador_actualizado, jugadores_actualizados, eventos = aplicar_efecto(
        ana_en_c2, reemplazar_jugador(jugadores_demo, ana_en_c2), elegir_color_al_azar
    )
    print(f"Ana queda en posición {jugador_actualizado['posicion']} (esperado 26)")
    print(f"Evento: {eventos}")

    print("\n--- C1 (pierde 1 turno) ---")
    ana_en_c1 = {**jugadores_demo[0], "posicion": 16}  # casilla C1
    jugador_actualizado, _, eventos = aplicar_efecto(
        ana_en_c1, reemplazar_jugador(jugadores_demo, ana_en_c1), elegir_color_al_azar
    )
    print(f"Ana pierde_turno = {jugador_actualizado['pierde_turno']} (esperado True)")
    print(f"Evento: {eventos}")

    print("\n--- P3 (avanza 2) ---")
    ana_en_p3 = {**jugadores_demo[0], "posicion": 22}  # casilla P3
    jugador_actualizado, _, eventos = aplicar_efecto(
        ana_en_p3, reemplazar_jugador(jugadores_demo, ana_en_p3), elegir_color_al_azar
    )
    print(f"Ana queda en posición {jugador_actualizado['posicion']} (esperado 24)")
    print(f"Evento: {eventos}")

    print("\n--- P1 (elige color castigado) ---")
    ana_en_p1 = {**jugadores_demo[0], "posicion": 5}  # casilla P1
    jugadores_base = reemplazar_jugador(jugadores_demo, ana_en_p1)
    _, jugadores_actualizados, eventos = aplicar_efecto(ana_en_p1, jugadores_base, elegir_color_al_azar)
    print(f"Estado de Beto: {jugadores_actualizados[1]} (esperado pierde_turno=True, es el único oponente)")
    print(f"Evento: {eventos}")