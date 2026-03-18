package com.example.lexer;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private String codigo;
    private int posicion;
    private int linea;
    private char caracterActual;

    public Lexer(String codigo) {
        this.codigo = codigo;
        this.posicion = 0;
        this.linea = 1;
        this.caracterActual = codigo.length() > 0 ? codigo.charAt(0) : '\0';
    }

    // Avanzar al siguiente caracter
    private void avanzar() {
        posicion++;
        if (posicion < codigo.length()) {
            caracterActual = codigo.charAt(posicion);
        } else {
            caracterActual = '\0'; // Fin del archivo
        }
    }

    // Ver el siguiente caracter sin avanzar
    private char verSiguiente() {
        if (posicion + 1 < codigo.length()) {
            return codigo.charAt(posicion + 1);
        }
        return '\0';
    }

    // Saltar espacios en blanco (excepto saltos de línea)
    private void saltarEspacios() {
        while (caracterActual == ' ' || caracterActual == '\t' || caracterActual == '\r') {
            avanzar();
        }
    }

    // Leer un número
    private Token leerNumero() {
        StringBuilder numero = new StringBuilder();
        int lineaInicio = linea;

        while (Character.isDigit(caracterActual)) {
            numero.append(caracterActual);
            avanzar();
        }

        return new Token(TipoToken.LITERAL_NUMERO, numero.toString(), lineaInicio);
    }

    // Leer un identificador o palabra reservada
    // Leer un identificador o palabra reservada
    private Token leerIdentificador() {
        StringBuilder identificador = new StringBuilder();
        int lineaInicio = linea;

        while (Character.isLetterOrDigit(caracterActual) || caracterActual == '_') {
            identificador.append(caracterActual);
            avanzar();
        }

        String valor = identificador.toString();

        // Verificar si es una palabra reservada
        TipoToken tipo;
        switch (valor) {
            case "entero":
                tipo = TipoToken.TIPO_ENTERO;  // ← Actualizar
                break;
            case "numero":                      // ← AGREGAR
                tipo = TipoToken.TIPO_NUMERO;
                break;
            case "texto":                       // ← AGREGAR
                tipo = TipoToken.TIPO_TEXTO;
                break;
            case "consola":
                tipo = TipoToken.CONSOLA;
                break;
            default:
                tipo = TipoToken.IDENTIFICADOR;
                break;
        }

        return new Token(tipo, valor, lineaInicio);
    }

    // Leer un string entre comillas
    private Token leerString() {
        StringBuilder string = new StringBuilder();
        int lineaInicio = linea;

        avanzar(); // Saltar la comilla inicial "

        while (caracterActual != '"' && caracterActual != '\0') {
            string.append(caracterActual);
            avanzar();
        }

        if (caracterActual == '"') {
            avanzar(); // Saltar la comilla final "
        }

        return new Token(TipoToken.LITERAL_STRING, string.toString(), lineaInicio);
    }

    // Obtener el siguiente token
    private Token siguienteToken() {
        while (caracterActual != '\0') {

            // Saltar espacios
            if (caracterActual == ' ' || caracterActual == '\t' || caracterActual == '\r') {
                saltarEspacios();
                continue;
            }

            // Salto de línea
            if (caracterActual == '\n') {
                Token token = new Token(TipoToken.NUEVA_LINEA, "\\n", linea);
                linea++;
                avanzar();
                return token;
            }

            // Números
            if (Character.isDigit(caracterActual)) {
                return leerNumero();
            }

            // Identificadores y palabras reservadas
            if (Character.isLetter(caracterActual) || caracterActual == '_') {
                if (caracterActual == 't' && verSiguiente() == '"') {
                    avanzar(); // saltar la 't'
                    return leerStringInterpolado();
                }
                return leerIdentificador();

            }

            // Strings
            if (caracterActual == '"') {
                return leerString();
            }

            // Operadores y símbolos
            int lineaActual = linea;
            switch (caracterActual) {
                case '=':
                    avanzar();
                    return new Token(TipoToken.IGUAL, "=", lineaActual);
                case '+':
                    avanzar();
                    return new Token(TipoToken.MAS, "+", lineaActual);
                case '-':
                    avanzar();
                    return new Token(TipoToken.MENOS, "-", lineaActual);
                case '*':
                    avanzar();
                    return new Token(TipoToken.MULTIPLICACION, "*", lineaActual);
                case '/':
                    avanzar();
                    return new Token(TipoToken.DIVISION, "/", lineaActual);
                case '(':
                    avanzar();
                    return new Token(TipoToken.PARENTESIS_IZQ, "(", lineaActual);
                case ')':
                    avanzar();
                    return new Token(TipoToken.PARENTESIS_DER, ")", lineaActual);
                case '.':
                    avanzar();
                    return new Token(TipoToken.PUNTO, ".", lineaActual);
                case ':':                                                          // ← AGREGAR
                    avanzar();                                                     // ← AGREGAR
                    return new Token(TipoToken.DOS_PUNTOS, ":", lineaActual);     // ← AGREGAR
                default:
                    throw new RuntimeException("Caracter no reconocido: '" + caracterActual + "' en línea " + linea);
            }
        }

        return new Token(TipoToken.EOF, "", linea);
    }

    // Tokenizar todo el código
    public List<Token> tokenizar() {
        List<Token> tokens = new ArrayList<>();

        Token token = siguienteToken();
        while (token.getTipo() != TipoToken.EOF) {
            tokens.add(token);
            token = siguienteToken();
        }
        tokens.add(token); // Agregar el EOF

        return tokens;
    }

    // Leer un string con interpolación t"..."
    private Token leerStringInterpolado() {
        StringBuilder string = new StringBuilder();
        int lineaInicio = linea;

        avanzar(); // Saltar la comilla inicial "

        while (caracterActual != '"' && caracterActual != '\0') {
            string.append(caracterActual);
            avanzar();
        }

        if (caracterActual == '"') {
            avanzar(); // Saltar la comilla final "
        }

        return new Token(TipoToken.STRING_INTERPOLADO, string.toString(), lineaInicio);
    }
}