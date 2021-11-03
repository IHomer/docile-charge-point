package chargepoint.docile
package dsl
package ocpp20transactions

import java.time.Instant
import java.util.UUID
import scala.collection.mutable
import com.thenewmotion.ocpp.VersionFamily
import com.thenewmotion.ocpp.messages.v20._

/**
  * DSL operations for OCPP 2.0 transactions.
  *
  * In order to easily do this in a way that is consistent with the rules of
  * OCPP 2.0 transactions, this trait keeps local state per EVSE in a mutable
  * map. It thus remembers which transactions you started, and their IDs and
  * states.
  *
  * Note that the methods return a request, but do not send it. This is so
  * that Docile scripts can send transaction methods in a different order from
  * the order in which they happened on the simulated charge point. That is
  * something that you'll want to test I guess :-)
  *
  * While they don't send the request, they are otherwise side-effecting wrt the
  * simulated charge point's state: they change the state of the EVSE's
  * transaction message counter and update fields in the transaction object for
  * use in the next method call on it.
  */
trait Ops {

  self: CoreOps[
    VersionFamily.V20.type,
    CsmsRequest,
    CsmsResponse,
    CsmsReqRes,
    CsRequest,
    CsResponse,
    CsReqRes
    ] =>

  val transactionMessageCounters: mutable.Map[Int, Int] = mutable.Map.empty[Int, Int]

  def startTransactionAtAuthorized(
    evseId: Int = 1,
    connectorId: Int = 1,
    idToken: IdToken = defaultIdToken
  ): (StartedTransaction, TransactionEventRequest) = {
    val transactionInfo = Transaction(
      transactionId = UUID.randomUUID().toString,
      chargingState = None,
      timeSpentCharging = None,
      stoppedReason = None,
      remoteStartId = None
    )

    startTransaction(
      transactionInfo,
      TriggerReason.Authorized,
      Some(idToken),
      evseId,
      connectorId
    )
  }

  def startTransactionAtCablePluggedIn(
    evseId: Int = 1,
    connectorId: Int = 1
  ): (StartedTransaction, TransactionEventRequest) = {
    val transactionInfo = Transaction(
      transactionId = UUID.randomUUID().toString,
      chargingState = Some(ChargingState.EVDetected),
      timeSpentCharging = Some(0),
      stoppedReason = None,
      remoteStartId = None
    )

    startTransaction(
      transactionInfo,
      TriggerReason.CablePluggedIn,
      idToken = None,
      evseId = evseId,
      connectorId = connectorId
    )
  }

  def startTransaction(
    transactionInfo: Transaction,
    triggerReason: TriggerReason,
    idToken: Option[IdToken] = None,
    evseId: Int = 1,
    connectorId: Int = 1
  ): (StartedTransaction, TransactionEventRequest) = {
    val seqNo = getAndIncrementTxCounter(evseId)

    val startedTx = new StartedTransaction(transactionInfo, evseId, connectorId)
    val req = TransactionEventRequest(
      seqNo = seqNo,
      evse = Some(EVSE(evseId, Some(connectorId))),
      eventType = TransactionEvent.Started,
      triggerReason = triggerReason,
      transactionInfo = transactionInfo,
      meterValue = Some(List(
        MeterValue(List(
          SampledValue(
            value = BigDecimal(0.0),
            context = Some(ReadingContext.TransactionBegin),
            measurand = None,
            phase = None,
            location = None,
            signedMeterValue = None,
            unitOfMeasure = None
          )),
          Instant.now()
        )
      )),
      timestamp = Instant.now(),
      offline = None,
      numberOfPhasesUsed = None,
      cableMaxCurrent = None,
      reservationId = None,
      idToken = idToken
    )

    (startedTx, req)
  }

  class StartedTransaction private[Ops] (var data: Transaction, val evseId: Int, val connectorId: Int) {

    /** Assuming there's an unauthorized transaction going on on an EVSE already,
      * notify the CSMS that that transaction has been authorized.
      *
      * Will fail the test if no transaction is going on on that EVSE.
      *
      * @param evseId
      * @param connectorId
      */
    def authorize(
      idToken: IdToken = defaultIdToken
    ): TransactionEventRequest = {
      data = data.copy(chargingState = Some(ChargingState.Charging))

      update(data, TriggerReason.Authorized, Some(idToken))
    }

    def plugInCable(): TransactionEventRequest = {
      data = data.copy(chargingState = Some(ChargingState.EVDetected))

      update(data, TriggerReason.CablePluggedIn, idToken = None)
    }

    def startEnergyOffer(): TransactionEventRequest = {
      data = data.copy(chargingState = Some(ChargingState.Charging))

      update(data, TriggerReason.ChargingStateChanged)
    }

    def suspendOnDeauthorization(): TransactionEventRequest = {
      data = data.copy(chargingState = Some(ChargingState.SuspendedEVSE))

      update(data, triggerReason = TriggerReason.Deauthorized)
    }

    def suspendOnEvCommunicationLoss(): TransactionEventRequest = {
      data = data.copy(chargingState = Some(ChargingState.SuspendedEVSE))

      update(data, triggerReason = TriggerReason.EVCommunicationLost)
    }

    def stopAuthorized(idToken: IdToken = defaultIdToken): TransactionEventRequest =
      update(data, triggerReason = TriggerReason.StopAuthorized, idToken = Some(idToken))

    def update(
      transactionInfo: Transaction,
      triggerReason: TriggerReason,
      idToken: Option[IdToken] = None,
      meterValue: Option[List[MeterValue]] = None
    ): TransactionEventRequest = {
      val seqNo = getAndIncrementTxCounter(evseId)
      TransactionEventRequest(
        seqNo = seqNo,
        eventType = TransactionEvent.Updated,
        triggerReason = triggerReason,
        transactionInfo = transactionInfo,
        idToken = idToken,
        meterValue = meterValue,
        timestamp = Instant.now(),
        offline = None,
        numberOfPhasesUsed = None,
        cableMaxCurrent = None,
        reservationId = None,
        evse = None
      )
    }

    def end(
      triggerReason: TriggerReason = TriggerReason.Deauthorized,
      stoppedReason: Reason = Reason.DeAuthorized,
      finalState: Option[ChargingState] = None
    ): TransactionEventRequest = {
      val seqNo = getAndIncrementTxCounter(evseId)
      data = data.copy(chargingState = None, stoppedReason = Some(stoppedReason))
      TransactionEventRequest(
        seqNo = seqNo,
        eventType = TransactionEvent.Ended,
        triggerReason = triggerReason,
        transactionInfo = data,
        idToken = None,
        meterValue = Some(List(
          MeterValue(List(
            SampledValue(
              value = BigDecimal(1234.567),
              context = Some(ReadingContext.TransactionEnd),
              measurand = None,
              phase = None,
              location = None,
              signedMeterValue = None,
              unitOfMeasure = None
            )),
            Instant.now())
        )),
        timestamp = Instant.now(),
        offline = None,
        numberOfPhasesUsed = None,
        cableMaxCurrent = None,
        reservationId = None,
        evse = None
      )
    }
  }

  private def getAndIncrementTxCounter(evseId: Int): Int =
    transactionMessageCounters.get(evseId) match {
      case None =>
        transactionMessageCounters.put(evseId, 1)
        0
      case Some(counter) =>
        transactionMessageCounters.update(evseId, counter + 1)
        counter
    }

  val defaultIdToken = IdToken(idToken = "01020304", `type` = IdTokenType.ISO14443, additionalInfo = None)
}
