package com.example.lexer;

public enum TipoToken {
    // Palabras reservadas
    TIPO_NUMERO,
    TIPO_TEXTO,
    TIPO_ENTERO,
    CONSOLA,

    // Identificadores y literales
    IDENTIFICADOR,
    LITERAL_NUMERO,   // 42, 3.14
    LITERAL_STRING,
    STRING_INTERPOLADO,

    // Operadores
    IGUAL,
    MAS,
    MENOS,
    MULTIPLICACION,
    DIVISION,

    // Símbolos
    PARENTESIS_IZQ,
    PARENTESIS_DER,
    PUNTO,
    DOS_PUNTOS,

    // Especiales
    NUEVA_LINEA,
    EOF
}