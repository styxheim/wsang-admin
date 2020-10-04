package ru.styxheim.wsang_admin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.competition_discipline_item.view.*

class CompetitionDisciplineAdapter(
  private val disciplines: MutableList<AdminAPI.Discipline>,
  private val context: Context
) : RecyclerView.Adapter<CompetitionDisciplineItemHolder>() {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): CompetitionDisciplineItemHolder {
    val inflater = LayoutInflater.from(parent.context)

    return CompetitionDisciplineItemHolder(inflater, parent, context)
  }

  override fun getItemCount(): Int = disciplines.size

  override fun onBindViewHolder(holder: CompetitionDisciplineItemHolder, position: Int) {
    holder.bind(disciplines[position])
  }
}

class CompetitionDisciplineItemHolder(
  inflater: LayoutInflater, parent: ViewGroup,
  private val context: Context
) :
  RecyclerView.ViewHolder(inflater.inflate(R.layout.competition_discipline_item, parent, false)) {

  init {
  }

  fun bind(discipline: AdminAPI.Discipline) {
    itemView.disciplineName?.text = discipline.Name;
  }
}