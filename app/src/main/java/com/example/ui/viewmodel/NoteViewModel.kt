package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.local.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = NoteRepository(database.noteDao)

    // --- State Observables ---
    val activeNotes: StateFlow<List<NoteEntity>> = repository.allActiveNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<NoteEntity>> = repository.archivedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteNotes: StateFlow<List<NoteEntity>> = repository.favoriteNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notebooks: StateFlow<List<NotebookEntity>> = repository.allNotebooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashcards: StateFlow<List<FlashcardEntity>> = repository.allFlashcards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs: StateFlow<List<ActivityLogEntity>> = repository.recentActivityLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Client-side Interactive UI States ---
    private val _selectedNote = MutableStateFlow<NoteEntity?>(null)
    val selectedNote: StateFlow<NoteEntity?> = _selectedNote.asStateFlow()

    private val _selectedNotebookId = MutableStateFlow<Int?>(null)
    val selectedNotebookId: StateFlow<Int?> = _selectedNotebookId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // --- AI Chat History ---
    private val _chatMessages = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList()) // Pair(Message, isUser)
    val chatMessages: StateFlow<List<Pair<String, Boolean>>> = _chatMessages.asStateFlow()

    // --- Subscription Tier ---
    private val _userTier = MutableStateFlow("Free") // "Free", "Pro", "Enterprise"
    val userTier: StateFlow<String> = _userTier.asStateFlow()

    private val _aiTokensLeft = MutableStateFlow(5) // Free users get 5 requests
    val aiTokensLeft: StateFlow<Int> = _aiTokensLeft.asStateFlow()

    init {
        // Populate default notebooks and onboarding notes if empty
        viewModelScope.launch {
            repository.allNotebooks.first().let { currentNotebooks ->
                if (currentNotebooks.isEmpty()) {
                    createDefaultData()
                }
            }
        }
    }

    private suspend fun createDefaultData() {
        val workId = repository.insertNotebook(NotebookEntity(name = "🏢 Work & Projects", color = "0xFF1A73E8", icon = "📁"))
        val personalId = repository.insertNotebook(NotebookEntity(name = "🏡 Personal & Life", color = "0xFF34A853", icon = "💡"))
        val researchId = repository.insertNotebook(NotebookEntity(name = "📓 Research Labs", color = "0xFF9333EA", icon = "🔬"))

        repository.insertNote(NoteEntity(
            notebookId = workId.toInt(),
            title = "✨ Welcome & Guide to SmartNote Pro",
            content = """# SmartNote Pro AI Guide

Welcome to the ultimate enterprise-grade knowledge management platform. Here is how you can use the workspace:

## Core Modules
- **Notes Engine**: Fully rich-text supported markdown notes. Move between folders, tag items, color code, and check off completed action goals.
- **AI Suites**: Run tone corrections, summaries, expansion or academic citations. Directly interact via Chat with the note.
- **Study Mode**: Play MCQs/Quizzes generated specifically from this note context. Correct answer evaluations are stored safely offline.
- **Backlinks Graph (Obsidian-Style)**: Open the visual connectivity node grid to instantly navigate related concepts and wiki backlinks! (Enter tags and note concepts to automatically construct connection hubs).
- **Notion Database Tab**: Switch to the tables to manage properties like priority, synced, and update time.
- **Kanban Board**: Drag to-dos from 'Todo' to 'InProgress' and 'Done'.

Use the prompt controls on the side toolbar to call the Gemini AI Assistant on this onboarding doc!
""",
            isPinned = true,
            color = "deep_indigo",
            tags = "Welcome, Onboarding, Gemini"
        ))

        repository.insertNote(NoteEntity(
            notebookId = researchId.toInt(),
            title = "🔬 Quantum Computing & Multi-world Hypothesis",
            content = """# Quantum Computing Explorations

Investigation on quantum entanglement and decoherence properties.

## Primary Hypothesis
By utilizing superposition, we can represent $2^N$ states simultaneously.
- Link: SmartNote Pro AI Guide
- Link: Research Workspace Concepts

We need a structured outline of the engineering hurdles in silicon-spin cubits.
""",
            isPinned = false,
            color = "purple_vibe",
            tags = "Research, Physics, Superposition"
        ))

        // Populate onboarding tasks
        repository.insertTask(TaskEntity(notebookId = workId.toInt(), title = "Draft annual marketing project schedule", status = "Todo", priority = "High"))
        repository.insertTask(TaskEntity(notebookId = workId.toInt(), title = "Polish presentation slides", status = "InProgress", priority = "Medium"))
        repository.insertTask(TaskEntity(notebookId = personalId.toInt(), title = "Complete mindfulness breathing session", status = "Done", priority = "Low"))

        // Add typical study helper card
        repository.insertFlashcard(FlashcardEntity(noteId = 1, question = "Name the 4 prominent AI platforms SmartNote leverages.", answer = "Gemini AI, Notion rels, Obsidian wiki networks, and Evernote organizers."))
        
        repository.insertLog("System Initialization", "Populated onboarding data to smart database container gracefully.")
    }

    // --- Search & Selection ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectNote(note: NoteEntity?) {
        _selectedNote.value = note
        _aiResponse.value = null
        if (note != null) {
            // Add a log for reading note
            viewModelScope.launch {
                repository.insertLog("Read Note", "Opened note ID: ${note.id} '${note.title}'")
            }
        }
    }

    fun selectNotebook(notebookId: Int?) {
        _selectedNotebookId.value = notebookId
    }

    // --- Notes Management ---
    fun createNote(title: String, content: String, tags: String, notebookId: Int? = null) {
        viewModelScope.launch {
            val newNote = NoteEntity(
                notebookId = notebookId ?: _selectedNotebookId.value,
                title = title.ifBlank { "Untitled Note" },
                content = content,
                tags = tags,
                updateTime = System.currentTimeMillis()
            )
            val noteId = repository.insertNote(newNote)
            val insertedNote = newNote.copy(id = noteId.toInt())
            _selectedNote.value = insertedNote
        }
    }

    fun updateSelectedNote(title: String, content: String, tags: String, isEncrypted: Boolean = false) {
        val current = _selectedNote.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                title = title,
                content = content,
                tags = tags,
                isEncrypted = isEncrypted,
                updateTime = System.currentTimeMillis()
            )
            repository.insertNote(updated)
            _selectedNote.value = updated
        }
    }

    fun selectNoteColor(color: String) {
        val current = _selectedNote.value ?: return
        viewModelScope.launch {
            val updated = current.copy(color = color)
            repository.insertNote(updated)
            _selectedNote.value = updated
        }
    }

    fun duplicateNote(note: NoteEntity) {
        viewModelScope.launch {
            val duplicate = note.copy(
                id = 0,
                title = "${note.title} (Copy)",
                updateTime = System.currentTimeMillis()
            )
            repository.insertNote(duplicate)
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.setPinned(note.id, !note.isPinned)
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = _selectedNote.value?.copy(isPinned = !note.isPinned)
            }
        }
    }

    fun toggleFavorite(note: NoteEntity) {
        viewModelScope.launch {
            repository.setFavorite(note.id, !note.isFavorite)
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = _selectedNote.value?.copy(isFavorite = !note.isFavorite)
            }
        }
    }

    fun toggleArchive(note: NoteEntity) {
        viewModelScope.launch {
            repository.setArchived(note.id, !note.isArchived)
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = null
            }
        }
    }

    fun deleteNotePermanently(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = null
            }
        }
    }

    // --- Notebooks ---
    fun createNotebook(name: String, color: String, icon: String) {
        viewModelScope.launch {
            repository.insertNotebook(NotebookEntity(name = name, color = color, icon = icon))
        }
    }

    fun deleteNotebook(notebook: NotebookEntity) {
        viewModelScope.launch {
            repository.deleteNotebook(notebook)
            if (_selectedNotebookId.value == notebook.id) {
                _selectedNotebookId.value = null
            }
        }
    }

    // --- Task Planner (Kanban / Daily) ---
    fun addTask(title: String, priority: String = "Medium") {
        viewModelScope.launch {
            repository.insertTask(TaskEntity(
                title = title,
                priority = priority,
                notebookId = _selectedNotebookId.value
            ))
        }
    }

    fun updateTaskState(task: TaskEntity, nextStatus: String) {
        viewModelScope.launch {
            repository.updateTask(task.copy(status = nextStatus))
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // --- Flashcards & Study Play ---
    fun generateFlashcard(question: String, answer: String, noteId: Int) {
        viewModelScope.launch {
            repository.insertFlashcard(FlashcardEntity(question = question, answer = answer, noteId = noteId))
        }
    }

    fun deleteFlashcard(flashcard: FlashcardEntity) {
        viewModelScope.launch {
            repository.deleteFlashcard(flashcard)
        }
    }

    // --- AI Integration Suite (Gemini Core Actions) ---
    private fun canUseAi(): Boolean {
        if (_userTier.value != "Free") return true
        if (_aiTokensLeft.value > 0) {
            _aiTokensLeft.value -= 1
            return true
        }
        return false
    }

    fun setSubscriptionTier(tier: String) {
        _userTier.value = tier
        viewModelScope.launch {
            repository.insertLog("Subscription Update", "Upgraded plan successfully to $tier tier for enterprise keys.")
        }
    }

    fun runWriterAssistant(workflow: String, noteDetails: String) {
        if (!canUseAi()) {
            _aiResponse.value = "AI Usage limits exceeded for Free tier. Please upgrade to Pro or Enterprise Plan to unlock unlimited server-side AI processing!"
            return
        }
        _isAiLoading.value = true
        _aiResponse.value = null
        viewModelScope.launch {
            val systemInstruction = "You are a Senior Editor inside SmartNote Pro. Support the workspace formatting completely."
            val prompt = when (workflow) {
                "summarize" -> "Summarize the following note content elegantly. Make a concise summary dashboard:\n\n$noteDetails"
                "grammar" -> "Proofread, correct formatting, grammar issues and elevate elegance and word flow of:\n\n$noteDetails"
                "expand" -> "Expand with structured points and deep industrial context on:\n\n$noteDetails"
                "outline" -> "Construct a detailed bullet outline and timeline project action list for:\n\n$noteDetails"
                "citation" -> "Extract academic citations (APA/IEEE) and related bibliography from note context:\n\n$noteDetails"
                "academic" -> "Rewrite with structured, high-standard Academic tone:\n\n$noteDetails"
                "business" -> "Rewrite into high-impact Professional Business tone:\n\n$noteDetails"
                "quiz" -> "Act as a study professor. Respond in plain JSON arrays representing 3 multiple-choice questions from this text. Each object must have fields 'q' (question), 'options' (array of 4 choices) and 'ans' (correct option index). Input:\n\n$noteDetails"
                else -> "Help organize and beautify:\n\n$noteDetails"
            }

            val result = GeminiClient.callGemini(prompt, systemInstruction)
            _aiResponse.value = result
            _isAiLoading.value = false
            repository.insertLog("AI Assistant Triggered", "Executed workflow '$workflow' successfully via Gemini.")
        }
    }

    fun sendChatMessage(userMsg: String, noteContext: String?) {
        if (userMsg.isBlank()) return
        if (!canUseAi()) {
            _chatMessages.value = _chatMessages.value + Pair("AI Usage limits exceeded for Free tier. Upgrade to Pro/Enterprise in custom settings for unlimited context queries.", false)
            return
        }
        val currentChat = _chatMessages.value + Pair(userMsg, true)
        _chatMessages.value = currentChat
        _isAiLoading.value = true

        viewModelScope.launch {
            val contextPrefix = if (noteContext != null) {
                "You are inside the SmartNote Pro AI Sidebar Chat. We are studying the following note:\n---\n$noteContext\n---\nUser query relative to note context:\n"
            } else {
                "You are inside the SmartNote Pro AI Sidebar Chat. Assist the professional workspace with intelligence.\n"
            }

            val result = GeminiClient.callGemini(contextPrefix + userMsg, "You are a professional research partner and Obsidian knowledge advisor.")
            _chatMessages.value = _chatMessages.value + Pair(result, false)
            _isAiLoading.value = false
            repository.insertLog("AI Conversation", "Chat Assistant response issued gracefully.")
        }
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
    }

    // --- Backups / Encryption ---
    fun syncCloudSimulation() {
        viewModelScope.launch {
            _isAiLoading.value = true
            kotlinx.coroutines.delay(1200) // Simulating visual network delay
            _isAiLoading.value = false
            repository.insertLog("Cloud Sync Triggered", "Forced real-time synchronization successfully across all workspaces.")
        }
    }
}
