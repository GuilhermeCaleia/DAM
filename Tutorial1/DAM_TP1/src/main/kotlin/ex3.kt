package org.example

// Calcula sucessivas alturas de um balao que perde 40% em cada salto ate cair abaixo de 1 metro ou 15 iteracoes.
fun main() {

    val heights = mutableListOf<String>()
    var current = 1000.0
    var count = 0

    // Continua enquanto ainda ha altura relevante e limita a 15 passos para evitar ciclos longos.
    while (current >= 1 && count < 15) {
        heights.add(String.format("%.2f", current))
        current *= 0.6
        count++
    }

    println(heights)
}
