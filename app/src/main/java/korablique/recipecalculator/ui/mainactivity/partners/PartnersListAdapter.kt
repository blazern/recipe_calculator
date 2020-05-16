package korablique.recipecalculator.ui.mainactivity.partners

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import korablique.recipecalculator.R
import korablique.recipecalculator.outside.partners.Partner
import korablique.recipecalculator.ui.MyViewHolder

class PartnersListAdapter(
        private val onClickFn: (Partner)->Unit,
        private val onLongClickFn: (Partner,View)->Boolean)
    : RecyclerView.Adapter<MyViewHolder>() {
    private val partners = mutableListOf<Partner>()

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.item.findViewById<TextView>(R.id.partner_name).text = partners[position].name
        holder.item.setOnClickListener {
            onClickFn.invoke(partners[position])
        }
        holder.item.setOnLongClickListener {
            onLongClickFn.invoke(partners[position], it)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val item = LayoutInflater.from(parent.context)
                .inflate(R.layout.partner_list_item_layout, parent, false)
        return MyViewHolder(item as ViewGroup)
    }

    override fun getItemCount(): Int {
        return partners.size
    }

    fun setPartners(partners: List<Partner>) {
        this.partners.clear()
        this.partners.addAll(partners)
        notifyDataSetChanged()
    }
}