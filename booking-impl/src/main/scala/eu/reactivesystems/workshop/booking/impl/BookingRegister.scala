package eu.reactivesystems.workshop.booking.impl

import java.time.LocalDate
import java.util.UUID

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger, PersistentEntity}
import eu.reactivesystems.workshop.booking.api.BookingRequest
import eu.reactivesystems.workshop.jsonformats.JsonFormats._
import play.api.libs.json.{Format, Json}

/**
  */
class BookingRegister extends PersistentEntity {

  override type State = BookingRegisterState
  override type Command = BookingRegisterCommand
  override type Event = BookingRegisterEvent

  override def initialState: BookingRegisterState = BookingRegisterState(BookingRegisterStatus.Listed, Map.empty)


  override def behavior: Behavior = {
    case BookingRegisterState(BookingRegisterStatus.Unlisted, _) => unlisted
    case BookingRegisterState(BookingRegisterStatus.Listed, _) => listed
  }

  /**
    * Behavior for the not created state.
    */
  private def unlisted = Actions().onCommand[ListRoom.type, Done] {
    case (ListRoom, ctx, state) =>
      ctx.thenPersist(RoomListed)(event => ctx.reply(Done))
  }

  private def listed = Actions()
    .onCommand[RequestBooking, UUID] {
      case (RequestBooking(bookingRequest), ctx, state) =>
        val bookingId = UUID.randomUUID()
        // some validation of bookingRequest could be done here, if/else is just an example
        if (bookingRequest.startingDate.isBefore(LocalDate.now())) {
          ctx.invalidCommand("Booking date has to be in the future")
          ctx.done
        } else {
          ctx.thenPersist(BookingRequested(bookingId, bookingRequest))(event => ctx.reply(event.bookingId))
        }
    }
    .onCommand[UnlistRoom.type, Done] {
      case (UnlistRoom, ctx, state) =>
        ctx.reply(Done)
        ctx.done
      }
    .onCommand[CancelBooking, Done] {
      case (CancelBooking(bookingId), ctx, state) =>
        state.requestedBookings.get(bookingId.toString).fold {
          ctx.invalidCommand("No such booking")
          ctx.done
        } ( _ => ctx.thenPersist(BookingCancelled(bookingId))(event => ctx.reply(Done)))
    }
    .onEvent{
      case (BookingRequested(bookingId, bookingData), state) => {
        state.copy(requestedBookings = state.requestedBookings +
          (bookingId.toString -> Booking(
            bookingId,
            bookingData.guest,
            bookingData.startingDate,
            bookingData.duration,
            bookingData.numberOfGuests
          )))
      }
    }
    .onEvent{
      case (BookingCancelled(bookingId), state) =>
        state.copy(requestedBookings = state.requestedBookings - bookingId.toString)
    }

  //private def cancelAction =

}


/**
  * The state.
  */
case class BookingRegisterState(status: BookingRegisterStatus.Status, requestedBookings: Map[String, Booking])

object BookingRegisterState {
  implicit val format: Format[BookingRegisterState] = Json.format
}

// we could have reused BookingRequest, but to decouple things we have redundancy
// 'P' prefix might be used to differentiate persistent events and external API events
case class Booking(
  id: UUID,
  guest: UUID,
  startingDate: LocalDate,
  duration: Int,
  numberOfGuests: Int
)

object Booking {
  implicit val format: Format[Booking] = Json.format
}

/**
  * Status.
  */
object BookingRegisterStatus extends Enumeration {
  type Status = Value
  val Unlisted, Listed = Value

  implicit val format: Format[Status] = enumFormat(BookingRegisterStatus)
}

/**
  * A command.
  */
sealed trait BookingRegisterCommand

case class RequestBooking(request: BookingRequest) extends BookingRegisterCommand with ReplyType[UUID]
case class CancelBooking(id: UUID) extends BookingRegisterCommand with ReplyType[Done]
case class ConfirmBooking(id: UUID) extends BookingRegisterCommand with ReplyType[Done]
case class RejectBooking(id: UUID) extends BookingRegisterCommand with ReplyType[Done]
case class WithdrawBooking(id: UUID) extends BookingRegisterCommand with ReplyType[Done]
case class ModifyBooking(request: BookingRequest) extends BookingRegisterCommand with ReplyType[Done]

case object ListRoom extends BookingRegisterCommand with ReplyType[Done] {
  implicit val format: Format[ListRoom.type] = singletonFormat(ListRoom)
}
case object UnlistRoom extends BookingRegisterCommand with ReplyType[Done] {
  implicit val format: Format[UnlistRoom.type] = singletonFormat(UnlistRoom)
}

object RequestBooking {
  implicit val format: Format[RequestBooking] = Json.format
}

object CancelBooking {
  implicit val format: Format[CancelBooking] = Json.format
}

object ConfirmBooking {
  implicit val format: Format[ConfirmBooking] = Json.format
}

object RejectBooking {
  implicit val format: Format[RejectBooking] = Json.format
}

//case object ListRoom {
//  implicit val format: Format[ListRoom.type] = singletonFormat((ListRoom))
//}


/**
  * A persisted event.
  */
trait BookingRegisterEvent extends AggregateEvent[BookingRegisterEvent] {
  override def aggregateTag: AggregateEventTagger[BookingRegisterEvent] = BookingRegisterEvent.Tag
}

object BookingRegisterEvent {
  val Tag = AggregateEventTag[BookingRegisterEvent]
}

case object RoomListed extends BookingRegisterEvent {
  implicit val format: Format[RoomListed.type] = singletonFormat(RoomListed)
}

case object RoomUnlisted extends BookingRegisterEvent {
  implicit val format: Format[RoomUnlisted.type] = singletonFormat(RoomUnlisted)
}

case class BookingRequested(bookingId: UUID, data: BookingRequest) extends BookingRegisterEvent

object BookingRequested {
  implicit val format: Format[BookingRequested] = Json.format
}

case class BookingCancelled(bookingId: UUID) extends BookingRegisterEvent

object BookingCancelled {
  implicit val format: Format[BookingCancelled] = Json.format
}
