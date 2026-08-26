IVA = 0.21


def calcula_iva_lista(precios):
    return [precio * IVA for precio in precios]


def precios_mayores(precios, precio):
    return [p for p in precios if p >= precio]


def suma_precios_con_iva(precios):
    return sum(precio + precio * IVA for precio in precios)


if __name__ == "__main__":
    precios = [100, 250, 999.99]
    print(calcula_iva_lista(precios))
    print(precios_mayores(precios, 250))
    print(suma_precios_con_iva(precios))
