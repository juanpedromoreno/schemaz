package schemaz

package tests

import monocle.Iso
import testz._

object SchemaModuleExamples {

  def tests[T](harness: Harness[T]): T = {
    import harness._

    val jsonModule = new JsonModule[JsonSchema.type] {
      override val R = JsonSchema
    }

    import jsonModule._

    section("Manipulating Schemas")(
      test("imap on IsoSchema shouldn't add new layer") { () =>
        val adminToListIso  = Iso[Admin, List[String]](_.rights)(Admin.apply)
        def listToSeqIso[A] = Iso[List[A], Seq[A]](_.toSeq)(_.toList)

        val adminRecord = "rights" -*>: seq(prim(JsonSchema.JsonString))

        val adminSchema = caseClass(
          adminRecord,
          Iso[List[String], Admin](Admin.apply)(_.rights)
        )

        adminSchema.imap(adminToListIso).imap(listToSeqIso).unFix match {
          case IsoSchemaF(base, _) => assert(base == record(adminRecord))
          case _                   => assert(false)
        }
      }
    )
  }
}
