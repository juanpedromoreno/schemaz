package schemaz

package generic
import scalaz.{ Alt, Decidable, \/, ~> }
import recursion._

trait GenericSchemaModule[R <: Realisation] extends SchemaModule[R] {

  def discardingFieldLabel[H[_]]: Field[H, ?] ~> H = λ[Field[H, ?] ~> H](field => field.schema)

  def discardingBranchLabel[H[_]]: Branch[H, ?] ~> H = λ[Branch[H, ?] ~> H](branch => branch.schema)

  def covariantTargetFunctor[H[_]](
    primNT: R.Prim ~> H,
    seqNT: H ~> λ[X => H[List[X]]],
    prodLabelNT: Field[H, ?] ~> H,
    sumLabelNT: Branch[H, ?] ~> H,
    delay: λ[X => () => H[X]] ~> H
  )(implicit H: Alt[H]): HAlgebra[RSchema, H] =
    new (RSchema[H, ?] ~> H) {

      def apply[A](schema: RSchema[H, A]): H[A] =
        schema match {
          case PrimSchemaF(prim)         => primNT(prim)
          case x: Sum[H, a, b]           => H.either2(x.left, x.right)
          case x: Prod[H, a, b]          => H.tuple2(x.left, x.right)
          case x: IsoSchema[H, a0, a]    => H.map(x.base)(x.iso.get)
          case x: Record[H, a]           => x.fields
          case x: Sequence[H, a]         => seqNT(x.element)
          case pt: Field[H, a]           => prodLabelNT(pt)
          case x: Union[H, a]            => x.choices
          case st: Branch[H, a]          => sumLabelNT(st)
          case _: ROne[H]                => H.pure(())
          case ref @ SelfReference(_, _) => delay(() => ref.unroll)
        }
    }

  def contravariantTargetFunctor[H[_]](
    primNT: R.Prim ~> H,
    seqNT: H ~> λ[X => H[List[X]]],
    prodLabelNT: Field[H, ?] ~> H,
    sumLabelNT: Branch[H, ?] ~> H,
    delay: λ[X => () => H[X]] ~> H
  )(implicit H: Decidable[H]): HAlgebra[RSchema, H] =
    new (RSchema[H, ?] ~> H) {

      def apply[A](schema: RSchema[H, A]): H[A] =
        schema match {
          case PrimSchemaF(prim) => primNT(prim)
          case x: Prod[H, a, b]  => H.divide(x.left, x.right)(identity[(a, b)])
          case x: Sum[H, a, b]   => H.choose(x.left, x.right)(identity[a \/ b])
          //UHOH THOSE BOTH COMPILE?! (for the love of all that is precious to you, please leave the pattern matches that actually bind the type variables)
          //case IsoSchema(base, iso)      => H.contramap(base)(iso.get)
          //case IsoSchema(base, iso)      => H.contramap(base)(iso.reverseGet)
          //Luckily does not compile
          //case x: IsoSchema[_, a, a0]    => H.contramap(x.base)(x.iso.get)
          case x: IsoSchema[H, a, a0]    => H.contramap(x.base)(x.iso.reverseGet)
          case x: Record[H, a]           => x.fields
          case x: Sequence[H, a]         => seqNT(x.element)
          case pt: Field[H, a]           => prodLabelNT(pt)
          case x: Union[H, a]            => x.choices
          case st: Branch[H, a]          => sumLabelNT(st)
          case _: ROne[H]                => H.xproduct0[Unit](())
          case ref @ SelfReference(_, _) => delay(() => ref.unroll)
        }
    }
}
