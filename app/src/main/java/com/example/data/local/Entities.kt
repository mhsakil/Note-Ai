package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: String, // Hex string or identifier (e.g. "blue", "teal", "amber")
    val icon: String // Emoji or symbol identifier
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val notebookId: Int? = null,
    val title: String,
    val content: String,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val color: String = "dark_slate", // Theme background
    val tags: String = "", // Comma-separated list of tags
    val updateTime: Long = System.currentTimeMillis(),
    val backlinks: String = "", // Comma-separated list of linking/related Note titles/IDs
    val isSynced: Boolean = true,
    val isEncrypted: Boolean = false
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val notebookId: Int? = null,
    val title: String,
    val isDone: Boolean = false,
    val priority: String = "Medium", // Low, Medium, High
    val status: String = "Todo", // Todo, InProgress, Done
    val dueDate: Long? = null
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noteId: Int,
    val question: String,
    val answer: String,
    val progress: Int = 0, // 0 to 100
    val lastReviewed: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userName: String = "Professional AI Partner",
    val action: String,
    val details: String
)
