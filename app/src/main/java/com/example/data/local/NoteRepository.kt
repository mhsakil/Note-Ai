package com.example.data.local

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    // --- Notes ---
    val allActiveNotes: Flow<List<NoteEntity>> = noteDao.getAllActiveNotes()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    val favoriteNotes: Flow<List<NoteEntity>> = noteDao.getFavoriteNotes()

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    suspend fun getNoteById(id: Int): NoteEntity? = noteDao.getNoteById(id)

    suspend fun insertNote(note: NoteEntity): Long {
        val rowId = noteDao.insertNote(note)
        insertLog("Created / Updated Note", "Saved note ID: ${note.id} named '${note.title}'")
        return rowId
    }

    suspend fun deleteNote(note: NoteEntity) {
        noteDao.deleteNote(note)
        insertLog("Deleted Note", "Removed note ID: ${note.id} named '${note.title}'")
    }

    suspend fun setArchived(id: Int, isArchived: Boolean) {
        noteDao.setArchived(id, isArchived)
        val stringStatus = if (isArchived) "Archived" else "Restored from Archive"
        insertLog("$stringStatus Note", "Updated note ID: $id")
    }

    suspend fun setPinned(id: Int, isPinned: Boolean) {
        noteDao.setPinned(id, isPinned)
        val stringStatus = if (isPinned) "Pinned" else "Unpinned"
        insertLog("$stringStatus Note", "Updated note ID: $id")
    }

    suspend fun setFavorite(id: Int, isFavorite: Boolean) {
        noteDao.setFavorite(id, isFavorite)
        val stringStatus = if (isFavorite) "Added to Favorites" else "Removed from Favorites"
        insertLog("$stringStatus Note", "Updated note ID: $id")
    }

    // --- Notebooks ---
    val allNotebooks: Flow<List<NotebookEntity>> = noteDao.getAllNotebooks()

    suspend fun insertNotebook(notebook: NotebookEntity): Long {
        val id = noteDao.insertNotebook(notebook)
        insertLog("Created Notebook", "Saved notebook ID: $id named '${notebook.name}'")
        return id
    }

    suspend fun deleteNotebook(notebook: NotebookEntity) {
        noteDao.deleteNotebook(notebook)
        insertLog("Deleted Notebook", "Removed notebook ID: ${notebook.id} named '${notebook.name}'")
    }

    // --- Tasks ---
    val allTasks: Flow<List<TaskEntity>> = noteDao.getAllTasks()

    suspend fun insertTask(task: TaskEntity): Long {
        val id = noteDao.insertTask(task)
        insertLog("Saved Task", "Saved task: '${task.title}' status: '${task.status}'")
        return id
    }

    suspend fun updateTask(task: TaskEntity) {
        noteDao.updateTask(task)
        insertLog("Updated Task", "Updated task ID: ${task.id} named '${task.title}'")
    }

    suspend fun deleteTask(task: TaskEntity) {
        noteDao.deleteTask(task)
        insertLog("Deleted Task", "Removed task ID: ${task.id} named '${task.title}'")
    }

    // --- Flashcards ---
    val allFlashcards: Flow<List<FlashcardEntity>> = noteDao.getAllFlashcards()

    fun getFlashcardsForNote(noteId: Int): Flow<List<FlashcardEntity>> = noteDao.getFlashcardsForNote(noteId)

    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long {
        return noteDao.insertFlashcard(flashcard)
    }

    suspend fun deleteFlashcard(flashcard: FlashcardEntity) {
        noteDao.deleteFlashcard(flashcard)
    }

    // --- Logs ---
    val recentActivityLogs: Flow<List<ActivityLogEntity>> = noteDao.getRecentActivityLogs()

    suspend fun insertLog(action: String, details: String) {
        noteDao.insertActivityLog(ActivityLogEntity(action = action, details = details))
    }
}
