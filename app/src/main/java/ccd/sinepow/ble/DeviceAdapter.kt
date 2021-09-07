package ccd.sinepow.ble

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * 作者 ： yp
 * 时间 ： 2021/8/4
 * 邮箱 ： 1632502697@qq.com
 * 版本 ： V 1.0
 * 简述 ：
 * 更新 ：
 * 注意 ：
 */
class DeviceAdapter(private val context: Context) :
    RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

   private  val deviceData: MutableList<Device> = mutableListOf()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.item_device_name)
        val addressText: TextView = itemView.findViewById(R.id.item_device_address)
        val cancelText: TextView = itemView.findViewById(R.id.item_device_cancel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = deviceData[position]
        holder.nameText.text = device.name
        holder.addressText.text = device.address
        holder.cancelText.visibility = if (device.enable) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            device.enable = true
            notifyItemChanged(position)
        }

        holder.cancelText.setOnClickListener {
            device.enable = false
            notifyItemChanged(position)
        }

    }

    fun addDevice(device: Device) {
        deviceData.forEach {
            if (it.address == device.address) {
                return
            }
        }
        deviceData.add(device)
        notifyDataSetChanged()
    }

    fun getEnableDevice():MutableList<Device>{

        val data:MutableList<Device> = mutableListOf()

        deviceData.forEach {
            if (it.enable)
                data.add(it)
        }
        return data
    }

    override fun getItemCount() = deviceData.size

}