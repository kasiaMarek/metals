package example

import io.circe.derivation.deriveDecoder
import io.circe.derivation.deriveEncoder

class Definitions {
  Predef.any2stringadd/*[Int]*/(1)
  List[
    java.util.Map.Entry[
      java.lang.Integer,
      java.lang.Double,
    ]
  ](
    elems = null
  )
  println(deriveDecoder[MacroAnnotation])
  println(deriveEncoder[MacroAnnotation])
}