package com.example.lexer;

public enum TipoToken {
    // Palabras reservadas
    ENTERO,           // entero
    CONSOLA,          // consola (nuevo)

    // Identificadores y literales
    IDENTIFICADOR,    // nombres de variables: a, b, suma
    NUMERO,           // números: 1, 2, 3, 456
    STRING,           // cadenas: "Hola mundo"

    // Operadores
    IGUAL,            // =
    MAS,              // +
    MENOS,            // -
    MULTIPLICACION,   // *
    DIVISION,         // /

    // Símbolos
    PARENTESIS_IZQ,   // (
    PARENTESIS_DER,   // )
    PUNTO,            // .

    // Especiales
    NUEVA_LINEA,      // salto de línea
    EOF               // fin del archivo
}