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
class TestAdapter(private val context: Context) :RecyclerView.Adapter<TestAdapter.ViewHolder>()  {

  private  val explainData:MutableList<Explain> = mutableListOf()

    class ViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView){
        val explain :TextView = itemView.findViewById(R.id.item_test_explain)
        val count :TextView = itemView.findViewById(R.id.item_test_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_test,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val explain = explainData[position]
        holder.count.text = explain.count.toString()
        holder.explain.text = when(explain.code){
            1->{
                "没有找到设备:"
            }
            2->{
                "没有找到设备:"
            }
            3->{
                "连接成功未写入特征值："
            }
            4->{
                "没有找到蓝牙服务："
            }
            5->{
                "无法写入特征值"
            }
            11->{
                "蓝牙连接断开："
            }
            12->{
                "蓝牙服务为空："
            }
            13->{
                "蓝牙未开启："
            }
            else->{
                "未知错误${explain.code}:"
            }
        }
    }

    fun clean(){
        explainData.clear()
        notifyDataSetChanged()
    }

    fun addData(explain: Explain){

        explainData.forEach {
            if (it.code == explain.code){
                it.count+=1
                notifyDataSetChanged()
                return
            }
        }
        explainData.add(explain)
        notifyDataSetChanged()
    }

    override fun getItemCount() = explainData.size

}