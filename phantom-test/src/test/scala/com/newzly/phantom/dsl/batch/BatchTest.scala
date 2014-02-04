package com.newzly.phantom.dsl.batch

import com.newzly.phantom.helper.BaseTest
import org.scalatest.{Assertions, Matchers}
import org.scalatest.concurrent.{PatienceConfiguration, AsyncAssertions}
import com.newzly.phantom.tables.{PrimitivesJoda, JodaRow}
import org.scalatest.time.SpanSugar._
import com.newzly.phantom.helper.AsyncAssertionsHelper._
import com.newzly.phantom.batch.BatchStatement
import org.joda.time.DateTime

class BatchTest extends BaseTest with Matchers with Assertions with AsyncAssertions {
  val keySpace: String = "BatchTestSpace"
  implicit val s: PatienceConfiguration.Timeout = timeout(10 seconds)

  it should "work fine" in {
    val row = JodaRow.sample
    val row2 = JodaRow.sample.copy(pkey = row.pkey)
    val row3 = JodaRow.sample
    PrimitivesJoda.insertSchema(session)
    val statement1 = PrimitivesJoda.insert
        .value(_.pkey, row.pkey)
        .value(_.intColumn, row.int)
        .value(_.timestamp, row.bi)
    val statement2 = PrimitivesJoda.insert
      .value(_.pkey, row3.pkey)
      .value(_.intColumn, row3.int)
      .value(_.timestamp, row3.bi)
    val statement3 = PrimitivesJoda.update
      .where(_.pkey eqs row2.pkey)
      .modify(_.intColumn, row2.int)
      .modify(_.timestamp, row2.bi)
    val statement4 = PrimitivesJoda.delete
      .where(_.pkey eqs row3.pkey)

    val batch = new BatchStatement().add(statement3).add(statement4)

    val w = for {
      s1 <- statement1.execute()
      s3 <- statement2.execute()
      b <- batch.execute()
      updated <- PrimitivesJoda.select.where(_.pkey eqs row.pkey).execute()
      deleted <- PrimitivesJoda.select.where(_.pkey eqs row3.pkey).execute()
    } yield (updated, deleted)

    w successful {
      case res =>
        assert(PrimitivesJoda.fromRow(res._1.one()) === row2)
        assert(res._2.all().isEmpty)
    }
  }
}
