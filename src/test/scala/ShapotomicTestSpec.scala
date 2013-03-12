
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
import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.{Step, Fragments}

import scala.concurrent._
import scala.concurrent.duration.Duration


// import play.api.libs.json._
// import play.api.libs.functional._
// import play.api.libs.functional.syntax._

import datomisca.executioncontext.ExecutionContextHelper._

class ShapotomicSpec extends Specification {
  sequential

  import shapeless._

  import datomisca._
  import Datomic._

  import shapotomic._

  // Datomic URI definition
  val uri = "datomic:mem://shapotomisca"

  // Datomic Connection as an implicit in scope
  implicit lazy val conn = Datomic.connect(uri)

  // Koala Schema
  object Koala {
    object ns {
      val koala = Namespace("koala")
    }

    // schema attributes
    val name        = Attribute(ns.koala / "name", SchemaType.string, Cardinality.one).withDoc("Koala's name")
    val age         = Attribute(ns.koala / "age", SchemaType.long, Cardinality.one).withDoc("Koala's age")
    val trees       = Attribute(ns.koala / "trees", SchemaType.string, Cardinality.many).withDoc("Koala's trees")
    
    // Draft playing with Datomisca and Shapeless Fields/Records
    val fieldName   = DField(name)
    val fieldAge    = DField(age)
    val fieldTrees  = DField(trees)

    // the schema in HList form
    val schema = name :: age :: trees :: HNil

    // the datomic facts corresponding to schema 
    // (need specifying upper type for shapeless conversion to list)
    val txData = schema.toList[Operation]
  }

  def startDB = {
    println(s"Creating DB with uri $uri: "+ createDatabase(uri))

    Await.result(
      Datomic.transact(Koala.txData),
      Duration("2 seconds")
    )
  } 

  def stopDB = {
    deleteDatabase(uri)
    println("Deleted DB")
    defaultExecutorService.shutdownNow()
  }

  override def map(fs: => Fragments) = Step(startDB) ^ fs ^ Step(stopDB)

  "shapotomic" should {
    
    "convert HList to Datomic Facts & Datomic Entities from HList" in {
      // creates a Temporary ID & keeps it for resolving entity after insertion
      val id = DId(Partition.USER)
      // creates an HList entity 
      val hListEntity = 
        id :: "kaylee" :: 3L :: 
        Set( "manna_gum", "tallowwood" ) :: 
        HNil

      // builds Datomisca Entity facts statically checking at compile-time HList against Schema
      val txData = hListEntity.toAddEntity(Koala.schema)

      // inserts data into Datomic
      val tx = Datomic.transact(txData) map { tx =>
        // resolves real DEntity from temporary ID
        val e = Datomic.resolveEntity(tx, id)

        // rebuilds HList entity from DEntity statically typed by schema
        // Explicitly typing the val to show that the compiler builds the right HList from schema
        val postHListEntity: Long :: String :: Long :: Set[String] :: HNil = e.toHList(Koala.schema)

        postHListEntity must beEqualTo( e.id :: "kaylee" :: 3L :: Set( "manna_gum", "tallowwood" ) :: HNil )
      }
      
      Await.result(tx, Duration("3 seconds"))

      success

    }

    "Draft test: Field/Record" in {
      import Record._

      val koala = 
        Koala.fieldName   -> "kaylee"                         ::
        Koala.fieldAge    -> 3L                               ::
        Koala.fieldTrees  -> Set( "manna_gum", "tallowwood" ) ::
        HNil

      println("name:"+koala.get(Koala.fieldName))
      println("age:"+koala.get(Koala.fieldAge))

      success
    }

  }

}
