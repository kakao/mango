package com.kakao.mango

import scala.concurrent.ExecutionContext

package object elasticsearch {

  /** use the same ExecutionContext as mango http */
  implicit val context: ExecutionContext = com.kakao.mango.http.context

}
