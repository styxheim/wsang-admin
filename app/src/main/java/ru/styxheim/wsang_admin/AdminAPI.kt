package ru.styxheim.wsang_admin

import com.squareup.moshi.JsonClass

object AdminAPI {
  @JsonClass(generateAdapter = true)
  data class Error (
    var Text: String = ""
  )

  @JsonClass(generateAdapter = true)
  data class Credentials (
    var Version: Short = 0,
    var TerminalString: String = "",
    var SecureKey: String = ""
  )

  @JsonClass(generateAdapter = true)
  open class AdminRequest(
    var Credentials: Credentials = Credentials(),
    var Competition: RaceStatus? = null
  )

  @JsonClass(generateAdapter = true)
  open class AdminResponse(
    val Error: Error?
  )

  @JsonClass(generateAdapter = true)
  data class Discipline (
    var Id: Int = 0,
    var Name: String = "",
    var Gates: MutableList<Int> = mutableListOf()
  )

  @JsonClass(generateAdapter = true)
  data class RaceStatus (
    val CompetitionId: Long = 0,
    var CompetitionName: String? = "",
    var SyncPoint: Long?,
    val TimeStamp: Long = 0,
    var Gates: MutableList<Int>? = mutableListOf(),
    var Penalties: MutableList<Int>? = mutableListOf(),
    var Crews: MutableList<Int>? = mutableListOf(),
    var Disciplines: MutableList<Discipline>? = mutableListOf(),
    val isActive: Boolean = false
  )

  @JsonClass(generateAdapter = true)
  data class CompetitionList (
    val Competitions: MutableList<RaceStatus> = mutableListOf(),
  ) : AdminResponse(Error = null)

  @JsonClass(generateAdapter = true)
  data class TerminalActivity(
    val LastActivity: Long
  )

  @JsonClass(generateAdapter = true)
  data class TerminalStatusShort(
    val TimeStamp: Long,
    val TerminalString: String,
    val Activity: TerminalActivity
  )

  @JsonClass(generateAdapter = true)
  data class TerminalList(
    val TerminalList: List<TerminalStatusShort> = mutableListOf()
  ) : AdminResponse(Error = null)
 }