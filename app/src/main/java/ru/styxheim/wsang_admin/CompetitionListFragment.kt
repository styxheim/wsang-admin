package ru.styxheim.wsang_admin

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.fragment_competition_list.*

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class CompetitionListFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {
  private var competitionLoaded: Boolean = false
  private var competitionList: MutableList<AdminAPI.RaceStatus> = mutableListOf()
  private var transport: Transport? = null
  private var sharedPreferences: SharedPreferences? = null
  private val moshi: Moshi = Moshi.Builder().build()
  private val competitionJsonAdapter:
      JsonAdapter<AdminAPI.RaceStatus> = moshi.adapter(AdminAPI.RaceStatus::class.java)
  private var competitionListAdapter: CompetitionListAdapter? = null

  override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
    when (key) {
      "server_address" -> loadCompetitionList()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    sharedPreferences?.registerOnSharedPreferenceChangeListener(this)

    transport = Transport(sharedPreferences!!)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_competition_list, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    competitionAddView.visibility = if (competitionLoaded) View.VISIBLE else View.INVISIBLE
    competitionAddView.setOnClickListener {
      val competitionId =
        ((competitionList.maxByOrNull { it.CompetitionId })?.CompetitionId ?: 0L) + 1
      val competition =
        AdminAPI.RaceStatus(
          CompetitionId = competitionId,
          TimeStamp = System.currentTimeMillis() / 1000,
          SyncPoint = 0
        )
      navToCompetition(competition)
    }
    refreshList.setOnClickListener { this.loadCompetitionList() }


    class OnClick : CompetitionListAdapter.OnItemSelectListenerInterface {
      override fun onItemSelect(competition: AdminAPI.RaceStatus) {
        navToCompetition(competition)
      }
    }

    competitionListAdapter = CompetitionListAdapter(competitionList, requireContext())

    competitionListAdapter?.onItemSelectListener = OnClick()
    competitionListView.apply {
      layoutManager = LinearLayoutManager(activity)
      adapter = competitionListAdapter
    }


    loadCompetitionList()
  }

  override fun onDestroyView() {
    sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    super.onDestroyView()
  }

  private fun navigateToFail(message: String) {
    val args = Bundle()

    args.putString("Message", message)
    args.putInt("From", R.id.CompetitionListFragment)
    findNavController().navigate(R.id.action_CompetitionListFragment_to_FailFragment, args)
  }

  private fun responseGenericError(message: String?) {
    navigateToFail("Connection error: $message")
  }

  private fun navToCompetition(competition: AdminAPI.RaceStatus) {
    val args = Bundle()

    args.putString("competition_json", competitionJsonAdapter.toJson(competition))
    findNavController().navigate(
      R.id.action_ComptitionListFragment_to_CompetitionFragment,
      args
    )
  }

  private fun responseAdminList(response: AdminAPI.CompetitionList) {
    competitionAddView.visibility = View.VISIBLE
    competitionLoaded = true
    competitionList.clear()
    competitionList.addAll(response.Competitions)
    competitionListAdapter?.notifyDataSetChanged()
  }

  private fun loadCompetitionList() {
    transport?.getCompetitionList(
      { -> activity?.runOnUiThread { activity?.title = getString(R.string.updating) } },
      { ->
        activity?.runOnUiThread {
          activity?.title = getString(R.string.competition_list_fragment_label)
        }
      },
      { message -> activity?.runOnUiThread { responseGenericError(message) } },
      { competitionList -> activity?.runOnUiThread { responseAdminList(competitionList) } })
  }
}