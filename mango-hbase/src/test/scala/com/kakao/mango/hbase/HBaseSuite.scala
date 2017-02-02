package com.kakao.mango.hbase

import com.kakao.mango.MangoFunSuite
import com.kakao.shaded.hbase.async.CompareFilter.CompareOp
import com.kakao.shaded.hbase.async._

class HBaseSuite extends MangoFunSuite with TestHBase {

  test ("hbase key-value storage") {
    val table = hbase.table("test")
    val column = table.column("family", "qualifier")
    column.put("hello", "world").sync()
    column.get("hello").sync().get.value shouldBe "world"
  }

  test ("mango hbase scanning") {
    val table = hbase.table("test")

    val current = (System.currentTimeMillis()/1000 - 1*60*60).toString
    val start = current.substring(0,8)
    val end = current.substring(0,9)

    val f1: ScanFilter = new ColumnRangeFilter(start,end)
    val f2: ScanFilter = new ColumnPrefixFilter("p")
    val f3: ScanFilter = new QualifierFilter( CompareOp.GREATER_OR_EQUAL, new BinaryComparator(current.getBytes))

    table.scan(limit=1,filter=f1).await()
  }

}
