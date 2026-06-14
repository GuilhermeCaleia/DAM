package org.example.ex1.cache

/**
 * Ex 1.2 - Cache genérica em memória.
 */
class Cache<K : Any, V : Any> {
    // armazenamento interno simples
    private val entries: MutableMap<K, V> = mutableMapOf()

    // insere ou substitui um valor
    fun put(key: K, value: V) {
        entries[key] = value
    }

    // obtém valor ou null
    fun get(key: K): V? = entries[key]

    // remove um valor
    fun evict(key: K) {
        entries.remove(key)
    }

    // tamanho atual
    fun size(): Int = entries.size

    // devolve existente ou calcula e guarda
    fun getOrPut(key: K, default: () -> V): V {
        if (entries.containsKey(key)) return entries[key]!!
        val value = default()
        entries[key] = value
        return value
    }

    // transforma valor existente
    fun transform(key: K, action: (V) -> V): Boolean {
        if (!entries.containsKey(key)) return false
        val newValue = action(entries[key]!!)
        entries[key] = newValue
        return true
    }

    // cópia imutável
    fun snapshot(): Map<K, V> = entries.toMap()

    // filtra por predicado
    fun filterValues(predicate: (V) -> Boolean): Map<K, V> {
        val result = mutableMapOf<K, V>()
        for ((k, v) in entries) {
            if (predicate(v)) {
                result[k] = v
            }
        }
        return result.toMap()
    }
}

private fun wordFrequencyDemo() {
    val wordCache = Cache<String, Int>()
    wordCache.put("kotlin", 1)
    wordCache.put("scala", 1)
    wordCache.put("haskell", 1)

    println("--- Word frequency cache ---")
    println("Size : ${wordCache.size()}")
    println("Frequency of \"kotlin\": ${wordCache.get("kotlin")}")
    println("getOrPut \"kotlin\": ${wordCache.getOrPut("kotlin") { 0 }}")
    println("getOrPut \"java\": ${wordCache.getOrPut("java") { 0 }}")
    println("Size after getOrPut : ${wordCache.size()}")
    println("Transform \"kotlin\" (+1) : ${wordCache.transform("kotlin") { it + 1 }}")
    println("Transform \"cobol\" (+1) : ${wordCache.transform("cobol") { it + 1 }}")
    println("Snapshot : ${wordCache.snapshot()}")
    println("Words with count > 0 : ${wordCache.filterValues { it > 0 }}")
}

private fun idRegistryDemo() {
    val registry = Cache<Int, String>()
    registry.put(1, "Alice")
    registry.put(2, "Bob")

    println("--- Id registry cache ---")
    println("Id 1 -> ${registry.get(1)}")
    println("Id 2 -> ${registry.get(2)}")
    registry.evict(1)
    println("After evict id 1, size : ${registry.size()}")
    println("Id 1 after evict -> ${registry.get(1)}")
}

fun main() {
    wordFrequencyDemo()
    idRegistryDemo()
}
