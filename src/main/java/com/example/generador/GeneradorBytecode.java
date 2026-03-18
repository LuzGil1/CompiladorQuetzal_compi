package com.example.generador;

import com.example.parser.ast.*;
import com.example.semantico.enums.TipoDato;
import com.example.semantico.gestores.TablaSimbolos;
import org.objectweb.asm.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class GeneradorBytecode {
    private ClassWriter classWriter;
    private MethodVisitor methodVisitor;
    private Map<String, Integer> variables;
    private Map<String, String> tiposVariables;
    private int contadorVariables;
    private TablaSimbolos tabla;
    private String nombreClase;

    public GeneradorBytecode(String nombreClase, TablaSimbolos tabla) {
        this.nombreClase = nombreClase;
        this.tabla = tabla;
    }

    public void generar(Programa programa, String archivoSalida) throws IOException {
        // Crear el ClassWriter
        classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        // Definir la clase
        classWriter.visit(
                V11,                          // Versión de Java
                ACC_PUBLIC,                   // Modificadores
                nombreClase,                  // Nombre de la clase
                null,                         // Signature (generics)
                "java/lang/Object",           // Superclase
                null                          // Interfaces
        );

        // Crear el método main
        methodVisitor = classWriter.visitMethod(
                ACC_PUBLIC | ACC_STATIC,      // public static
                "main",                       // Nombre del método
                "([Ljava/lang/String;)V",    // Descriptor (args[], retorna void)
                null,                         // Signature
                null                          // Excepciones
        );

        methodVisitor.visitCode();

        // Generar código para cada instrucción del programa
        for (Nodo instruccion : programa.getInstrucciones()) {
            if (instruccion instanceof DeclaracionVariable) {
                generarDeclaracionVariable((DeclaracionVariable) instruccion);
            } else if (instruccion instanceof LlamadaFuncion) {
                generarLlamadaConsola((LlamadaFuncion) instruccion);
            }
        }

        // Retornar
        methodVisitor.visitInsn(RETURN);

        // Finalizar método
        methodVisitor.visitMaxs(0, 0); // Se calculan automáticamente
        methodVisitor.visitEnd();

        // Finalizar clase
        classWriter.visitEnd();

        // Escribir el archivo .class
        byte[] bytecode = classWriter.toByteArray();
        try (FileOutputStream fos = new FileOutputStream(archivoSalida)) {
            fos.write(bytecode);
        }

        System.out.println("Archivo generado: " + archivoSalida);
    }


    private void generarDeclaracionVariable(DeclaracionVariable decl) {
        // Obtener índice desde la tabla de símbolos
        int indiceVariable = tabla.obtenerIndice(decl.getNombre());

        // Generar código para la expresión del lado derecho
        generarExpresion(decl.getValor());

        // Guardar según el tipo (consultando la tabla)
        TipoDato tipo = tabla.obtenerTipo(decl.getNombre());
        if (tipo == TipoDato.TEXTO) {
            methodVisitor.visitVarInsn(ASTORE, indiceVariable);
        } else {
            methodVisitor.visitVarInsn(ISTORE, indiceVariable);
        }
    }

    // ⭐ MODIFICADO - Ahora maneja LlamadaFuncion también
    private void generarExpresion(Expresion expresion) {
        if (expresion instanceof LiteralNumero) {
            generarLiteralNumero((LiteralNumero) expresion);
        } else if (expresion instanceof Variable) {
            generarVariable((Variable) expresion);
        } else if (expresion instanceof OperacionBinaria) {
            generarOperacionBinaria((OperacionBinaria) expresion);
        } else if (expresion instanceof ConversionNumero) {
            generarConversionNumero((ConversionNumero) expresion);
        } else if (expresion instanceof LlamadaFuncion) { // ⭐ NUEVO
            generarLlamadaConsola((LlamadaFuncion) expresion);
        }
    }

    private void generarLiteralNumero(LiteralNumero literal) {
        int valor = literal.getValor();

        // Optimización: usar instrucciones específicas para valores pequeños
        if (valor >= -1 && valor <= 5) {
            methodVisitor.visitInsn(ICONST_0 + valor);
        } else if (valor >= -128 && valor <= 127) {
            methodVisitor.visitIntInsn(BIPUSH, valor);
        } else if (valor >= -32768 && valor <= 32767) {
            methodVisitor.visitIntInsn(SIPUSH, valor);
        } else {
            methodVisitor.visitLdcInsn(valor);
        }
    }

    // ⭐ MODIFICADO - Ahora maneja tipos (texto y numero)
    private void generarVariable(Variable variable) {
        String nombre = variable.getNombre();

        // Obtener información de la tabla
        int indice = tabla.obtenerIndice(nombre);
        TipoDato tipo = tabla.obtenerTipo(nombre);

        if (tipo == TipoDato.TEXTO) {
            methodVisitor.visitVarInsn(ALOAD, indice);
        } else {
            methodVisitor.visitVarInsn(ILOAD, indice);
        }
    }

    private void generarOperacionBinaria(OperacionBinaria operacion) {
        // Generar código para el lado izquierdo
        generarExpresion(operacion.getIzquierda());

        // Generar código para el lado derecho
        generarExpresion(operacion.getDerecha());

        // Aplicar el operador
        String operador = operacion.getOperador();
        switch (operador) {
            case "+":
                methodVisitor.visitInsn(IADD);
                break;
            case "-":
                methodVisitor.visitInsn(ISUB);
                break;
            case "*":
                methodVisitor.visitInsn(IMUL);
                break;
            case "/":
                methodVisitor.visitInsn(IDIV);
                break;
            default:
                throw new RuntimeException("Operador no soportado: " + operador);
        }
    }

    private void generarConversionNumero(ConversionNumero conversion) {
        // Generar la expresión (debe ser un String)
        generarExpresion(conversion.getExpresion());

        // Convertir String a double usando Double.parseDouble()
        methodVisitor.visitMethodInsn(
                INVOKESTATIC,
                "java/lang/Double",
                "parseDouble",
                "(Ljava/lang/String;)D",
                false
        );

        // Convertir double a int
        methodVisitor.visitInsn(D2I);
    }

    public void generarConImpresion(Programa programa, String archivoSalida) throws IOException {
        // Crear el ClassWriter
        classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        // Definir la clase
        classWriter.visit(
                V11,
                ACC_PUBLIC,
                nombreClase,
                null,
                "java/lang/Object",
                null
        );

        // Crear el método main
        methodVisitor = classWriter.visitMethod(
                ACC_PUBLIC | ACC_STATIC,
                "main",
                "([Ljava/lang/String;)V",
                null,
                null
        );

        methodVisitor.visitCode();

        // Generar código para cada instrucción del programa
        for (Nodo instruccion : programa.getInstrucciones()) {
            if (instruccion instanceof DeclaracionVariable) {
                DeclaracionVariable decl = (DeclaracionVariable) instruccion;
                generarDeclaracionVariable(decl);
            } else if (instruccion instanceof LlamadaFuncion) {
                generarLlamadaConsola((LlamadaFuncion) instruccion);
            }
        }

        // Retornar
        methodVisitor.visitInsn(RETURN);

        // Finalizar método
        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();

        // Finalizar clase
        classWriter.visitEnd();

        // Escribir el archivo .class
        byte[] bytecode = classWriter.toByteArray();
        try (FileOutputStream fos = new FileOutputStream(archivoSalida)) {
            fos.write(bytecode);
        }

        System.out.println("Archivo generado: " + archivoSalida);
    }

    // ⭐ MODIFICADO - Ahora soporta mostrar() y pedir()
    private void generarLlamadaConsola(LlamadaFuncion llamada) {
        String metodo = llamada.getMetodo();

        if (metodo.equals("mostrar")) {
            // System.out.println(...)
            methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");

            if (!llamada.getArgumentos().isEmpty()) {
                generarExpresionString(llamada.getArgumentos().get(0));
            }

            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

        } else if (metodo.equals("pedir")) {
            // Mostrar el mensaje (si hay argumento)
            if (!llamada.getArgumentos().isEmpty()) {
                methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                generarExpresionString(llamada.getArgumentos().get(0));
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V", false);
            }

            // Leer input con Scanner
            // new Scanner(System.in)
            methodVisitor.visitTypeInsn(NEW, "java/util/Scanner");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "in", "Ljava/io/InputStream;");
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/Scanner", "<init>", "(Ljava/io/InputStream;)V", false);

            // scanner.nextLine()
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/util/Scanner", "nextLine", "()Ljava/lang/String;", false);

            // El String queda en el stack
        }
    }

    // Generar expresión de string
    private void generarExpresionString(Expresion expr) {
        if (expr instanceof LiteralString) {
            // String literal directo
            methodVisitor.visitLdcInsn(((LiteralString) expr).getValor());

        } else if (expr instanceof Concatenacion) {
            // Concatenación: usar StringBuilder
            generarConcatenacion((Concatenacion) expr);

        } else if (expr instanceof ConversionTexto) {
            // Convertir número a string
            generarExpresion(((ConversionTexto) expr).getExpresion());
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false);

        } else if (expr instanceof Variable) {
            // Agregar variable
            String nombre = ((Variable) expr).getNombre();
            generarVariable((Variable) expr);

            TipoDato tipo = tabla.obtenerTipo(nombre);
            if (tipo == TipoDato.TEXTO) {
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            } else {
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
            }

        } else if (expr instanceof LiteralNumero) {
            // Número literal
            generarLiteralNumero((LiteralNumero) expr);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false);
        }
    }

    // Generar concatenación de strings
    private void generarConcatenacion(Concatenacion concat) {
        // Crear StringBuilder
        methodVisitor.visitTypeInsn(NEW, "java/lang/StringBuilder");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);

        // Agregar elementos
        agregarAStringBuilder(concat.getIzquierda());
        agregarAStringBuilder(concat.getDerecha());

        // Convertir a String
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
    }

    // Agregar expresión al StringBuilder
    private void agregarAStringBuilder(Expresion expr) {
        if (expr instanceof LiteralString) {
            // Agregar string
            methodVisitor.visitLdcInsn(((LiteralString) expr).getValor());
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

        } else if (expr instanceof ConversionTexto) {
            // Agregar número convertido
            generarExpresion(((ConversionTexto) expr).getExpresion());
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);

        } else if (expr instanceof Variable) {
            // Agregar variable
            String nombre = ((Variable) expr).getNombre();
            generarVariable((Variable) expr);

            String tipo = tiposVariables.get(nombre);
            if (tipo != null && tipo.equals("texto")) {
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            } else {
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
            }

        } else if (expr instanceof LiteralNumero) {
            // Agregar número literal
            generarLiteralNumero((LiteralNumero) expr);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);

        } else if (expr instanceof Concatenacion) {
            // Concatenación anidada
            agregarAStringBuilder(((Concatenacion) expr).getIzquierda());
            agregarAStringBuilder(((Concatenacion) expr).getDerecha());
        }
    }
}