package x.x.memlists

import android.app.Application
import x.x.memlists.core.data.MemListsDatabaseHelper
import x.x.memlists.core.data.MemListsRepository
import x.x.memlists.core.i18n.AppLocalizer
import x.x.memlists.core.theme.ThemeRepository

class MemListsApplication : Application() {
    val databaseHelper: MemListsDatabaseHelper by lazy { MemListsDatabaseHelper(this) }
    val repository: MemListsRepository by lazy { MemListsRepository(databaseHelper) }
    val localizer: AppLocalizer by lazy { AppLocalizer(this) }
    val themeRepository: ThemeRepository by lazy { ThemeRepository(this) }
}

