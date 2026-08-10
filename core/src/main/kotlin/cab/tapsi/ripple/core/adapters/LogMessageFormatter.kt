package cab.tapsi.ripple.core.adapters

/**
 * Formats SDK log messages consistently across logger adapters.
 */
object LogMessageFormatter {
    fun format(message: String, vararg args: Any?): String {
        if (args.isEmpty()) return message

        return buildString {
            append(message)
            append(" ")
            append(args.joinToString(" ") { it.toString() })
        }
    }
}
