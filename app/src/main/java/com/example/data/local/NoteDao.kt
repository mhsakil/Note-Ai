package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // --- NOTES CRUD ---
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY isPinned DESC, updateTime DESC")
    fun getAllActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY updateTime DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 AND isArchived = 0 ORDER BY updateTime DESC")
    fun getFavoriteNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): NoteEntity?

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("UPDATE notes SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Int, isArchived: Boolean)

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Int, isPinned: Boolean)

    @Query("UPDATE notes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Int, isFavorite: Boolean)

    // --- NOTEBOOKS ---
    @Query("SELECT * FROM notebooks ORDER BY name ASC")
    fun getAllNotebooks(): Flow<List<NotebookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebook(notebook: NotebookEntity): Long

    @Delete
    suspend fun deleteNotebook(notebook: NotebookEntity)

    // --- TASKS ---
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    // --- FLASHCARDS ---
    @Query("SELECT * FROM flashcards WHERE noteId = :noteId")
    fun getFlashcardsForNote(noteId: Int): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long

    @Delete
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)

    // --- ACTIVITY LOGS ---
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentActivityLogs(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity): Long
}
