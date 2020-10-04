package ru.styxheim.wsang_admin

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.competition_list_item.view.*

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

    return CompetitionListItemHolder(inflater, parent, context, this)
  }
}

class CompetitionListItemHolder(
  inflater: LayoutInflater, parent: ViewGroup,
  private val context: Context,
  private val onClick: OnClickListenerInterface
) :
  RecyclerView.ViewHolder(inflater.inflate(R.layout.competition_list_item, parent, false)) {
  private var crewsCount: TextView? = null
  private var name: TextView? = null
  private var plate: LinearLayout? = null
  private var id: TextView? = null

  interface OnClickListenerInterface {
    fun onClick(competition: AdminAPI.RaceStatus)
  }

  init {
    name = itemView.competitionName
    crewsCount = itemView.competitionCrewsCount
    id = itemView.competitionId
    plate = itemView.competitionPlate
  }

  fun bind(competition: AdminAPI.RaceStatus) {
    val crewsCountValue = competition.Crews?.size ?: 0

    if ((competition.CompetitionName?.compareTo("") ?: 0) != 0) {
      name?.text = competition.CompetitionName
    } else {
      name?.text = context.resources.getString(
        R.string.competition_name_null,
        competition.CompetitionId
      )
    }
    id?.text = context.resources.getString(
      R.string.competition_id_title,
      competition.CompetitionId
    )
    crewsCount?.text = context.resources.getQuantityString(
      R.plurals.competition_crews,
      crewsCountValue, crewsCountValue
    )
    plate?.setOnClickListener {
      onClick.onClick(competition)
    }
  }
}