package com.crm.connectlabpodcast.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.crm.connectlabpodcast.MainActivity
import com.crm.connectlabpodcast.data.Record
import com.crm.connectlabpodcast.databinding.ItemRecordBinding
import com.crm.connectlabpodcast.record.RecyclerViewOnClickListener

class RecordListAdapter (
    private val listener: RecyclerViewOnClickListener
) : ListAdapter<Record, RecordItemHolder>(DiffCallBack()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordItemHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordItemHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordItemHolder, position: Int) {
        val item = getItem(position)
        holder.itemRecordBinding.parentCard.setOnClickListener {
            listener.onItemClick(position)
        }
        holder.bind(item)
    }
}

class DiffCallBack : DiffUtil.ItemCallback<Record>() {
    override fun areItemsTheSame(oldItem: Record, newItem: Record) = oldItem.title == newItem.title

    override fun areContentsTheSame(oldItem: Record, newItem: Record) =
        oldItem == newItem

}

class RecordItemHolder constructor(
    val itemRecordBinding: ItemRecordBinding,
) : RecyclerView.ViewHolder(itemRecordBinding.root) {

    fun bind(record: Record) {
        itemRecordBinding.recordTitle.text = record.title
    }
}
