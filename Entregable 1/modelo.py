"""
modelo.py
Representación del tablero y del estado del juego usando únicamente
diccionarios y tuplas (sin definir ninguna clase propia). La
inmutabilidad es por convención: nunca se modifica un diccionario
existente, siempre se construye uno nuevo con `{**original, ...}`.

Entregable 1 - Programación Funcional - Programación Avanzada 2026
"""

from typing import Dict, Tuple

# ---------------------------------------------------------------------------
# Configuración del tablero (grilla de 10x10, camino perimetral / anillo)
# ---------------------------------------------------------------------------

FILAS = 10
COLUMNAS = 10

# Casillas especiales: índice dentro del camino -> tipo de casilla
# (fila, columna en base 1, según la letra: INICIO(10,1), P1(5,1),
#  P2(1,3), C1(1,8), P3(5,10), C2(10,8), FIN(10,2))
CASILLAS_ESPECIALES = {
    0: "INICIO",
    5: "P1",
    11: "P2",
    16: "C1",
    22: "P3",
    29: "C2",
    35: "FIN",
}


def generar_perimetro(filas: int, columnas: int):
    """
    Generador (yield) que recorre el borde de una grilla filas x columnas
    en sentido antihorario, empezando en la esquina inferior izquierda:
    sube por el borde izquierdo, cruza por arriba, baja por el borde
    derecho y vuelve por abajo hasta cerrar el anillo (sin repetir esquinas).

    Devuelve tuplas (fila, columna), en base 1, en el orden del recorrido.
    """
    # Borde izquierdo, de abajo hacia arriba (columna 1)
    for fila in range(filas, 0, -1):
        yield (fila, 1)

    # Borde superior, de izquierda a derecha (fila 1), sin repetir la esquina
    for columna in range(2, columnas + 1):
        yield (1, columna)

    # Borde derecho, de arriba hacia abajo (columna = columnas), sin repetir la esquina
    for fila in range(2, filas + 1):
        yield (fila, columnas)

    # Borde inferior, de derecha a izquierda (fila = filas), sin repetir esquinas
    for columna in range(columnas - 1, 1, -1):
        yield (filas, columna)


def crear_casilla(indice: int, fila: int, columna: int, tipo: str) -> Dict:
    """Construye una casilla como diccionario (registro inmutable por convención)."""
    return {"indice": indice, "fila": fila, "columna": columna, "tipo": tipo}


def crear_tablero() -> Tuple[Dict, ...]:
    """
    Construye el tablero como una tupla inmutable de diccionarios-casilla,
    combinando el generador de perímetro con una comprensión que asigna
    a cada índice su tipo correspondiente (NORMAL salvo que sea especial).
    """
    coordenadas = list(generar_perimetro(FILAS, COLUMNAS))
    return tuple(
        crear_casilla(i, fila, columna, CASILLAS_ESPECIALES.get(i, "NORMAL"))
        for i, (fila, columna) in enumerate(coordenadas)
    )


TABLERO: Tuple[Dict, ...] = crear_tablero()
TOTAL_CASILLAS = len(TABLERO)  # 36


# ---------------------------------------------------------------------------
# Estado del juego (diccionarios, inmutables por convención)
# ---------------------------------------------------------------------------

def crear_jugador(nombre: str, color: str, posicion: int = 0, pierde_turno: bool = False) -> Dict:
    """Construye un jugador como diccionario. posicion=0 significa 'en INICIO'."""
    return {
        "nombre": nombre,
        "color": color,
        "posicion": posicion,
        "pierde_turno": pierde_turno,
    }


def crear_estado(jugadores: Tuple[Dict, ...], turno_actual: int = 0) -> Dict:
    """Construye el estado global del juego."""
    return {"jugadores": jugadores, "turno_actual": turno_actual}


if __name__ == "__main__":
    for casilla in TABLERO:
        print(casilla)
    print(f"\nTotal de casillas: {TOTAL_CASILLAS}")