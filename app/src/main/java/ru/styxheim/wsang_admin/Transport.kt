package ru.styxheim.wsang_admin

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
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

  private val httpClient = OkHttpClient()
  private val moshi: Moshi = Moshi.Builder().build()
  private val adminRequestJsonAdapter:
      JsonAdapter<AdminAPI.AdminRequest> = moshi.adapter(AdminAPI.AdminRequest::class.java)
  private val competitionListJsonAdapter:
      JsonAdapter<AdminAPI.CompetitionList> = moshi.adapter(AdminAPI.CompetitionList::class.java)

  private fun getCredentials(): AdminAPI.Credentials {
    val terminalString: String = sharedPreferences.getString("terminal_string", "")!!
    val secureKey: String = sharedPreferences.getString("secure_key", "")!!

    return AdminAPI.Credentials(TerminalString = terminalString, SecureKey = secureKey)
  }

  private fun enqueue(
    call: Call,
    onBegin: () -> Unit,
    onEnd: () -> Unit,
    onFail: (message: String) -> Unit,
    onResult: (source: BufferedSource) -> Unit
  ) {
    onBegin()
    call.enqueue(object : Callback {
      override fun onResponse(call: Call, response: Response) {
        onEnd()
        when (response.code) {
          200 -> {
            onResult(response.body!!.source())
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
  }

  fun setCompetition(
    competition: AdminAPI.RaceStatus,
    onBegin: () -> Unit,
    onEnd: () -> Unit,
    onFail: (message: String) -> Unit,
    onResult: () -> Unit
  ) {
    val serverAddress: String = sharedPreferences.getString("server_address", null) ?: return
    val areq = AdminAPI.AdminRequest(Credentials = getCredentials(), Competition = competition)
    val body = adminRequestJsonAdapter.toJson(areq).toString().toRequestBody(mediaType)
    val request = Request.Builder()
      .url("$serverScheme$serverAddress/api/admin/competition/set/${competition.CompetitionId}")
      .post(body)
      .build()

    callCompetitionSet?.let {
      onEnd()
      it.cancel()
    }

    callCompetitionListGet = httpClient.newCall(request)
    enqueue(callCompetitionListGet!!, onBegin, onEnd, onFail, { onResult() })
  }

  fun getCompetitionList(
    onBegin: () -> Unit,
    onEnd: () -> Unit,
    onFail: (message: String) -> Unit,
    onResult: (competitionList: AdminAPI.CompetitionList) -> Unit,
  ) {
    val serverAddress: String = sharedPreferences.getString("server_address", null) ?: return
    val areq = AdminAPI.AdminRequest(Credentials = getCredentials())
    val body = adminRequestJsonAdapter.toJson(areq).toString().toRequestBody(mediaType)
    val request = Request.Builder()
      .url("$serverScheme$serverAddress/api/admin/competition/list")
      .post(body)
      .build()

    callCompetitionListGet?.let {
      onEnd()
      it.cancel()
    }

    callCompetitionListGet = httpClient.newCall(request)
    enqueue(callCompetitionListGet!!, onBegin, onEnd, onFail, { source ->
      competitionListJsonAdapter.fromJson(source)?.let {
        onResult(it)
      } ?: run {
        onFail("Unknown error: json not parsed")
      }
    })
  }
}