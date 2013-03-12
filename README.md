# Shapeless HList Schema-Type-Safe conversion to Datomisca/Datomic Entities

#### Datomisca is a Scala API for Datomic DB

If you want to know more about Datomisca/Datomic schema go to my [recent article](http://www.mandubian.com/2013-03-04-datomisa-schema.html). What's interesting with Datomisca schema is that they are statically typed allowing some compiler validations and type inference.

#### [Shapeless HList](https://github.com/milessabin/shapeless) are heterogenous polymorphic lists 

HList are able to contain different types of data and able to keep tracks of these types.

<br/>
This project is an experience trying to :

- convert HList to/from Datomic Entities
- check HList types against schema at compile-time

This uses :

- Datomisca type-safe schema
- Shapeless HList
- Shapeless polymorphic functions

Please note that we don't provide any `Iso[From, To]` since there is no isomorphism here.
Actually, there are 2 monomorphisms (injective):

- `HList   => AddEntity` to provision an entity 
- `DEntity => HList` when retrieving entity

We would need to implement `Mono[From, To]` certainly for our case...

## Code sample

### Create schema based on `HList`

```scala
// Koala Schema
object Koala {
  object ns {
    val koala = Namespace("koala")
  }

  // schema attributes
  val name        = Attribute(ns.koala / "name", SchemaType.string, Cardinality.one).withDoc("Koala's name")
  val age         = Attribute(ns.koala / "age", SchemaType.long, Cardinality.one).withDoc("Koala's age")
  val trees       = Attribute(ns.koala / "trees", SchemaType.string, Cardinality.many).withDoc("Koala's trees")

  // the schema in HList form
  val schema = name :: age :: trees :: HNil

  // the datomic facts corresponding to schema 
  // (need specifying upper type for shapeless conversion to list)
  val txData = schema.toList[Operation]
}

// Provision schema
Datomic.transact(Koala.txData) map { tx => ... }
```

### Validate `HList` against Schema

```scala
// creates a Temporary ID & keeps it for resolving entity after insertion
val id = DId(Partition.USER)
// creates an HList entity 
val hListEntity = 
  id :: "kaylee" :: 3L :: 
  Set( "manna_gum", "tallowwood" ) :: 
  HNil

// validates and converts at compile-time this HList against schema
hListEntity.toAddEntity(Koala.schema)

// If you remove a field from HList and try again, the compiler fails
val badHListEntity = 
  id :: "kaylee" :: 
  Set( "manna_gum", "tallowwood" ) :: 
  HNil

scala> badHListEntity.toAddEntity(Koala.schema)
<console>:23: error: could not find implicit value for parameter pull: shapotomic.SchemaCheckerFromHList.Pullback2[shapeless.::[datomisca.TempId,shapeless.::[String,shapeless.::[scala.collection.immutable.Set[String],shapeless.HNil]]],shapeless.::[datomisca.RawAttribute[datomisca.DString,datomisca.CardinalityOne.type],shapeless.::[datomisca.RawAttribute[datomisca.DLong,datomisca.CardinalityOne.type],shapeless.::[datomisca.RawAttribute[datomisca.DString,datomisca.CardinalityMany.type],shapeless.HNil]]],datomisca.AddEntity]
```

### Convert `DEntity` to static-typed `HList` based on schema

```scala
val e = Datomic.resolveEntity(tx, id)

// rebuilds HList entity from DEntity statically typed by schema
// Explicitly typing the val to show that the compiler builds the right HList from schema
val postHListEntity = e.toHList(Koala.schema)
val validateHListEntityType: Long :: String :: Long :: Set[String] :: HNil = postHListEntity
```

## Conclusion

Using `HList` with compile-time schema validation is quite interesting because it provides a very basic and versatile data structure to manipulate Datomic entities in a type-safe style.

Moreover, as Datomic pushes atomic data manipulation (simple facts instead of full entities), it's really cool to use `HList` instead of static structure such as case-class. For ex:

```scala
val simplerOp = (id :: "kaylee" :: 5L).toAddEntity(Koala.name :: Koala.age :: HNil)
```

Have TypedFun