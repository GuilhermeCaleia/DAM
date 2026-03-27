package org.example.ex1.pipeline

/**
 * Ex 1.3 - Pipeline configurável com funções de alta ordem.
 */
class Pipeline {
    // guarda nome e função de cada etapa
    private data class Stage(val name: String, val transform: (List<String>) -> List<String>)

    private val stages = mutableListOf<Stage>()

    // adiciona uma etapa ao fim
    fun addStage(name: String, transform: (List<String>) -> List<String>) {
        stages.add(Stage(name, transform))
    }

    // corre todas as etapas pela ordem
    fun execute(input: List<String>): List<String> {
        var current = input
        for (stage in stages) {
            current = stage.transform(current)
        }
        return current
    }

    // mostra as etapas com numeração
    fun describe() {
        println("Pipeline stages :")
        for ((index, stage) in stages.withIndex()) {
            println("${index + 1}. ${stage.name}")
        }
    }
}

fun buildPipeline(block: Pipeline.() -> Unit): Pipeline =
    Pipeline().apply(block)

private fun samplePipeline(): Pipeline =
    buildPipeline {
        // remove espaços laterais
        addStage("Trim") { lines ->
            val result = mutableListOf<String>()
            for (line in lines) result.add(line.trim())
            result
        }
        // fica apenas com linhas que contêm "ERROR"
        addStage("Filter errors") { lines ->
            val result = mutableListOf<String>()
            for (line in lines) {
                if (line.contains("ERROR", ignoreCase = true)) result.add(line)
            }
            result
        }
        // converte para maiúsculas
        addStage("Uppercase") { lines ->
            val result = mutableListOf<String>()
            for (line in lines) result.add(line.uppercase())
            result
        }
        // adiciona índice 1-based
        addStage("Add index") { lines ->
            val result = mutableListOf<String>()
            for ((idx, line) in lines.withIndex()) result.add("${idx + 1}. $line")
            result
        }
    }

fun main() {
    val logs = listOf(
        " INFO : server started ",
        " ERROR : disk full ",
        " DEBUG : checking config ",
        " ERROR : out of memory ",
        " INFO : request received ",
        " ERROR : connection timeout "
    )

    val pipeline = samplePipeline()
    pipeline.describe()
    println("Result :")
    pipeline.execute(logs).forEach { println(it) }
}
