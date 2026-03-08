package com.example;

import com.example.lexer.Lexer;
import com.example.lexer.Token;
import com.example.parser.Parser;
import com.example.parser.ast.Programa;
import com.example.generador.GeneradorBytecode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Verificar si se pasó un archivo como argumento
        if (args.length == 0) {
            System.out.println("❌ Uso: java Main <archivo.qz>");
            System.out.println("   Ejemplo: java Main ejemplos/suma.qz");
            return;
        }

        String archivoEntrada = args[0];

        try {
            System.out.println("=== COMPILADOR QUETZAL → JVM ===\n");
            System.out.println("📁 Archivo: " + archivoEntrada);

            // Leer el archivo .qtz
            String codigoQuetzal = Files.readString(Paths.get(archivoEntrada));

            System.out.println("\nCódigo Quetzal:");
            System.out.println("─────────────────────");
            System.out.println(codigoQuetzal);
            System.out.println("─────────────────────");

            // FASE 1: LEXER
            System.out.println("\n--- FASE 1: ANÁLISIS LÉXICO ---");
            Lexer lexer = new Lexer(codigoQuetzal);
            List<Token> tokens = lexer.tokenizar();
            System.out.println("✅ Tokens generados: " + tokens.size());

            // FASE 2: PARSER
            System.out.println("\n--- FASE 2: ANÁLISIS SINTÁCTICO ---");
            Parser parser = new Parser(tokens);
            Programa programa = parser.parsear();
            System.out.println("✅ AST construido correctamente");

            // FASE 3: GENERADOR DE BYTECODE
            System.out.println("\n--- FASE 3: GENERACIÓN DE BYTECODE ---");

            // Obtener nombre del archivo sin extensión
            String nombreArchivo = Paths.get(archivoEntrada).getFileName().toString();
            String nombreClase = nombreArchivo.replace(".qz", "");
            nombreClase = nombreClase.substring(0, 1).toUpperCase() + nombreClase.substring(1);

            String archivoSalida = "output/" + nombreClase + ".class";

            GeneradorBytecode generador = new GeneradorBytecode(nombreClase);
            generador.generarConImpresion(programa, archivoSalida);

            System.out.println("\n🎉 ¡COMPILACIÓN EXITOSA!");
            System.out.println("📁 Archivo generado: " + archivoSalida);
            System.out.println("\n💡 Para ejecutar:");
            System.out.println("   cd output");
            System.out.println("   java " + nombreClase);

        } catch (IOException e) {
            System.err.println("❌ ERROR al leer el archivo: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ ERROR de compilación: " + e.getMessage());
            e.printStackTrace();
        }
    }
}