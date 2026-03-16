package org.example

fun main() {

    val bounces = generateSequence(1000.0) { it * 0.6 }
        .takeWhile { it >= 1 }
        .take(15)
        .map { String.format("%.2f", it) }
        .toList()

    println(bounces)
}