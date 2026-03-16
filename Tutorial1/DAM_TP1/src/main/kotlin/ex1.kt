package org.example

fun main() {

    // a) IntArray constructor
    val squaresA = IntArray(50) { i -> (i + 1) * (i + 1) }
    println("a) IntArray:")
    println(squaresA.joinToString())


    // b) range e map()
    val squaresB = (1..50).map { it * it }
    println("\nb) Range + map:")
    println(squaresB.joinToString())


    // c) Array com constructor
    val squaresC = Array(50) { i -> (i + 1) * (i + 1) }
    println("\nc) Array constructor:")
    println(squaresC.joinToString())
}