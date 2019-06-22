package ink.ptms.cronus.builder.element;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import ink.ptms.cronus.Cronus;
import ink.ptms.cronus.builder.Builders;
import ink.ptms.cronus.builder.editor.EditorAPI;
import ink.ptms.cronus.builder.editor.data.PlayerData;
import ink.ptms.cronus.builder.editor.data.PlayerDataHandler;
import ink.ptms.cronus.command.CronusCommand;
import ink.ptms.cronus.internal.version.MaterialControl;
import ink.ptms.cronus.util.Utils;
import me.skymc.taboolib.common.util.SimpleIterator;
import me.skymc.taboolib.inventory.InventoryUtil;
import me.skymc.taboolib.inventory.builder.ItemBuilder;
import me.skymc.taboolib.inventory.builder.v2.ClickType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author 坏黑
 * @Since 2019-06-18 23:09
 */
public class BuilderStageContent extends CronusCommand {

    private Player player;
    private String display;
    private BuilderStage builderStage;
    private List<List<String>> content;
    private Map<Integer, List<String>> map = Maps.newHashMap();
    private Map<Integer, Integer> mapIndex = Maps.newHashMap();
    private boolean toggle;
    private boolean append;
    private int page;

    public BuilderStageContent(Player player, String display, List<List<String>> content, BuilderStage builderStage) {
        this.player = player;
        this.display = display;
        this.content = content;
        this.builderStage = builderStage;
    }

    public void open() {
        open(page);
    }

    public void open(int page) {
        this.map.clear();
        this.page = page;
        this.toggle = true;
        this.append = hasAppend() || content.add(Lists.newArrayList("$append"));
        Inventory inventory = Builders.normal(display,
                e -> {
                    if (e.getClickType() == ClickType.CLICK) {
                        e.castClick().setCancelled(true);
                        // 关闭界面
                        if (e.castClick().getRawSlot() == 49) {
                            toggle = true;
                            content.stream().filter(this::isAppend).forEach(array -> content.remove(array));
                            builderStage.open(player);
                        }
                        // 下一页
                        else if (e.castClick().getRawSlot() == 52 && MaterialControl.GREEN_STAINED_GLASS_PANE.isSameMaterial(e.castClick().getCurrentItem())) {
                            open(page + 1);
                        }
                        // 上一页
                        else if (e.castClick().getRawSlot() == 46 && MaterialControl.GREEN_STAINED_GLASS_PANE.isSameMaterial(e.castClick().getCurrentItem())) {
                            open(page - 1);
                        }
                        List<String> content = map.get(e.castClick().getRawSlot());
                        if (content == null) {
                            return;
                        }
                        // 添加
                        if (isAppend(content)) {
                            content.clear();
                            open(page);
                        }
                        // 修改
                        else {
                            int index = mapIndex.get(e.castClick().getRawSlot());
                            // 左键
                            if (e.castClick().isLeftClick()) {
                                if (e.castClick().getClick().isLeftClick()) {
                                    // 左移
                                    if (e.castClick().isShiftClick()) {
                                        // 有效
                                        if (index > 0 && !Utils.isActionCooldown(player)) {
                                            List<String> element = this.content.remove(index);
                                            this.content.add(index - 1, element);
                                        }
                                    }
                                } else {
                                    toggle = true;
                                    player.closeInventory();
                                    // 编辑模式
                                    EditorAPI.openEdit(player, content);
                                    EditorAPI.eval(player, ":edit");
                                    // 执行动作
                                    PlayerData playerData = PlayerDataHandler.getPlayerData(player);
                                    playerData.setSaveTask(() -> {
                                        content.clear();
                                        content.addAll(EditorAPI.getContent(player));
                                    });
                                    playerData.setCloseTask(() -> {
                                        open(page);
                                    });
                                }
                            }
                            // 右键
                            else if (e.castClick().isRightClick()) {
                                // 右移
                                if (e.castClick().isShiftClick()) {
                                    // 有效
                                    if (index < this.content.size() - 2 && !Utils.isActionCooldown(player)) {
                                        List<String> element = this.content.remove(index);
                                        this.content.add(index + 1, element);
                                    }
                                } else {
                                    content.add("$delete");
                                    content.stream().filter(c -> c.contains("$delete")).forEach(content::remove);
                                    open(page);
                                }
                            }
                        }
                    }
                }, e -> {
                    if (!toggle) {
                        Bukkit.getScheduler().runTaskLater(Cronus.getInst(), () -> {
                            content.stream().filter(this::isAppend).forEach(array -> content.remove(array));
                            builderStage.open(player);
                        }, 1);
                    }
                });
        List<List<String>> iterator = new SimpleIterator(content).listIterator(page * 28, (page + 1) * 28);
        for (int i = 0; i < iterator.size(); i++) {
            List<String> content = iterator.get(i);
            // 添加笔记
            if (isAppend(content)) {
                inventory.setItem(InventoryUtil.SLOT_OF_CENTENTS.get(i), new ItemBuilder(Material.MAP).name("§f增加新的" + display).lore("", "§7点击").build());
            }
            // 修改笔记
            else {
                List<String> list = Lists.newArrayList("");
                if (content.isEmpty()) {
                    list.add("§f无内容");
                } else {
                    list.addAll(content.stream().map(s -> "§f" + s).collect(Collectors.toList()));
                }
                list.addAll(Lists.newArrayList("§8§m                  ", "§7修改: §8左键", "§7删除: §8右键", "§7左移: §8SHIFT+左键", "§7右移: §8SHIFT+右键"));
                inventory.setItem(InventoryUtil.SLOT_OF_CENTENTS.get(i), new ItemBuilder(Material.PAPER).name("§f第 " + (((page + 1) * i) + 1) + " 页").lore(list).build());
            }
            map.put(InventoryUtil.SLOT_OF_CENTENTS.get(i), content);
            mapIndex.put(InventoryUtil.SLOT_OF_CENTENTS.get(i), page * 28 + i);
        }
        if (page > 0) {
            inventory.setItem(46, new ItemBuilder(MaterialControl.GREEN_STAINED_GLASS_PANE.parseItem()).name("§a上一页").lore("", "§7点击").build());
        }
        if (Utils.next(page, content.size(), 28)) {
            inventory.setItem(52, new ItemBuilder(MaterialControl.GREEN_STAINED_GLASS_PANE.parseItem()).name("§a下一页").lore("", "§7点击").build());
        }
        inventory.setItem(49, new ItemBuilder(MaterialControl.RED_STAINED_GLASS_PANE.parseItem()).name("§c上级目录").lore("", "§7点击").build());
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        this.toggle = false;
    }

    public boolean hasAppend() {
        return content.stream().anyMatch(this::isAppend);
    }

    public boolean isAppend(List<String> content) {
        return content.size() == 1 && content.get(0).equals("$append");
    }
}