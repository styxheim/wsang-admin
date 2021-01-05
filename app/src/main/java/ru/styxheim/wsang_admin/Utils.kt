package ru.styxheim.wsang_admin

import android.app.AlertDialog
import android.content.Context
import androidx.annotation.StringRes

object Utils {
  fun showInfoDialog(context: Context, title: String, message: String) {
    val infoDialog = AlertDialog.Builder(context)

    infoDialog.setTitle(title)
    infoDialog.show()
    infoDialog.setMessage(message)
    infoDialog.setNeutralButton(R.string.accept) { _, _ -> }
  }

  fun showInfoDialog(context: Context, @StringRes title: Int, message: String) {
    showInfoDialog(context, context.resources.getString(title), message)
  }

  fun showInfoDialog(context: Context, @StringRes title: Int, @StringRes message: Int) {
    showInfoDialog(
      context,
      context.resources.getString(title),
      context.resources.getString(message)
    )
  }
}