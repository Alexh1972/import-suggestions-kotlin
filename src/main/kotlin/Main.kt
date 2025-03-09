const val SMALL_BOUND = Int.MIN_VALUE

fun main(args: Array<String>) {
    if (args.size != 1 && args.size != 2) {
        println("Usage: ./suggestions <search-name> [number-of-suggestions]")
        return
    }

    if (args.size == 1) {
        val input = args[0]
        KotlinImport.suggestImports(input.toLowerCase())
    } else {
        val input = args[0]
        val numberSuggestions = args[1]
        KotlinImport.suggestImports(input.toLowerCase(), numberSuggestions.toInt())
    }
}