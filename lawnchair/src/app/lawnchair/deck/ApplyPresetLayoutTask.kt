package app.lawnchair.deck

import com.android.launcher3.LauncherModel
import com.android.launcher3.LauncherSettings.Favorites
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_DESKTOP
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT
import com.android.launcher3.model.AllAppsList
import com.android.launcher3.model.BgDataModel
import com.android.launcher3.model.ModelTaskController
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.util.ContentWriter
import com.android.launcher3.util.Executors.MAIN_EXECUTOR
import java.util.Locale

/**
 * Replaces the home screen with [PresetLayout]: the dock, pages and folders are
 * rebuilt at the listed positions, and every installed app that the preset does
 * not mention is placed on the pages after it. Widgets are left untouched.
 */
class ApplyPresetLayoutTask(
    private val onComplete: (() -> Unit)? = null,
) : LauncherModel.ModelUpdateTask {

    override fun execute(
        taskController: ModelTaskController,
        dataModel: BgDataModel,
        apps: AllAppsList,
    ) {
        val app = taskController.app
        val context = app.context
        val idp = app.invariantDeviceProfile
        val columns = idp.numColumns
        val rows = idp.numRows
        val db = app.model.modelDbController

        // Rows are written straight to the database (no per-item UI binding);
        // the model is reloaded once at the end.
        fun insert(item: ItemInfo, container: Int, screenId: Int, x: Int, y: Int) {
            item.container = container
            item.screenId = screenId
            item.cellX = x
            item.cellY = y
            item.id = db.generateNewItemId()
            val writer = ContentWriter(context)
            item.onAddToDatabase(writer)
            writer.put(Favorites._ID, item.id)
            db.insert(Favorites.TABLE_NAME, writer.getValues(context))
        }

        val allApps = apps.data.toList()
        val pool = allApps.toMutableList()

        // Exact label first, then case-insensitive, then prefix for "Name*".
        // Apps still in the pool are preferred, so two entries with the same
        // label (for example a dual app) take different apps; when the pool
        // has none left, the app is reused (an app in both the dock and a page).
        fun find(label: String): AppInfo? {
            val prefix = label.endsWith("*")
            val text = label.removeSuffix("*").trim()
            val lower = text.lowercase(Locale.ROOT)
            fun title(info: AppInfo) = info.title?.toString()?.trim().orEmpty()
            val matchers: List<(AppInfo) -> Boolean> = if (prefix) {
                listOf { info -> title(info).lowercase(Locale.ROOT).startsWith(lower) }
            } else {
                listOf(
                    { info -> title(info) == text },
                    { info -> title(info).lowercase(Locale.ROOT) == lower },
                )
            }
            for (matcher in matchers) {
                pool.firstOrNull(matcher)?.let {
                    pool.remove(it)
                    return it
                }
            }
            for (matcher in matchers) {
                allApps.firstOrNull(matcher)?.let { return it }
            }
            return null
        }

        // Clear the current icons and folders; keep widgets.
        db.delete(
            Favorites.TABLE_NAME,
            "${Favorites.ITEM_TYPE} NOT IN (${Favorites.ITEM_TYPE_APPWIDGET}, ${Favorites.ITEM_TYPE_CUSTOM_APPWIDGET})",
            null,
        )

        fun place(slot: PresetLayout.Slot, container: Int, screenId: Int, x: Int, y: Int) {
            when (slot) {
                is PresetLayout.App -> {
                    val info = find(slot.label) ?: return
                    val item = info.makeWorkspaceItem(context) ?: return
                    insert(item, container, screenId, x, y)
                }

                is PresetLayout.Folder -> {
                    val items = slot.apps.mapNotNull { find(it)?.makeWorkspaceItem(context) }
                    when {
                        items.isEmpty() -> return

                        items.size == 1 ->
                            insert(items[0], container, screenId, x, y)

                        else -> {
                            val folder = FolderInfo().apply { title = slot.title }
                            insert(folder, container, screenId, x, y)
                            items.forEachIndexed { rank, item ->
                                item.rank = rank
                                insert(item, folder.id, 0, 0, 0)
                            }
                        }
                    }
                }
            }
        }

        // Dock
        PresetLayout.hotseat.take(idp.numDatabaseHotseatIcons).forEachIndexed { i, slot ->
            if (slot != null) place(slot, CONTAINER_HOTSEAT, i, i, 0)
        }

        // Pages; cells outside the current grid are skipped.
        PresetLayout.pages.forEachIndexed { screenId, page ->
            page.take(rows).forEachIndexed { y, row ->
                row.take(columns).forEachIndexed { x, slot ->
                    if (slot != null) place(slot, CONTAINER_DESKTOP, screenId, x, y)
                }
            }
        }

        // Everything else goes on the following pages, sorted by name.
        val remaining = pool.sortedBy { it.title?.toString()?.lowercase(Locale.getDefault()).orEmpty() }
        var screenId = PresetLayout.pages.size
        var cell = 0
        for (info in remaining) {
            val item = info.makeWorkspaceItem(context) ?: continue
            if (cell == columns * rows) {
                screenId++
                cell = 0
            }
            insert(item, CONTAINER_DESKTOP, screenId, cell % columns, cell / columns)
            cell++
        }

        // Rebuild every page and folder from the database.
        MAIN_EXECUTOR.execute {
            app.model.forceReload()
            onComplete?.invoke()
        }
    }
}
