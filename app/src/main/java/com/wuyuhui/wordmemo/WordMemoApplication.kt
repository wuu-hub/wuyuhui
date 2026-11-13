package com.wuyuhui.wordmemo

import android.app.Application
import com.wuyuhui.wordmemo.data.local.WordMemoDatabase
import com.wuyuhui.wordmemo.data.repository.WordRepository

/**
 * 应用级容器：数据库与仓库在这里只创建一次，供各界面共享。
 */
class WordMemoApplication : Application() {

    val database: WordMemoDatabase by lazy { WordMemoDatabase.get(this) }

    val repository: WordRepository by lazy {
        WordRepository(
            libraryDao = database.libraryDao(),
            wordDao = database.wordDao(),
            reviewLogDao = database.reviewLogDao()
        )
    }
}
