package ru.styxheim.wsang_admin

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class Transport(private val sharedPreferences: SharedPreferences) {
  private val serverScheme: String = "http://"
  private var callCompetitionListGet: Call? = null

  private val httpClient = OkHttpClient()
  private val moshi: Moshi = Moshi.Builder().build()
  private val adminRequestJsonAdapter:
      JsonAdapter<AdminAPI.AdminRequest> = moshi.adapter(AdminAPI.AdminRequest::class.java)
  private val competitionListJsonAdapter:
      JsonAdapter<AdminAPI.CompetitionList> = moshi.adapter(AdminAPI.CompetitionList::class.java)

  fun getCompetitionList(
    onBegin: () -> Unit,
    onEnd: () -> Unit,
    onFail: (message: String) -> Unit,
    onResult: (competitionList: AdminAPI.CompetitionList) -> Unit,
  ) {
    val serverAddress: String = sharedPreferences.getString("server_address", null) ?: return
    val terminalString: String = sharedPreferences.getString("terminal_string", null) ?: return
    val secureKey: String = sharedPreferences.getString("secure_key", null) ?: return
    val credentials = AdminAPI.Credentials(TerminalString = terminalString, SecureKey = secureKey)
    val areq = AdminAPI.AdminRequest(Credentials = credentials)
    val mediaType = "application/json; charset=utf8".toMediaType()
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
    onBegin()
    callCompetitionListGet?.enqueue(object : Callback {
      override fun onResponse(call: Call, response: Response) {
        onEnd()
        when (response.code) {
          200 -> {
            competitionListJsonAdapter.fromJson(response.body!!.source())?.let {
              onResult(it)
              return;
            }
            onFail("Unknown error: json not parsed")
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
}