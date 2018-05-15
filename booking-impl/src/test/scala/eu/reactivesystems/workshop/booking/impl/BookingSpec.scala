package eu.reactivesystems.workshop.booking.impl

import java.time.LocalDate
import java.util.UUID

import akka.actor.ActorSystem
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import eu.reactivesystems.workshop.booking.api.BookingRequest
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._

class BookingSpec extends WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with TypeCheckedTripleEquals {

  val system = ActorSystem("BookingSpec", JsonSerializerRegistry.actorSystemSetupFor(BookingSerializerRegistry))

  override def afterAll(): Unit = {
    Await.ready(system.terminate, 10.seconds)
  }

  "Booking entity" must {
    "handle RequestBooking" in {
      val driver = new PersistentEntityTestDriver(system, new BookingRegister, "booking-1")
      val guestId = UUID.randomUUID()
      val date = LocalDate.now().plusWeeks(2)
      val commandPayload = BookingRequest(guestId, date, 10, 2)
      val command = RequestBooking(commandPayload)

      val outcome = driver.run(command)
      val uuid = outcome.replies.head.asInstanceOf[UUID]

      // weird test case, UUID would (should) actually be different every time
      outcome.events should be(Seq(BookingRequested(uuid, commandPayload)))
      outcome.state should be(BookingRegisterState(
        BookingRegisterStatus.Listed,
        Map(uuid.toString -> Booking(uuid, guestId, date, 10, 2))
      ))
      outcome.issues should be(empty)
    }
  }

}
