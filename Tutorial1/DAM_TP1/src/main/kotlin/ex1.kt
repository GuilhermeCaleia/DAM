package org.example

fun main() {

    // a) IntArray constructor
    val squaresA = IntArray(50)
    for (i in squaresA.indices) {
        val n = i + 1
        squaresA[i] = n * n
    }
    println("a) IntArray:")
    for (value in squaresA) print("$value ")
    println()

// b) Using a range and map();
    val squaresB = (1..50).map { it * it }
    println("\nb) Range + map:")
    for (value in squaresB) print("$value ")
    println()

 // Using Array with constructor
    val squaresC = Array(50) { 0 }
    for (i in squaresC.indices) {
        val n = i + 1
        squaresC[i] = n * n
    }
    println("\nc) Array constructor:")
    for (value in squaresC) print("$value ")
    println()
}
