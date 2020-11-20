package ru.styxheim.wsang_admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.competition_discipline_item.view.*

class CompetitionDisciplineAdapter(
  private val disciplines: MutableList<AdminAPI.Discipline>,
) : RecyclerView.Adapter<CompetitionDisciplineItemHolder>() {
  private var onClickNameListener: ((id: Int) -> Unit)? = null

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): CompetitionDisciplineItemHolder {
    val inflater = LayoutInflater.from(parent.context)

    return CompetitionDisciplineItemHolder(
      inflater,
      parent,
      onClickName = { id -> onClickNameListener?.invoke(id) })
  }

  override fun getItemCount(): Int = disciplines.size

  override fun onBindViewHolder(holder: CompetitionDisciplineItemHolder, position: Int) {
    holder.bind(disciplines[position])
  }

  fun setOnClickName(onClickName: (id: Int) -> Unit) {
    onClickNameListener = onClickName
  }
}

class CompetitionDisciplineItemHolder(
  inflater: LayoutInflater, parent: ViewGroup,
  private val onClickName: (id: Int) -> Unit
) :
  RecyclerView.ViewHolder(inflater.inflate(R.layout.competition_discipline_item, parent, false)) {

  init {
  }

  fun bind(discipline: AdminAPI.Discipline) {
    itemView.disciplineName?.let {
      it.text = discipline.Name
      it.setOnClickListener { onClickName(discipline.Id) }
    }
  }
}