package uk.co.sam_giles.finagled_batch

import com.twitter.finagle.Service
import com.twitter.util.Future

class BatchService[-Req, +Rep](mapper: (Req) => Future[Rep]) extends Service[Seq[Req], Seq[Rep]] {
  def apply(req: Seq[Req]): Future[Seq[Rep]] = {
 
    val responses = req map mapper
    
    Future.collect(responses)
  }
}