import org.apache.commons.text.similarity.LevenshteinDistance
import java.io.File
import java.net.URL

class KotlinImport {
    companion object {
        /**
         * Checks if a given class name belongs to the Kotlin standard library.
         *
         * @param name The fully qualified class name.
         * @return True if the class belongs to the Kotlin standard library, otherwise false.
         */
        private fun isKotlinStdLibClass(name: String) : Boolean {
            return name.toLowerCase().matches(Regex("^kotlin\\.(?!x\\.).*"))
                    && !name.contains("$")
        }

        /**
         * Computes a match score between an input string and a target class name.
         *
         * @param input The search input string.
         * @param target The fully qualified class name.
         * @return A score representing the match relevance.
         */
        private fun matchScore(input: String, target: String) : Int {
            val names = target.toLowerCase().split(".")
            val namesSize = names.size
            val className = names.last()

            val index = className.indexOf(input)

            if (index == 0) {
                return SMALL_BOUND
            }

            if (index != -1) {
                return SMALL_BOUND + index
            }

            for (name in names) {
                if (name.startsWith(input)) {
                    return 0
                }
            }

            val distanceSearchLength = (input.length).coerceAtMost(names[namesSize - 1].length)
            return LevenshteinDistance().apply(input, className.substring(0, distanceSearchLength))
        }

        /**
         * Finds and collects class names from a given file or directory.
         *
         * @param file The file or directory to search.
         * @param suggestions A mutable list to store found class names.
         */
        private fun findClasses(file: File, suggestions: MutableList<String>) {
            if (file.isDirectory) {
                file.walk().forEach {
                    if (it.isFile && it.extension == "class") {
                        val className = it
                            .relativeTo(file.parentFile)
                            .path
                            .replace(File.separatorChar, '.')
                            .removeSuffix(".class")
                        if (isKotlinStdLibClass(className)) {
                            suggestions.add(className)
                        }
                    }
                }
            } else if (file.extension == "jar") {
                try {
                    val jarFile = java.util.jar.JarFile(file)
                    jarFile.entries().asSequence().forEach { entry ->
                        if (entry.name.endsWith(".class")) {
                            val className = entry.name
                                .replace('/', '.')
                                .removeSuffix(".class")
                            if (isKotlinStdLibClass(className)) {
                                suggestions.add(className)
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Error reading JAR file: " + e.message)
                }
            }
        }

        /**
         * Retrieves the classpath URLs of the running JVM.
         *
         * @return A list of URLs representing the classpath.
         */
        private fun getClassPathURLs(): List<URL> {
            val classPathURLs = mutableListOf<URL>()

            try {
                val classpath = System.getProperty("java.class.path")
                classpath.split(File.pathSeparator).forEach { path ->
                    val file = File(path)
                    if (file.exists()) {
                        classPathURLs.add(file.toURI().toURL())
                    }
                }
            } catch (e: Exception) {
                println("Error getting classpath URLs: " + e.message)
            }

            return classPathURLs
        }

        /**
         * Suggests import statements based on the input string.
         *
         * @param input The search input string.
         * @param numberSuggestions The maximum number of suggestions to return (-1 for all suggestions).
         */
        fun suggestImports(input: String, numberSuggestions: Int = -1) {
            val classPathURLs = getClassPathURLs()
            val suggestions = mutableListOf<String>()

            for (url in classPathURLs) {
                if (url.protocol == "file") {
                    val file = File(url.toURI())
                    if (file.exists()) {
                        findClasses(file, suggestions)
                    }
                }
            }

            if (suggestions.isNotEmpty()) {
                val matchComparator = Comparator<String> { a, b ->
                    matchScore(input, a).compareTo(matchScore(input, b))
                }

                val sortedSuggestions = suggestions.sortedWith(matchComparator)

                if (numberSuggestions == -1) {
                    sortedSuggestions.forEach { e ->
                        println(e)
                    }
                } else {
                    var i = 0
                    while (i < numberSuggestions && i < sortedSuggestions.size) {
                        println(sortedSuggestions[i])
                        i += 1
                    }
                }
            }
        }
    }
}