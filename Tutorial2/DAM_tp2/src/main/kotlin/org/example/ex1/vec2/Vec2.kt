package org.example.ex1.vec2

import kotlin.math.sqrt

/**
 * Ex 1.4 - Biblioteca de vetores 2D com operadores.
 */
data class Vec2(val x: Double, val y: Double) : Comparable<Vec2> {

    // soma componente a componente
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)

    // subtrai componente a componente
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)

    // multiplica por escalar
    operator fun times(scalar: Double) = Vec2(x * scalar, y * scalar)

    // inverte sinal
    operator fun unaryMinus() = Vec2(-x, -y)

    override operator fun compareTo(other: Vec2): Int {
        val diff = magnitude() - other.magnitude()
        return when {
            diff < 0 -> -1
            diff > 0 -> 1
            else -> 0
        }
    }

    // comprimento do vetor
    fun magnitude(): Double = sqrt(x * x + y * y)

    // produto escalar
    fun dot(other: Vec2): Double = x * other.x + y * other.y

    // devolve vetor normalizado; falha se for zero
    fun normalized(): Vec2 {
        val mag = magnitude()
        require(mag != 0.0) { "Cannot normalize the zero vector" }
        return Vec2(x / mag, y / mag)
    }

    // acesso por índice 0 -> x, 1 -> y
    operator fun get(index: Int): Double = when (index) {
        0 -> x
        1 -> y
        else -> throw IndexOutOfBoundsException("Vec2 supports only indices 0 and 1")
    }

}

/**
 * Multiplicação escalar pela esquerda (desafio opcional).
 */
operator fun Double.times(v: Vec2) = v * this

fun main() {
    val a = Vec2(3.0, 4.0)
    val b = Vec2(1.0, 2.0)
    println("a = $a")
    println("b = $b")
    println("a + b = ${a + b}")
    println("a - b = ${a - b}")
    println("a * 2.0 = ${a * 2.0}")
    println("-a = ${-a}")
    println("|a| = ${a.magnitude()}")
    println("a dot b = ${a.dot(b)}")
    println("norm(a) = ${a.normalized()}")
    println("a[0] = ${a[0]}")
    println("a[1] = ${a[1]}")
    println("a > b = ${a > b}")
    println("a < b = ${a < b}")
    val vectors = listOf(Vec2(1.0, 0.0), Vec2(3.0, 4.0), Vec2(0.0, 2.0))
    println("Longest = ${vectors.max()}")
    println("Shortest = ${vectors.min()}")
    val (x, y) = a
    println("Destructured a -> x=$x, y=$y")
    println("2.0 * a = ${2.0 * a}")
}
