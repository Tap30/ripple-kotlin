package cab.tapsi.ripple.core

import java.util.*

object IdGenerator {
    fun generate(): String = UUID.randomUUID().toString()
}
