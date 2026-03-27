package org.example

// Calculadora de linha de comando com operacoes aritmeticas, booleanas e bitwise basicas.
fun main() {

    try {
        println("Calculadora kotlin")

        // Le os operandos como inteiros; falha de parse e' tratada pelo try/catch.
        print("Primeiro numero: ")
        val a = readln().toInt()

        print("Segundo numero: ")
        val b = readln().toInt()

        // Permite escolher o operador a aplicar.
        println("Escolher operacao (+, -, *, /, &&, ||, !, shl, shr): ")
        val op = readln()

        // Seleciona a logica conforme o operador lido.
        val result = when (op) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> {
                if (b == 0) throw ArithmeticException("Divisao por 0")
                a / b
            }
            "&&" -> (a != 0 && b != 0)
            "||" -> (a != 0 || b != 0)
            "!" -> !(a != 0)
            "shl" -> a shl b
            "shr" -> a shr b
            else -> throw IllegalArgumentException("operacao invalida")
        }

        println("\nResultado: $result")

        // Apenas para resultados inteiros imprime representacoes extra.
        if (result is Int) {
            println("Decimal: $result")
            println("Hexadecimal: ${result.toString(16)}")
            println("Boolean: ${result != 0}")
        }

    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}
