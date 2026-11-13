package com.wuyuhui.wordmemo.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordLibraryDao {

    @Query("SELECT * FROM word_libraries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WordLibraryEntity>>

    @Query("SELECT * FROM word_libraries WHERE id = :id")
    suspend fun findById(id: Long): WordLibraryEntity?

    @Query("SELECT COUNT(*) FROM word_libraries")
    suspend fun count(): Int

    @Insert
    suspend fun insert(library: WordLibraryEntity): Long

    @Update
    suspend fun update(library: WordLibraryEntity)

    @Delete
    suspend fun delete(library: WordLibraryEntity)
}

@Dao
interface WordDao {

    @Query("SELECT * FROM words WHERE libraryId = :libraryId ORDER BY spelling ASC")
    fun observeByLibrary(libraryId: Long): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE id = :id")
    fun observeById(id: Long): Flow<WordEntity?>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun findById(id: Long): WordEntity?

    @Query("SELECT * FROM words WHERE nextReviewAt <= :now ORDER BY nextReviewAt ASC")
    suspend fun dueAt(now: Long): List<WordEntity>

    @Query("SELECT COUNT(*) FROM words WHERE libraryId = :libraryId")
    fun observeCount(libraryId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE libraryId = :libraryId AND nextReviewAt <= :now")
    fun observeDueCount(libraryId: Long, now: Long): Flow<Int>

    @Insert
    suspend fun insert(word: WordEntity): Long

    @Insert
    suspend fun insertAll(words: List<WordEntity>): List<Long>

    @Update
    suspend fun update(word: WordEntity)

    @Delete
    suspend fun delete(word: WordEntity)
}

@Dao
interface ReviewLogDao {

    @Query("SELECT * FROM review_logs WHERE wordId = :wordId ORDER BY reviewedAt DESC")
    fun observeForWord(wordId: Long): Flow<List<ReviewLogEntity>>

    @Insert
    suspend fun insert(log: ReviewLogEntity): Long

    @Query("SELECT COUNT(*) FROM review_logs WHERE reviewedAt >= :since")
    fun observeCountSince(since: Long): Flow<Int>
}
