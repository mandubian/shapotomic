
/*
 * Copyright 2012 Pascal Voitot (@mandubian)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package object `shapotomic` {
  import shapeless._
  import datomisca._
  import Datomic._

  /** Polymorphic function checking HList is correctly typed against an existing schema 
    * + Converting it into PartialAddEntity/AddEntity
    */
  object SchemaCheckerFromHList extends Poly2{
    /* This one is for being able to write SchemaCheckerFromHList(HNil, HNil) */
    implicit def caseHNilHNilObj = at[HNil.type, HNil.type]{ (h, s) => PartialAddEntity.empty }

    implicit def caseHNilHNil = at[HNil, HNil]{ (h, s) => PartialAddEntity.empty }
    
    implicit def caseTempId[L <: HList, SL <: HList]
                           (implicit pull: Pullback2[L, SL, PartialAddEntity]) 
                  : Pullback2[ TempId :: L, SL, AddEntity] = 
      at[TempId :: L, SL]{ (hl, s) =>
        AddEntity(hl.head, SchemaCheckerFromHList(hl.tail, s))
      }

    implicit def caseHList[H, DD <: DatomicData, L <: HList, SL <: HList, Card <: Cardinality]
                          (implicit a2pw: Attribute2PartialAddEntityWriter[DD, Card, H],
                                    pull: Pullback2[L, SL, PartialAddEntity])
                  : Pullback2[ H :: L, Attribute[DD, Card] :: SL, PartialAddEntity] = 
      at[H :: L, Attribute[DD, Card] :: SL]{ (hl, sl) =>
        a2pw.convert(sl.head).write(hl.head) ++
        SchemaCheckerFromHList(hl.tail, sl.tail)
      }

    /** overloading for RawAttribute as HList is not covariant (hopefully :)) */
    implicit def caseHListRawAttr[H, DD <: DatomicData, L <: HList, SL <: HList, Card <: Cardinality]
                                 (implicit a2pw: Attribute2PartialAddEntityWriter[DD, Card, H],
                                           pull: Pullback2[L, SL, PartialAddEntity])
                  : Pullback2[ H :: L, RawAttribute[DD, Card] :: SL, PartialAddEntity] = 
      at[H :: L, RawAttribute[DD, Card] :: SL]{ (hl, sl) =>
        a2pw.convert(sl.head).write(hl.head) ++
        SchemaCheckerFromHList(hl.tail, sl.tail)
      }
    
  }

  /** Polymorphic function extracting typed fields from DEntity using an existing schema
    * + Converting it into correctly typed HList
    */
  object SchemaCheckerFromDEntity extends Poly2{
    import DatomicMapping._

    implicit def caseHNil = at[DEntity, HNil]{ (entity, s) => HNil }

    implicit def caseHList[H, DD <: DatomicData, HL <: HList, SL <: HList, Card <: Cardinality]
      (implicit a2er: Attribute2EntityReader[DD, Card, H],
                pull: Pullback2[DEntity, SL, HL]): 
      Pullback2[DEntity, Attribute[DD, Card] :: SL, H :: HL] =
        at[DEntity, Attribute[DD, Card] :: SL] { (entity, sl) =>
          val attr = sl.head
          val h: H = entity(attr)
          h :: SchemaCheckerFromDEntity(entity, sl.tail)
        }

    /** overloading for RawAttribute as HList is not covariant (hopefully :)) */
    implicit def caseHListRawAttribute[H, DD <: DatomicData, HL <: HList, SL <: HList, Card <: Cardinality]
      (implicit a2er: Attribute2EntityReader[DD, Card, H],
                pull: Pullback2[DEntity, SL, HL]): 
      Pullback2[DEntity, RawAttribute[DD, Card] :: SL, H :: HL] =
        at[DEntity, RawAttribute[DD, Card] :: SL] { (entity, sl) =>
          val attr = sl.head
          val h: H = entity(attr)
          h :: SchemaCheckerFromDEntity(entity, sl.tail)
        }
  }

  /** add toAddEntity to HList to statically validate HList types against Schema */
  implicit class HListToAddEntityOps[HL <: HList](hl: HL) {
    def toAddEntity[Schema <: HList](schema: Schema)
        (implicit pull: SchemaCheckerFromHList.Pullback2[HL, Schema, AddEntity]) : AddEntity = 
        SchemaCheckerFromHList(hl, schema)
  }

  /** add toHList to DEntity to convert a DEntity into HList  */
  implicit class DEntityToHListOps(entity: DEntity) {
    def toHList[HL <: HList, Schema <: HList](schema: Schema)
        (implicit pull: SchemaCheckerFromDEntity.Pullback2[DEntity, Schema, HL]) : Long :: HL = 
        entity.id :: SchemaCheckerFromDEntity(entity, schema)
  }


  /** A Shapeless Field wrapping a Datomisca Schema Attribute for building AddEntity
    * using Record HList
    */
  case class DField[DD <: DatomicData, Card <: Cardinality, A](attr: Attribute[DD, Card])
    (implicit attrC: Attribute2EntityReader[DD, Card, A]) extends Field[A]


}