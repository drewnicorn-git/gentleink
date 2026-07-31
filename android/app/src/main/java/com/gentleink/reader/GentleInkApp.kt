package com.gentleink.reader

import android.app.Application
import com.gentleink.reader.data.BookRepository
import com.gentleink.reader.data.SettingsRepository
import com.gentleink.reader.filter.GentleInkFilter

class GentleInkApp : Application() {
    lateinit var filter: GentleInkFilter
        private set
    lateinit var books: BookRepository
        private set
    lateinit var settings: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        filter = GentleInkFilter.fromContext(this)
        books = BookRepository(this)
        settings = SettingsRepository(this)
    }
}
