package net.testusuke.open.man10mail

import net.testusuke.open.man10mail.DataBase.MailBox
import net.testusuke.open.man10mail.DataBase.MailConsole
import net.testusuke.open.man10mail.Main.Companion.plugin
import net.testusuke.open.man10mail.Main.Companion.prefix
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

/**
 * Created by testusuke on 2020/07/04
 * @author testusuke
 */
object EventListener : Listener {

    //  InventoryClick
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked
        if (player !is Player) return
        val title = event.view.title
        if (title == MailBox.INVENTORY_TITLE) {
            //  cancel
            event.isCancelled = true
            //  ItemStack if null -> return
            val item = event.currentItem ?: return
            if (item.type != Material.PAPER) return
            val id = checkMailID(item)
            if (id == -1) return

            //  LeftClick
            if(event.isLeftClick) {
                //  show
                Thread(Runnable {
                    MailBox.showMail(player, id)
                }).start()

            }else if(event.isRightClick){
                Thread(Runnable {
                    val result = MailConsole.getInformation(id) ?: return@Runnable
                    //  Vault
                    val money = VaultManager.economy?.getBalance(player)
                    if (money != null) {
                        if(!player.hasPermission(Permission.ADMIN) && money < plugin.MONEY_EXPORT_MAIL.toDouble()){
                            player.sendMessage("${Main.prefix}§c必要料金を持っていません。料金: ${plugin.MONEY_EXPORT_MAIL}")
                            return@Runnable
                        }
                    }
                    //  引き出し
                    if(!player.hasPermission(Permission.ADMIN) && plugin.MONEY_EXPORT_MAIL != 0) {
                        VaultManager.economy?.withdrawPlayer(player, plugin.MONEY_SEND_MAIL.toDouble())
                    }
                    //  Create
                    val mailItem = MailUtil.createExportedMail(result)
                    //  Add
                    player.inventory.addItem(mailItem)
                    //  Message
                    player.sendMessage("${prefix}§aメールを紙に出力しました。")
                }).start()
            }
            //  Close GUI
            player.closeInventory()
        }
    }

    //  PlayerJoinEvent
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        Thread(Runnable {
            MailConsole.sendEveryoneMail(player.uniqueId.toString())
            MailConsole.removeOldMail(player.uniqueId.toString())
            MailConsole.sendNotReadMail(player)
            MailNoticeSetting.enableNotice(player)
        }).start()
    }

    /**
     * function of check mail id
     * @param item[ItemStack]
     * @return id[Int]
     */
    private fun checkMailID(item: ItemStack): Int {
        return MailUtil.getMailID(item)
    }
}