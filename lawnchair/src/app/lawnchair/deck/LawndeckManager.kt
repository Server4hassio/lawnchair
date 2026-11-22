package app.lawnchair.deck

import android.content.Context
import android.util.Log
import app.lawnchair.LawnchairLauncher
import app.lawnchair.launcher
import app.lawnchair.launcherNullable
import app.lawnchair.util.restartLauncher
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherSettings
import com.android.launcher3.model.ItemInstallQueue
import com.android.launcher3.model.ModelDbController
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.provider.RestoreDbTask
import com.android.launcher3.util.ComponentKey
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LawndeckManager(private val context: Context) {

    // TODO

    private val launcher = context.launcherNullable ?: LawnchairLauncher.instance?.launcher

    suspend fun enableLawndeck(
        onProgress: ((String) -> Unit)? = null,
    ) = withContext(Dispatchers.IO) {
        val completionDeferred = CompletableDeferred<Unit>()

        if (!backupExists("bk")) createBackup("bk")
        if (backupExists("lawndeck")) {
            onProgress?.invoke("Restoring previous layout...")
            restoreBackup("lawndeck")
            completionDeferred.complete(Unit)
        } else {
            onProgress?.invoke("Categorizing apps...")
            addAllAppsToWorkspace(onProgress) {
                completionDeferred.complete(Unit)
            }
        }

        completionDeferred.await()
    }

    suspend fun disableLawndeck() = withContext(Dispatchers.IO) {
        if (backupExists("bk")) {
            createBackup("lawndeck")
            restoreBackup("bk")
        }
    }

    private fun createBackup(suffix: String) = runCatching {
        getDatabaseFiles(suffix).apply {
            db.copyTo(backupDb, overwrite = true)
            if (journal.exists()) journal.copyTo(backupJournal, overwrite = true)
        }
    }.onFailure { Log.e("LawndeckManager", "Failed to create backup: $suffix", it) }

    private fun restoreBackup(suffix: String) = runCatching {
        getDatabaseFiles(suffix).apply {
            backupDb.copyTo(db, overwrite = true)
            if (backupJournal.exists()) backupJournal.copyTo(journal, overwrite = true)
        }
        postRestoreActions()
    }.onFailure { Log.e("LawndeckManager", "Failed to restore backup: $suffix", it) }

    private fun getDatabaseFiles(suffix: String): DatabaseFiles {
        val idp = InvariantDeviceProfile.INSTANCE.get(context)
        val dbFile = context.getDatabasePath(idp.dbFile)
        return DatabaseFiles(
            db = dbFile,
            backupDb = File(dbFile.parent, "${suffix}_${idp.dbFile}"),
            journal = File(dbFile.parent, "${idp.dbFile}-journal"),
            backupJournal = File(dbFile.parent, "${suffix}_${idp.dbFile}-journal"),
        )
    }

    private fun backupExists(suffix: String): Boolean = getDatabaseFiles(suffix).backupDb.exists()

    private fun postRestoreActions() {
        ModelDbController(context).let { RestoreDbTask.performRestore(context, it) }
        restartLauncher(context)
    }

    private fun addAllAppsToWorkspace(
        onProgress: ((String) -> Unit)?,
        onComplete: (() -> Unit)?,
    ) {
        onProgress?.invoke("Adding apps to workspace...")
        // Builds the dock, pages and folders from PresetLayout; any other
        // installed app is placed on the pages after it.
        LauncherAppState.getInstance(context).model.enqueueModelUpdateTask(
            ApplyPresetLayoutTask { onComplete?.invoke() },
        )
    }

    /**
     * Adds a newly installed app directly to the workspace (no category folders).
     * This is called when a new app is installed and deck layout is enabled.
     *
     * @param packageName The package name of the newly installed app
     * @param user The user handle for the app
     * @param modelWriter Unused; kept for the existing caller's signature
     * @param dataModel Unused; kept for the existing caller's signature
     */
    fun addNewlyInstalledApp(
        packageName: String,
        user: android.os.UserHandle,
        modelWriter: com.android.launcher3.model.ModelWriter,
        dataModel: com.android.launcher3.model.BgDataModel,
    ) {
        // Flat layout: newly installed apps go straight onto the workspace,
        // never into a category folder.
        ItemInstallQueue.INSTANCE.get(context).queueItem(packageName, user)
    }

    private data class DatabaseFiles(
        val db: File,
        val backupDb: File,
        val journal: File,
        val backupJournal: File,
    )
}
