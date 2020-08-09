package net.testusuke.open.man10mail

import net.testusuke.open.man10mail.Main.Companion.plugin
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * Created by testusuke on 2020/07/11
 * @author testusuke
 */
object MailUtil {

    /**
     * DBへの保存用に変換
     * @param tag[String] tag
     *
     */
    fun convertTag(tag: String): String {
        return when (tag) {
            "0" -> {    //  normal = 0
                "normal"
            }
            "5" -> {    //  notice=5
                "#5"
            }
            "6" -> {    //  information=6
                "#6"
            }
            else -> {   //  normal=else
                tag
            }
        }
    }

    fun formatTag(tag: String): String {
        return when (tag) {
            "normal" -> {    //  normal = 0
                "§6§lNormal"
            }
            "#5" -> {    //  notice=5
                "§a§lNotice"
            }
            "#6" -> {    //  information=6
                "§d§lInformation"
            }
            else -> {   //  normal=else
                "§6§l${tag}"
            }
        }
    }

    /**
     * send message
     */
    fun sendMailMessage(player:Player,msg:String){
        val messages = msg.split(";")
        var i = 0
        for (m in messages) {
            if(i == 0){
                player.sendMessage(m.substring(1).replace("&","§"))
                i++
                continue
            }
            player.sendMessage(m.replace("&","§"))
            i++
        }
    }
    fun sendMailMessage(sender:CommandSender,msg:String){
        val messages = msg.split(";")
        var i = 0
        for (m in messages) {
            if(i == 0){
                sender.sendMessage(m.substring(1).replace("&","§"))
                i++
                continue
            }
            sender.sendMessage(m.replace("&","§"))
            i++
        }
    }

    /**
     * Set/Get NBT tag to/from Item
     * @param id[Int] Mail ID
     * @param item[ItemStack] Item
     * @return item[ItemStack] item
     */
    //  Setter
    fun setMailID(id:Int,item:ItemStack): ItemStack{
        val meta = item.itemMeta
        meta.persistentDataContainer.set(NamespacedKey(plugin,"id"), PersistentDataType.INTEGER, id)
        item.itemMeta = meta
        return item
    }
    //  Getter
    fun getMailID(item:ItemStack):Int{
        val meta = item.itemMeta
        return meta.persistentDataContainer[NamespacedKey(plugin,"id"), PersistentDataType.INTEGER] ?: return -0
    }

}