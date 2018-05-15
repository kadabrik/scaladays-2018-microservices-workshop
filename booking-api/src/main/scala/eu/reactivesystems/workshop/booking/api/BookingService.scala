package eu.reactivesystems.workshop.booking.api

import java.util.UUID

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

/**
  * The booking service.
  */
trait BookingService extends Service {

  def healthCheck(): ServiceCall[NotUsed, String]

  def cancelBooking(roomId: UUID, bookingId: UUID): ServiceCall[NotUsed, Done]

  def requestBooking(roomId: UUID): ServiceCall[BookingRequest, UUID]

  def confirmBooking(roomId: UUID, bookingId: UUID): ServiceCall[NotUsed, Done]

  def rejectBooking(roomId: UUID, bookingId: UUID): ServiceCall[NotUsed, Done]

  def withdrawBooking(roomId: UUID, bookingId: UUID): ServiceCall[NotUsed, Done]

  def modifyBooking(roomId: UUID, bookingId: UUID): ServiceCall[BookingRequest, Done]

  def listRoom(roomId: UUID): ServiceCall[NotUsed, Done]

  def unlistRoom(roomId: UUID): ServiceCall[NotUsed, Done]

  final override def descriptor = {
    import Service._

    named("booking")
      .withCalls(restCall(Method.GET, "/api/booking/healthCheck", healthCheck),
        restCall(Method.POST,"/api/rooms/:roomId/bookings", requestBooking _),
        restCall(Method.DELETE,"/api/rooms/:roomId/bookings/:bookingId", cancelBooking _),
        restCall(Method.POST, "/api/rooms/:roomId/bookings/:bookingId/confirm", confirmBooking _),
        restCall(Method.POST, "/api/rooms/:roomId/bookings/:bookingId/reject", rejectBooking _),
        restCall(Method.POST, "/api/rooms/:roomId/bookings/:bookingId/withdraw", withdrawBooking _),
        restCall(Method.POST, "/api/rooms/:roomId/bookings/:bookingId", modifyBooking _),
        restCall(Method.POST, "/api/rooms/:roomId", listRoom _),
        restCall(Method.DELETE, "/api/rooms/:roomId", unlistRoom _)
      )
      .withAutoAcl(true)
  }
}
