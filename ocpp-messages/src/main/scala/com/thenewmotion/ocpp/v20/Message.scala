package com.thenewmotion.ocpp
package v20

import java.time.Instant
import enums.reflection.EnumUtils.{Enumerable, Nameable}

trait Message extends messages.Message

trait Request extends Message with messages.Request
trait Response extends Message with messages.Response

trait CsRequest extends Request
trait CsResponse extends Response
trait CsmsRequest extends Request
trait CsmsResponse extends Response

case class BootNotificationRequest(
  chargingStation: ChargingStation,
  reason: BootReason
) extends CsmsRequest

sealed trait BootReason extends Nameable
case object BootReason extends Enumerable[BootReason] {
  case object ApplicationReset extends BootReason
  case object FirmwareUpdate extends BootReason
  case object LocalReset extends BootReason
  case object PowerUp extends BootReason
  case object RemoteReset extends BootReason
  case object ScheduledReset extends BootReason
  case object Triggered extends BootReason
  case object Unknown extends BootReason
  case object Watchdog extends BootReason

  val values = List(ApplicationReset, FirmwareUpdate, LocalReset, PowerUp,
                    RemoteReset, ScheduledReset, Triggered, Unknown, Watchdog
  )
}


case class ChargingStation(
  serialNumber: Option[String],
  model: String,
  modem: Option[Modem],
  vendorName: String,
  firmwareVersion: Option[String]
)

case class Modem(
  iccid: Option[String],
  imsi: Option[String]
)

case class BootNotificationResponse(
  currentTime: Instant,
  interval: Int,
  status: BootNotificationStatus
) extends CsmsResponse

sealed trait BootNotificationStatus extends Nameable
case object BootNotificationStatus extends Enumerable[BootNotificationStatus] {
  case object Accepted extends BootNotificationStatus
  case object Pending extends BootNotificationStatus
  case object Rejected extends BootNotificationStatus

  val values = List(Accepted, Pending, Rejected)
}
