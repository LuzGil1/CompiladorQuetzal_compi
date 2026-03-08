package com.example.parser;

import com.example.lexer.Token;
import com.example.lexer.TipoToken;
import com.example.parser.ast.*;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private List<Token> tokens;
    private int posicion;
    private Token tokenActual;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.posicion = 0;
        this.tokenActual = tokens.size() > 0 ? tokens.get(0) : null;
    }

    // Avanzar al siguiente token
    private void avanzar() {
        posicion++;
        if (posicion < tokens.size()) {
            tokenActual = tokens.get(posicion);
        }
    }

    // Verificar si el token actual es del tipo esperado
    private boolean verificar(TipoToken tipo) {
        if (tokenActual == null) return false;
        return tokenActual.getTipo() == tipo;
    }

    // Consumir un token esperado (si no coincide, lanza error)
    private Token consumir(TipoToken tipo, String mensajeError) {
        if (verificar(tipo)) {
            Token token = tokenActual;
            avanzar();
            return token;
        }
        throw new RuntimeException(mensajeError + " en línea " + tokenActual.getLinea());
    }

    // Saltar tokens de nueva línea
    private void saltarNuevasLineas() {
        while (verificar(TipoToken.NUEVA_LINEA)) {
            avanzar();
        }
    }

    // Parsear el programa completo
    public Programa parsear() {
        List<Nodo> instrucciones = new ArrayList<>();

        saltarNuevasLineas();

        while (!verificar(TipoToken.EOF)) {
            instrucciones.add(parsearInstruccion());
            saltarNuevasLineas();
        }

        return new Programa(instrucciones);
    }

    // Parsear una instrucción
    private Nodo parsearInstruccion() {
        // Declaración de variable
        if (verificar(TipoToken.ENTERO)) {
            return parsearDeclaracionVariable();
        }

        // Llamada a consola.mostrar()
        if (verificar(TipoToken.CONSOLA)) {  // ← DEBE DECIR CONSOLA, no IDENTIFICADOR
            return parsearLlamadaConsola();
        }

        throw new RuntimeException("Instrucción no reconocida en línea " + tokenActual.getLinea());
    }

// ========== MÉTODOS NUEVOS - Agregar al final de la clase Parser ==========

    // Parsear consola.mostrar(...)
    // Parsear consola.mostrar(...)
    private LlamadaFuncion parsearLlamadaConsola() {
        // Consumir 'consola'
        consumir(TipoToken.CONSOLA, "Se esperaba 'consola'");  // ← CAMBIAR AQUÍ

        // Consumir '.'
        consumir(TipoToken.PUNTO, "Se esperaba '.'");

        // Consumir el nombre del método
        Token metodoToken = consumir(TipoToken.IDENTIFICADOR, "Se esperaba nombre del método");
        String metodo = metodoToken.getValor();

        // Consumir '('
        consumir(TipoToken.PARENTESIS_IZQ, "Se esperaba '('");

        // Parsear argumentos (con concatenación)
        List<Expresion> argumentos = new ArrayList<>();

        if (!verificar(TipoToken.PARENTESIS_DER)) {
            argumentos.add(parsearExpresionString());
        }

        // Consumir ')'
        consumir(TipoToken.PARENTESIS_DER, "Se esperaba ')'");

        return new LlamadaFuncion("consola", metodo, argumentos);
    }

    // Parsear expresiones de string (con concatenación)
    private Expresion parsearExpresionString() {
        Expresion izquierda = parsearElementoString();

        // Concatenaciones con +
        while (verificar(TipoToken.MAS)) {
            avanzar(); // consumir '+'
            Expresion derecha = parsearElementoString();
            izquierda = new Concatenacion(izquierda, derecha);
        }

        return izquierda;
    }

    // Parsear un elemento de string (literal o variable.texto())
    private Expresion parsearElementoString() {
        // String literal
        if (verificar(TipoToken.STRING)) {
            String valor = tokenActual.getValor();
            avanzar();
            return new LiteralString(valor);
        }

        // Variable con posible .texto()
        if (verificar(TipoToken.IDENTIFICADOR)) {
            String nombre = tokenActual.getValor();
            avanzar();

            // Verificar si tiene .texto()
            if (verificar(TipoToken.PUNTO)) {
                avanzar(); // consumir '.'
                Token metodo = consumir(TipoToken.IDENTIFICADOR, "Se esperaba 'texto'");

                if (metodo.getValor().equals("texto")) {
                    consumir(TipoToken.PARENTESIS_IZQ, "Se esperaba '('");
                    consumir(TipoToken.PARENTESIS_DER, "Se esperaba ')'");
                    return new ConversionTexto(new Variable(nombre));
                }
            }

            return new Variable(nombre);
        }

        // Número (para casos como "resultado: " + 5)
        if (verificar(TipoToken.NUMERO)) {
            int valor = Integer.parseInt(tokenActual.getValor());
            avanzar();
            return new LiteralNumero(valor);
        }

        throw new RuntimeException("Expresión de string no válida en línea " + tokenActual.getLinea());
    }

    // Parsear declaración de variable: entero a = 5
    private DeclaracionVariable parsearDeclaracionVariable() {
        // Consumir 'entero'
        Token tipoToken = consumir(TipoToken.ENTERO, "Se esperaba 'entero'");
        String tipo = tipoToken.getValor();

        // Consumir el nombre de la variable
        Token nombreToken = consumir(TipoToken.IDENTIFICADOR, "Se esperaba un nombre de variable");
        String nombre = nombreToken.getValor();

        // Consumir '='
        consumir(TipoToken.IGUAL, "Se esperaba '='");

        // Parsear la expresión del lado derecho
        Expresion valor = parsearExpresion();

        return new DeclaracionVariable(tipo, nombre, valor);
    }

    // Parsear una expresión (con operadores de suma y resta)
    private Expresion parsearExpresion() {
        return parsearExpresionAditiva();
    }

    // Parsear expresiones aditivas: a + b, a - b
    private Expresion parsearExpresionAditiva() {
        Expresion izquierda = parsearExpresionMultiplicativa();

        while (verificar(TipoToken.MAS) || verificar(TipoToken.MENOS)) {
            String operador = tokenActual.getValor();
            avanzar();
            Expresion derecha = parsearExpresionMultiplicativa();
            izquierda = new OperacionBinaria(operador, izquierda, derecha);
        }

        return izquierda;
    }

    // Parsear expresiones multiplicativas: a * b, a / b
    private Expresion parsearExpresionMultiplicativa() {
        Expresion izquierda = parsearExpresionPrimaria();

        while (verificar(TipoToken.MULTIPLICACION) || verificar(TipoToken.DIVISION)) {
            String operador = tokenActual.getValor();
            avanzar();
            Expresion derecha = parsearExpresionPrimaria();
            izquierda = new OperacionBinaria(operador, izquierda, derecha);
        }

        return izquierda;
    }

    // Parsear expresiones primarias: números, variables, paréntesis
    private Expresion parsearExpresionPrimaria() {
        // Número
        if (verificar(TipoToken.NUMERO)) {
            int valor = Integer.parseInt(tokenActual.getValor());
            avanzar();
            return new LiteralNumero(valor);
        }

        // Variable
        if (verificar(TipoToken.IDENTIFICADOR)) {
            String nombre = tokenActual.getValor();
            avanzar();
            return new Variable(nombre);
        }

        // Paréntesis: (expresión)
        if (verificar(TipoToken.PARENTESIS_IZQ)) {
            avanzar(); // consumir '('
            Expresion expresion = parsearExpresion();
            consumir(TipoToken.PARENTESIS_DER, "Se esperaba ')'");
            return expresion;
        }

        throw new RuntimeException("Expresión no válida en línea " + tokenActual.getLinea());
    }
}