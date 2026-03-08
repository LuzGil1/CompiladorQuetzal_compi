package com.example.generador;

import com.example.parser.ast.*;
import org.objectweb.asm.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class GeneradorBytecode {
    private ClassWriter classWriter;
    private MethodVisitor methodVisitor;
    private Map<String, Integer> variables; // Mapa de variables a índices locales
    private int contadorVariables;
    private String nombreClase;

    public GeneradorBytecode(String nombreClase) {
        this.nombreClase = nombreClase;
        this.variables = new HashMap<>();
        this.contadorVariables = 1; // 0 está reservado para args[] en main
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

        System.out.println("✅ Archivo generado: " + archivoSalida);
    }

    private void generarDeclaracionVariable(DeclaracionVariable decl) {
        // Asignar un índice local a la variable
        int indiceVariable = contadorVariables++;
        variables.put(decl.getNombre(), indiceVariable);

        // Generar código para la expresión del lado derecho
        generarExpresion(decl.getValor());

        // Guardar el resultado en la variable local
        methodVisitor.visitVarInsn(ISTORE, indiceVariable);
    }

    private void generarExpresion(Expresion expresion) {
        if (expresion instanceof LiteralNumero) {
            generarLiteralNumero((LiteralNumero) expresion);
        } else if (expresion instanceof Variable) {
            generarVariable((Variable) expresion);
        } else if (expresion instanceof OperacionBinaria) {
            generarOperacionBinaria((OperacionBinaria) expresion);
        }
    }

    private void generarLiteralNumero(LiteralNumero literal) {
        int valor = literal.getValor();

        // Optimización: usar instrucciones específicas para valores pequeños
        if (valor >= -1 && valor <= 5) {
            methodVisitor.visitInsn(ICONST_0 + valor); // ICONST_0, ICONST_1, ..., ICONST_5
        } else if (valor >= -128 && valor <= 127) {
            methodVisitor.visitIntInsn(BIPUSH, valor);
        } else if (valor >= -32768 && valor <= 32767) {
            methodVisitor.visitIntInsn(SIPUSH, valor);
        } else {
            methodVisitor.visitLdcInsn(valor);
        }
    }

    private void generarVariable(Variable variable) {
        String nombre = variable.getNombre();

        if (!variables.containsKey(nombre)) {
            throw new RuntimeException("Variable no declarada: " + nombre);
        }

        int indice = variables.get(nombre);
        methodVisitor.visitVarInsn(ILOAD, indice);
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

    // Agregar al final de la clase GeneradorBytecode, antes de la última llave }

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
                imprimirVariable(decl.getNombre());
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

        System.out.println("✅ Archivo generado: " + archivoSalida);
    }


    private void imprimirVariable(String nombreVariable) {
        // System.out.print(nombreVariable + " = ")
        methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        methodVisitor.visitLdcInsn(nombreVariable + " = ");
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V", false);

        // Cargar el valor de la variable
        int indice = variables.get(nombreVariable);
        methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        methodVisitor.visitVarInsn(ILOAD, indice);

        // System.out.println(valor)
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V", false);
    }


    // ========== MÉTODOS NUEVOS - Agregar al final de la clase ==========

    // Generar código para consola.mostrar()
    private void generarLlamadaConsola(LlamadaFuncion llamada) {
        // System.out
        methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");

        // Generar la expresión del argumento (puede ser string, concatenación, etc.)
        if (!llamada.getArgumentos().isEmpty()) {
            generarExpresionString(llamada.getArgumentos().get(0));
        }

        // println
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
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
            // Variable (cargar y convertir a string)
            generarVariable((Variable) expr);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false);

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
            generarVariable((Variable) expr);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);

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