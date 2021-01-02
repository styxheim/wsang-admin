package ru.styxheim.wsang_admin

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import java.io.IOException

class Transport(private val sharedPreferences: SharedPreferences) {
  private val mediaType = "application/json; charset=utf8".toMediaType()
  private val serverScheme: String = "http://"
  private var callCompetitionListGet: Call? = null
  private var callCompetitionSet: Call? = null
  private var callTerminalsGet: Call? = null
  private var callCompetitionGet: Call? = null
  private var callCompetitionTerminalsSet: Call? = null

  private val httpClient = OkHttpClient()
  private val moshi: Moshi = Moshi.Builder().build()
  private val adminRequestJsonAdapter = moshi.adapter(AdminAPI.AdminRequest::class.java)
  private val adminResponseJsonAdapter = moshi.adapter(AdminAPI.AdminResponse::class.java)
  private val competitionListJsonAdapter = moshi.adapter(AdminAPI.CompetitionList::class.java)
  private val getTerminalsJsonAdapter = moshi.adapter(AdminAPI.TerminalActivityList::class.java)
  private val getCompetitionJsonAdapter = moshi.adapter(AdminAPI.CompetitionResponse::class.java)

  private fun getCredentials(): AdminAPI.Credentials {
    val terminalString: String = sharedPreferences.getString("terminal_string", "")!!
    val secureKey: String = sharedPreferences.getString("secure_key", "")!!

    return AdminAPI.Credentials(TerminalString = terminalString, SecureKey = secureKey)
  }

  private fun <T : AdminAPI.AdminResponse, TReq : AdminAPI.AdminRequest> enqueue(
    previousCall: Call?,
    httpResource: String,
    apiRequest: TReq,
    responseJsonAdapter: (BufferedSource) -> T?,
    onBegin: () -> Unit,
    onEnd: () -> Unit,
    onFail: (message: String) -> Unit,
    onResult: (adminResponse: T) -> Unit
  ): Call? {
    val serverAddress: String = sharedPreferences.getString("server_address", null) ?: return null
    val body = adminRequestJsonAdapter.toJson(apiRequest).toString().toRequestBody(mediaType)
    val httpRequest = Request.Builder()
      .url("${serverScheme}${serverAddress}${httpResource}")
      .post(body)
      .build()
    val call = httpClient.newCall(httpRequest)

    previousCall?.let {
      onEnd()
      it.cancel()
    }

    onBegin()
    call.enqueue(object : Callback {
      override fun onResponse(call: Call, response: Response) {
        onEnd()
        when (response.code) {
          200 -> {
            responseJsonAdapter(response.body!!.source())?.let {
              (it as AdminAPI.AdminResponse).Error?.let { error ->
                onFail(error.Text)
              } ?: run {
                onResult(it)
              }
            } ?: run {
              onFail("received json not parsed: unknown error")
            }
          }
          else -> {
            onFail("response code: ${response.code} (expected 200)")
          }
        }
      }

      override fun onFailure(call: Call, e: IOException) {
        if (call.isCanceled()) return
        onEnd()
        onFail(e.toString())
      }
    })
    return call
  }

  fun getTerminalsActivities(
    onBegin: () -> Unit,
    onEnd: () -> Unit,
    onFail: (message: String) -> Unit,
    onResult: (terminalActivityList: AdminAPI.TerminalActivityList) -> Unit
  ) {
    val areq = AdminAPI.AdminRequest(Credentials = getCredentials())

    callTerminalsGet = enqueue(
      callTerminalsGet,
      "/api/admin/terminal/list",
      areq,
      { source -> getTerminalsJsonAdapter.fromJson(source) },
      onBegin = onBegin,
      onEnd = onEnd,
      onFail = onFail,
      onResult = onResult
    )
  }

  fun getCompetition(
    competitionId: Long,
    onBegin: () -> Unit,
    onEnd: () -> Unit,
    onFail: (message: String) -> Unit,
    onResult: (terminalList: AdminAPI.CompetitionResponse) -> Unit
  ) {
    val areq = AdminAPI.AdminRequest(Credentials = getCredentials())

    callCompetitionGet =
      enqueue(
        callCompetitionGet,
        "/api/admin/comeptition/get/${competitionId}",
        areq,
        { source -> getCompetitionJsonAdapter.fromJson(source) },
        onBegin,
        onEnd,
        onFail,
        onResult
      )
  }

  fun setCompetitionTerminals(
    competitionId: Long,
    terminals: MutableList<AdminAPI.TerminalStatus>,
    onBegin: () -> Unit,
    onEnd: () -> Unit,
    onFail: (message: String) -> Unit,
    onResult: () -> Unit
  ) {
    val areq = AdminAPI.Request.TerminalsSet(TerminalList = terminals)

    areq.Credentials = getCredentials()
    callCompetitionTerminalsSet = enqueue(
      callCompetitionTerminalsSet,
      "/api/admin/competition/set/${competitionId}",
      areq,
      { source -> adminResponseJsonAdapter.fromJson(source) },
      onBegin,
      onEnd,
      onFail,
      { onResult() }
    )
  }

  fun setCompetition(
    competition: AdminAPI.RaceStatus,
    onBegin: () -> Unit,
    onEnd: () -> Unit,
    onFail: (message: String) -> Unit,
    onResult: () -> Unit
  ) {
    val areq = AdminAPI.AdminRequest(Credentials = getCredentials(), Competition = competition)

    callCompetitionSet = enqueue(
      callCompetitionSet,
      "/api/admin/competition/set/${competition.CompetitionId}",
      areq,
      { source -> adminResponseJsonAdapter.fromJson(source) },
      onBegin,
      onEnd,
      onFail,
      { onResult() }
    )
  }

  fun getCompetitionList(
    onBegin: () -> Unit,
    onEnd: () -> Unit,
    onFail: (message: String) -> Unit,
    onResult: (competitionList: AdminAPI.CompetitionList) -> Unit,
  ) {
    val areq = AdminAPI.AdminRequest(Credentials = getCredentials())

    callCompetitionListGet = enqueue(
      callCompetitionListGet,
      "/api/admin/competition/list",
      areq,
      { source -> competitionListJsonAdapter.fromJson(source) },
      onBegin,
      onEnd,
      onFail,
      onResult
    )
  }
}