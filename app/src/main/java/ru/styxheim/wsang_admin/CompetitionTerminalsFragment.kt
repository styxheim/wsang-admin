package ru.styxheim.wsang_admin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import com.squareup.moshi.Moshi
import ru.styxheim.wsang_admin.databinding.FragmentCompetitionTerminalsBinding

private const val COMPTITION_PARAM = "competition_json"

/**
 * A simple [Fragment] subclass.
 * Use the [CompetitionTerminalsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CompetitionTerminalsFragment : Fragment() {
  private var binding: FragmentCompetitionTerminalsBinding? = null
  private var competition: AdminAPI.RaceStatus = AdminAPI.RaceStatus(SyncPoint = 0)
  private var transport: Transport? = null
  private val moshi: Moshi = Moshi.Builder().build()
  private val competitionJsonAdapter = moshi.adapter(AdminAPI.RaceStatus::class.java)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.getString(COMPTITION_PARAM, null)?.let {
      competition = competitionJsonAdapter.fromJson(it)!!
    }

    transport = Transport(PreferenceManager.getDefaultSharedPreferences(context)!!)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    binding = FragmentCompetitionTerminalsBinding.inflate(inflater, container, false)
    return binding?.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding = null
  }
}