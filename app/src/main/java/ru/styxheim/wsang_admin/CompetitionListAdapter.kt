package ru.styxheim.wsang_admin

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.styxheim.wsang_admin.databinding.CompetitionListItemBinding

class CompetitionListAdapter(
  private val competitions: List<AdminAPI.RaceStatus>,
  private val context: Context
) : RecyclerView.Adapter<CompetitionListItemHolder>(),
  CompetitionListItemHolder.OnClickListenerInterface {
  var onItemSelectListener: OnItemSelectListenerInterface? = null

  override fun onClick(competition: AdminAPI.RaceStatus) {
    onItemSelectListener?.onItemSelect(competition)
  }

  interface OnItemSelectListenerInterface {
    fun onItemSelect(competition: AdminAPI.RaceStatus)
  }

  override fun onBindViewHolder(holder: CompetitionListItemHolder, position: Int) {
    holder.bind(competitions[position])
  }

  override fun getItemCount(): Int = competitions.size

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompetitionListItemHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = CompetitionListItemBinding.inflate(inflater, parent, false)

    return CompetitionListItemHolder(binding, context, this)
  }
}

class CompetitionListItemHolder(
  private val binding: CompetitionListItemBinding,
  private val context: Context,
  private val onClick: OnClickListenerInterface
) : RecyclerView.ViewHolder(binding.root) {

  interface OnClickListenerInterface {
    fun onClick(competition: AdminAPI.RaceStatus)
  }

  fun bind(competition: AdminAPI.RaceStatus) {
    val crewsCountValue = competition.Crews?.size ?: 0

    if ((competition.CompetitionName?.compareTo("") ?: 0) != 0) {
      binding.competitionName.text = competition.CompetitionName
    } else {
      binding.competitionName.text = context.resources.getString(
        R.string.competition_name_null,
        competition.CompetitionId
      )
    }
    binding.competitionId.text = context.resources.getString(
      R.string.competition_id_title,
      competition.CompetitionId
    )
    binding.competitionCrewsCount.text = context.resources.getQuantityString(
      R.plurals.competition_crews,
      crewsCountValue, crewsCountValue
    )
    binding.competitionPlate.setOnClickListener {
      onClick.onClick(competition)
    }
  }
}