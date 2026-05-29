package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import com.example.data.local.*
import com.example.ui.viewmodel.NoteViewModel

enum class WorkspaceSection {
    NOTES, DATABASE, GRAPH_VIEW, KANBAN, STUDY, ADMIN
}

@Composable
fun PulsingBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = alpha))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHostScreen(viewModel: NoteViewModel) {
    var currentSection by remember { mutableStateOf(WorkspaceSection.NOTES) }

    // Collect Reactive Flows from ViewModel
    val activeNotes by viewModel.activeNotes.collectAsState()
    val notebooks by viewModel.notebooks.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()

    val selectedNote by viewModel.selectedNote.collectAsState()
    val selectedNotebookId by viewModel.selectedNotebookId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val userTier by viewModel.userTier.collectAsState()

    // Filtering active notes based on search query and folder/notebook filter
    val filteredNotes = remember(activeNotes, searchQuery, selectedNotebookId) {
        activeNotes.filter { note ->
            val matchesSearch = note.title.contains(searchQuery, ignoreCase = true) || 
                                note.content.contains(searchQuery, ignoreCase = true) ||
                                note.tags.contains(searchQuery, ignoreCase = true)
            val matchesNotebook = selectedNotebookId == null || note.notebookId == selectedNotebookId
            matchesSearch && matchesNotebook
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🧠", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "SmartNote Pro",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "AI ENTERPRISE EDITION • $userTier".uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.25.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                actions = {
                    // Subscription upgrade quick chip
                    FilledTonalButton(
                        onClick = {
                            if (userTier == "Free") {
                                viewModel.setSubscriptionTier("Pro")
                            } else if (userTier == "Pro") {
                                viewModel.setSubscriptionTier("Enterprise")
                            } else {
                                viewModel.setSubscriptionTier("Free")
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.padding(end = 8.dp).testTag("billing_tier_chip")
                    ) {
                        Text(
                            text = if (userTier == "Free") "Go Pro" else if (userTier == "Pro") "Go Enterprise" else "Enterprise tier Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Global Sync status
                    IconButton(
                        onClick = { viewModel.syncCloudSimulation() },
                        modifier = Modifier.testTag("sync_action_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Data")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
            Divider(modifier = Modifier.height(1.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }
        },
        bottomBar = {
            // Elegant navigation bar with consistent active states
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentSection == WorkspaceSection.NOTES,
                    onClick = { currentSection = WorkspaceSection.NOTES },
                    icon = { Text("📝", fontSize = 20.sp) },
                    label = { Text("Notes", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_notes")
                )
                NavigationBarItem(
                    selected = currentSection == WorkspaceSection.DATABASE,
                    onClick = { currentSection = WorkspaceSection.DATABASE },
                    icon = { Text("📋", fontSize = 20.sp) },
                    label = { Text("Database", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_database")
                )
                NavigationBarItem(
                    selected = currentSection == WorkspaceSection.GRAPH_VIEW,
                    onClick = { currentSection = WorkspaceSection.GRAPH_VIEW },
                    icon = { Text("🕸️", fontSize = 20.sp) },
                    label = { Text("Graph", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_graph")
                )
                NavigationBarItem(
                    selected = currentSection == WorkspaceSection.KANBAN,
                    onClick = { currentSection = WorkspaceSection.KANBAN },
                    icon = { Text("📅", fontSize = 20.sp) },
                    label = { Text("Planner", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_kanban")
                )
                NavigationBarItem(
                    selected = currentSection == WorkspaceSection.STUDY,
                    onClick = { currentSection = WorkspaceSection.STUDY },
                    icon = { Text("🃏", fontSize = 20.sp) },
                    label = { Text("Cards", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_study")
                )
                NavigationBarItem(
                    selected = currentSection == WorkspaceSection.ADMIN,
                    onClick = { currentSection = WorkspaceSection.ADMIN },
                    icon = { Text("⚙️", fontSize = 20.sp) },
                    label = { Text("Admin", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_admin")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            AnimatedContent(
                targetState = currentSection,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "workspace_screen_fade"
            ) { section ->
                when (section) {
                    WorkspaceSection.NOTES -> NotesWorkspaceScreen(
                        viewModel = viewModel,
                        filteredNotes = filteredNotes,
                        notebooks = notebooks
                    )
                    WorkspaceSection.DATABASE -> NotionDatabaseScreen(
                        viewModel = viewModel,
                        activeNotes = activeNotes,
                        notebooks = notebooks
                    )
                    WorkspaceSection.GRAPH_VIEW -> ObsidianGraphViewScreen(
                        viewModel = viewModel,
                        activeNotes = activeNotes
                    )
                    WorkspaceSection.KANBAN -> KanbanPlannerScreen(
                        viewModel = viewModel,
                        tasks = tasks,
                        notebooks = notebooks
                    )
                    WorkspaceSection.STUDY -> StudyLabScreen(
                        viewModel = viewModel,
                        activeNotes = activeNotes,
                        flashcards = flashcards
                    )
                    WorkspaceSection.ADMIN -> AnalyticsAdminScreen(
                        viewModel = viewModel,
                        activityLogs = activityLogs,
                        activeNotes = activeNotes,
                        tasks = tasks
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. NOTES WORKSPACE SCREEN (Rich Markdown, Cover art, AI writing assistant, Live sidebar chat)
// ==========================================
@Composable
fun NotesWorkspaceScreen(
    viewModel: NoteViewModel,
    filteredNotes: List<NoteEntity>,
    notebooks: List<NotebookEntity>
) {
    val selectedNote by viewModel.selectedNote.collectAsState()
    val selectedNotebookId by viewModel.selectedNotebookId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showNotebookCreateDialog by remember { mutableStateOf(false) }
    var notebookNameInput by remember { mutableStateOf("") }
    var notebookColorInput by remember { mutableStateOf("#38BDF8") }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Column (Notebook Folders & Notes List)
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(0.dp))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
        ) {
            // Search Input Block
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search knowledge base...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .testTag("search_notes_field"),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // Folders Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WORKSPACES / NOTEBOOKS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                IconButton(
                    onClick = { showNotebookCreateDialog = true },
                    modifier = Modifier.size(24.dp).testTag("add_notebook_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Notebook", modifier = Modifier.size(16.dp))
                }
            }

            // Notebook Row list
            LazyColumn(modifier = Modifier.weight(0.4f).padding(horizontal = 4.dp)) {
                item {
                    NotebookSelectionRow(
                        name = "📁 All Workspaces",
                        isSelected = selectedNotebookId == null,
                        color = Color.Gray,
                        onClick = { viewModel.selectNotebook(null) }
                    )
                }
                items(notebooks) { notebook ->
                    NotebookSelectionRow(
                        name = "${notebook.icon} ${notebook.name}",
                        isSelected = selectedNotebookId == notebook.id,
                        color = try { Color(android.graphics.Color.parseColor(notebook.color)) } catch (e: Exception) { MaterialTheme.colorScheme.primary },
                        onClick = { viewModel.selectNotebook(notebook.id) }
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)

            // Notes list under selected Notebook/Folder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PAGES & PAPERS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                IconButton(
                    onClick = { viewModel.createNote("", "", "") },
                    modifier = Modifier.size(24.dp).testTag("add_note_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Note", modifier = Modifier.size(16.dp))
                }
            }

            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notes found.\nTap + to draft.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(0.6f).padding(horizontal = 4.dp)) {
                    items(filteredNotes) { note ->
                        NoteItemRow(
                            note = note,
                            isSelected = selectedNote?.id == note.id,
                            onClick = { viewModel.selectNote(note) },
                            onPinClick = { viewModel.togglePin(note) },
                            onFavClick = { viewModel.toggleFavorite(note) }
                        )
                    }
                }
            }
        }

        // Right Column (The interactive Rich Text Editor & AI Copilot Workspace)
        Column(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
        ) {
            if (selectedNote == null) {
                WorkspaceWelcomeView(
                    onCreateNoteClick = { viewModel.createNote("Drafting Proposal", "# New Proposal Title\n\nOutline items...", "AI, Pitch") }
                )
            } else {
                val current = selectedNote!!
                var editorTitle by remember(current.id) { mutableStateOf(current.title) }
                var editorContent by remember(current.id) { mutableStateOf(current.content) }
                var editorTags by remember(current.id) { mutableStateOf(current.tags) }
                var isEncrypted by remember(current.id) { mutableStateOf(current.isEncrypted) }

                // Quick cover card look & toolbar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏷️ Tags: ${editorTags.ifBlank { "None" }}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Archive tool
                            IconButton(
                                onClick = { viewModel.toggleArchive(current) },
                                modifier = Modifier.size(28.dp).testTag("action_archive")
                            ) {
                                Text("📥", fontSize = 14.sp)
                            }
                            // Delete
                            IconButton(
                                onClick = { viewModel.deleteNotePermanently(current) },
                                modifier = Modifier.size(28.dp).testTag("action_delete")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Note", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // Save indicator
                            FilledTonalButton(
                                onClick = { viewModel.updateSelectedNote(editorTitle, editorContent, editorTags, isEncrypted) },
                                modifier = Modifier.height(30.dp).testTag("save_note_btn"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Sync Draft", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Editor inputs
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    TextField(
                        value = editorTitle,
                        onValueChange = {
                            editorTitle = it
                            viewModel.updateSelectedNote(it, editorContent, editorTags, isEncrypted)
                        },
                        placeholder = { Text("Note Title", style = MaterialTheme.typography.titleLarge) },
                        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_title_field")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏷️", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        TextField(
                            value = editorTags,
                            onValueChange = {
                                editorTags = it
                                viewModel.updateSelectedNote(editorTitle, editorContent, it, isEncrypted)
                            },
                            placeholder = { Text("Add tags...", fontSize = 11.sp) },
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("editor_tags_field")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("E2E Lock", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = isEncrypted,
                                onCheckedChange = {
                                    isEncrypted = it
                                    viewModel.updateSelectedNote(editorTitle, editorContent, editorTags, it)
                                },
                                modifier = Modifier
                                    .scale(0.6f)
                                    .testTag("encrypt_switch")
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                    // Rich Text toolbar helper shortcuts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            TextShortcutBubble("B") { editorContent += " **Bold** " }
                            TextShortcutBubble("I") { editorContent += " *Italic* " }
                            TextShortcutBubble("H1") { editorContent += "\n# Heading 1\n" }
                            TextShortcutBubble("H2") { editorContent += "\n## Heading 2\n" }
                            TextShortcutBubble("List") { editorContent += "\n- Bullet item\n" }
                            TextShortcutBubble("Todo") { editorContent += "\n- [ ] Checklist goal\n" }
                            TextShortcutBubble("Code") { editorContent += "\n```kotlin\n// Code block\n```\n" }
                        }
                    }

                    // Content Field
                    OutlinedTextField(
                        value = editorContent,
                        onValueChange = {
                            editorContent = it
                            viewModel.updateSelectedNote(editorTitle, it, editorTags, isEncrypted)
                        },
                        placeholder = { Text("Start drafting Markdown, math, links, project references here... Use the Gemini Copilot on your right to organize things!") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("editor_content_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }
            }
        }

        // Sliding Right Column: Gemini Research Workspace or Context Assistant Chat
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(0.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            GeminiCopilotActiveTab(viewModel = viewModel)
        }
    }

    // New Notebook dialog
    if (showNotebookCreateDialog) {
        AlertDialog(
            onDismissRequest = { showNotebookCreateDialog = false },
            title = { Text("Create Workspace Folder") },
            text = {
                Column {
                    OutlinedTextField(
                        value = notebookNameInput,
                        onValueChange = { notebookNameInput = it },
                        label = { Text("Notebook Title") },
                        modifier = Modifier.fillMaxWidth().testTag("new_notebook_title")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Tag Color:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        ColorChipSelector("#38BDF8") { notebookColorInput = it }
                        ColorChipSelector("#34A853") { notebookColorInput = it }
                        ColorChipSelector("#FB923C") { notebookColorInput = it }
                        ColorChipSelector("#EC4899") { notebookColorInput = it }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (notebookNameInput.isNotBlank()) {
                        viewModel.createNotebook(notebookNameInput, notebookColorInput, "📁")
                        notebookNameInput = ""
                        showNotebookCreateDialog = false
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotebookCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun NotebookSelectionRow(name: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NoteItemRow(
    note: NoteEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPinClick: () -> Unit,
    onFavClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp)
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(onClick = onPinClick, modifier = Modifier.size(24.dp)) {
                        Text(if (note.isPinned) "📌" else "📍", fontSize = 12.sp)
                    }
                    IconButton(onClick = onFavClick, modifier = Modifier.size(24.dp)) {
                        Text(if (note.isFavorite) "⭐" else "☆", fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = note.content.ifBlank { "No content preview..." },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.tags.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "#${note.tags.split(",").firstOrNull()?.trim()}",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (note.isEncrypted) {
                    Text("🔒 Sec", fontSize = 9.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TextShortcutBubble(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ColorChipSelector(colorHex: String, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(android.graphics.Color.parseColor(colorHex)))
            .clickable { onClick(colorHex) }
    )
}

@Composable
fun WorkspaceWelcomeView(onCreateNoteClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(420.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text("👋", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Knowledge Center Terminal",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Welcome to SmartNote Pro. Start by selecting a page/paper on the notebook sidebar or tap create below to spawn a grounded research document.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onCreateNoteClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Spawn Research Draft", fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ==========================================
// GEMINI COPILOT ACTIVE PANEL TAB (Research tool + Chat engine)
// ==========================================
@Composable
fun GeminiCopilotActiveTab(viewModel: NoteViewModel) {
    val selectedNote by viewModel.selectedNote.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()

    var chatInputField by remember { mutableStateOf("") }
    var copilotSubTab by remember { mutableStateOf("ASSIST") } // "ASSIST" or "CHAT"

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Headers
        TabRow(
            selectedTabIndex = if (copilotSubTab == "ASSIST") 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = copilotSubTab == "ASSIST",
                onClick = { copilotSubTab = "ASSIST" }
            ) {
                Box(modifier = Modifier.padding(12.dp)) { Text("Assistant", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
            Tab(
                selected = copilotSubTab == "CHAT",
                onClick = { copilotSubTab = "CHAT" }
            ) {
                Box(modifier = Modifier.padding(12.dp)) { Text("NoteLM Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }

        if (copilotSubTab == "ASSIST") {
            // Action Assistant triggers
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ASK INTELLIGENCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.25.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingBadge()
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "LIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.tertiary,
                                letterSpacing = 1.1.sp
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActionAssistantButton("Summarize Draft", "summarize", viewModel, selectedNote, Modifier.weight(1f))
                            ActionAssistantButton("Grammar Proof", "grammar", viewModel, selectedNote, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActionAssistantButton("Expand Material", "expand", viewModel, selectedNote, Modifier.weight(1f))
                            ActionAssistantButton("Outline Plan", "outline", viewModel, selectedNote, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActionAssistantButton("Academic Tone", "academic", viewModel, selectedNote, Modifier.weight(1f))
                            ActionAssistantButton("Business Pitch", "business", viewModel, selectedNote, Modifier.weight(1f))
                        }
                    }
                }

                item {
                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    Text(
                        text = "AI OUTPUT RESULTS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 180.dp)
                            .padding(top = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(16.dp)
                            )
                            .border(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        if (isAiLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center).size(24.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (aiResponse != null) {
                            Column {
                                SelectionContainer {
                                    Text(
                                        text = aiResponse!!,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        // Quickly append output to currently selected Note
                                        selectedNote?.let { n ->
                                            viewModel.updateSelectedNote(
                                                n.title,
                                                n.content + "\n\n### AI Generation Attachment\n" + aiResponse!!,
                                                n.tags,
                                                n.isEncrypted
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Insert Into Draft", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(
                                text = "Select a workspace note and call one of the Gemini actions above to start processing.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        } else {
            // Context Grounded Chat Workspace
            Column(modifier = Modifier.weight(1f).padding(8.dp)) {
                Text(
                    text = "Grounded context model query. Ask questions referring strictly to note context.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Conversation body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(4.dp)
                ) {
                    if (chatMessages.isEmpty()) {
                        Text(
                            "Dialogue history is empty.\nAsk about the elements inside this note.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.align(Alignment.Center),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(chatMessages) { pair ->
                                ChatBubbleView(message = pair.first, isUser = pair.second)
                            }
                        }
                    }
                }

                // Chat Input Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatInputField,
                        onValueChange = { chatInputField = it },
                        modifier = Modifier.weight(1f).testTag("chat_input_field"),
                        placeholder = { Text("Ask note details...", fontSize = 11.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            if (chatInputField.isNotBlank()) {
                                viewModel.sendChatMessage(chatInputField, selectedNote?.content)
                                chatInputField = ""
                            }
                        },
                        modifier = Modifier.testTag("send_chat_msg_btn")
                    ) {
                        Text("🚀", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ActionAssistantButton(
    label: String,
    action: String,
    viewModel: NoteViewModel,
    selectedNote: NoteEntity?,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            if (selectedNote != null) {
                viewModel.runWriterAssistant(action, selectedNote.content)
            }
        },
        modifier = modifier
            .height(40.dp)
            .testTag("ai_chip_$action"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        enabled = selectedNote != null
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ChatBubbleView(message: String, isUser: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 2.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 210.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(8.dp)
        ) {
            Text(message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}


// ==========================================
// 2. NOTION DATABASE VIEW SCREEN (Sortable database tables & customizable database parameters)
// ==========================================
@Composable
fun NotionDatabaseScreen(
    viewModel: NoteViewModel,
    activeNotes: List<NoteEntity>,
    notebooks: List<NotebookEntity>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📊 Notion-style Relational Databases",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Overview table showing relations, updates, cloud backups, and custom database indices.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Button(
                onClick = { viewModel.createNote("Draft Database Record", "# Database Note Entry\nContent here...", "Database, Notion") },
                modifier = Modifier.testTag("db_add_row_btn")
            ) {
                Text("Insert Record", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Table headers Custom Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Title & Relations", modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Parent Folder", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Sync Status", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Encryption", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Priority Star", modifier = Modifier.weight(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (activeNotes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No database documents spawned yet. Add records above.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(activeNotes) { note ->
                    val matchedFolder = notebooks.find { it.id == note.notebookId }?.name ?: "📦 Default Sandbox"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .clickable { viewModel.selectNote(note) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = note.title,
                            modifier = Modifier.weight(1.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = matchedFolder,
                            modifier = Modifier.weight(1f),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Row(modifier = Modifier.weight(0.8f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (note.isSynced) Color.Green else Color.Red)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (note.isSynced) "Synced" else "Local DB", fontSize = 11.sp)
                        }
                        Text(
                            text = if (note.isEncrypted) "AES SSL Secured" else "Plain Draft",
                            modifier = Modifier.weight(0.8f),
                            fontSize = 11.sp,
                            color = if (note.isEncrypted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        IconButton(
                            onClick = { viewModel.togglePin(note) },
                            modifier = Modifier.weight(0.6f).size(24.dp)
                        ) {
                            Text(if (note.isPinned) "⭐" else "☆", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}


// ==========================================
// 3. OBSIDIAN-STYLE GRAPH VIEW SCREEN (Wiki linkages, bidirectional graph drawing visual nodes)
// ==========================================
@Composable
fun ObsidianGraphViewScreen(viewModel: NoteViewModel, activeNotes: List<NoteEntity>) {
    var chosenNode by remember { mutableStateOf<NoteEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🕸️ Obsidian Graph View & Wiki Linkages",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Visual relationship grid showing semantic connections and Wiki link associations between documents.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Interconnected Graph View Sandbox
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary

            // Layout pre-computation for simple circular arrangement
            val nodes = remember(activeNotes) {
                activeNotes.mapIndexed { index, _ ->
                    val angle = (index * 2 * Math.PI) / if (activeNotes.isEmpty()) 1 else activeNotes.size
                    // Node coordinate offsets
                    Offset(
                        x = (320 + 200 * Math.cos(angle)).toFloat(),
                        y = (240 + 200 * Math.sin(angle)).toFloat()
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(activeNotes) {
                        detectDragGestures { change, _ ->
                            change.consume()
                        }
                    }
                    .testTag("obsidian_graph_canvas")
            ) {
                // 1. Draw connecting web lines
                for (i in activeNotes.indices) {
                    for (j in (i + 1) until activeNotes.size) {
                        // Check if notes share tags or keywords for connection
                        val shareTags = activeNotes[i].tags.split(",").any { it.isNotBlank() && activeNotes[j].tags.contains(it) }
                        if (shareTags || i == 0) {
                            drawLine(
                                color = primaryColor.copy(alpha = 0.15f),
                                start = nodes.getOrElse(i) { Offset.Zero },
                                end = nodes.getOrElse(j) { Offset.Zero },
                                strokeWidth = 3f
                            )
                        }
                    }
                }

                // 2. Draw nodes themselves
                nodes.forEachIndexed { idx, offset ->
                    drawCircle(
                        color = if (idx == 0) tertiaryColor else primaryColor,
                        radius = 24f,
                        center = offset
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.6f),
                        radius = 24f,
                        center = offset,
                        style = Stroke(width = 3f)
                    )
                }
            }

            // Click listener overlay using Compose boxes containing elements
            activeNotes.forEachIndexed { index, note ->
                val offset = nodes.getOrElse(index) { Offset.Zero }
                Box(
                    modifier = Modifier
                        .offset(
                            x = (offset.x / 2.5f).dp,
                            y = (offset.y / 2.5f).dp
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable { chosenNode = note }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = note.title.take(16),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Dialog for detail viewing
            chosenNode?.let { node ->
                AlertDialog(
                    onDismissRequest = { chosenNode = null },
                    title = { Text(node.title, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Note reference summary", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(node.content.take(150) + "...")
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.selectNote(node)
                            chosenNode = null
                        }) {
                            Text("Open in Editor")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { chosenNode = null }) { Text("Dismiss") }
                    }
                )
            }
        }
    }
}


// ==========================================
// 4. KANBAN PLANNER / WORKSPACE TASKS (Drag-drop column moves, daily schedule, habit trackers)
// ==========================================
@Composable
fun KanbanPlannerScreen(
    viewModel: NoteViewModel,
    tasks: List<TaskEntity>,
    notebooks: List<NotebookEntity>
) {
    var taskTitleInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Planner Input Box card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("add_task_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "📅 Projects Kanban Planner & Daily Goals",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = taskTitleInput,
                        onValueChange = { taskTitleInput = it },
                        modifier = Modifier.weight(1f).testTag("task_title_field"),
                        placeholder = { Text("Add project to-do items...", fontSize = 13.sp) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (taskTitleInput.isNotBlank()) {
                                viewModel.addTask(taskTitleInput, "Medium")
                                taskTitleInput = ""
                            }
                        },
                        modifier = Modifier.testTag("add_task_btn")
                    ) {
                        Text("Add")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Columns holder: Todo, InProgress, Done
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KanbanColumn("📌 To Do", tasks.filter { it.status == "Todo" }, viewModel, "Todo")
            KanbanColumn("⚡ In Progress", tasks.filter { it.status == "InProgress" }, viewModel, "InProgress")
            KanbanColumn("✅ Completed", tasks.filter { it.status == "Done" }, viewModel, "Done")
        }
    }
}

@Composable
fun RowScope.KanbanColumn(
    header: String,
    tasks: List<TaskEntity>,
    viewModel: NoteViewModel,
    columnStatus: String
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Text(
            text = "$header (${tasks.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(tasks) { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("task_card_${task.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = task.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Priority: ${task.priority}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Row {
                                if (columnStatus != "Todo") {
                                    IconButton(
                                        onClick = { viewModel.updateTaskState(task, "Todo") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("⬅️", fontSize = 11.sp)
                                    }
                                }
                                if (columnStatus != "Done") {
                                    IconButton(
                                        onClick = {
                                            val next = if (columnStatus == "Todo") "InProgress" else "Done"
                                            viewModel.updateTaskState(task, next)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("➡️", fontSize = 11.sp)
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.deleteTask(task) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("❌", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 5. STUDY LAB & EXAM PREPARATION SCREEN (MCQ generated study progress)
// ==========================================
@Composable
fun StudyLabScreen(
    viewModel: NoteViewModel,
    activeNotes: List<NoteEntity>,
    flashcards: List<FlashcardEntity>
) {
    var activeCardIndex by remember { mutableStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🃏 Exam Preparation Labs & Flashcards",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Grounded exam preparation deck containing MCQ systems powered by note indexing.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            TextButton(
                onClick = {
                    viewModel.generateFlashcard(
                        "What is Superposition context defined in SmartNotes?",
                        "Representing 2^N quantum states altogether simultaneously using probability vectors.",
                        1
                    )
                },
                modifier = Modifier.testTag("gen_flashcard_helper")
            ) {
                Text("Simulate Core Flashcard")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large card play engine
        if (flashcards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No study cards created. Tap Simulation button above or study a specific note.", fontSize = 12.sp, color = Color.Gray)
            }
        } else {
            val card = flashcards.getOrNull(activeCardIndex) ?: flashcards.first()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "STUDY CARD DETAILED PREVIEW (${activeCardIndex + 1} / ${flashcards.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "QUESTION:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = card.question,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (showAnswer) {
                            Text(
                                text = "ANSWER CONCEPT:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = card.answer,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.tertiary,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Button(
                                onClick = { showAnswer = true },
                                modifier = Modifier.testTag("reveal_ans_btn")
                            ) {
                                Text("Reveal Concept Answer")
                            }
                        }
                    }

                    Row {
                        Button(
                            onClick = {
                                showAnswer = false
                                if (activeCardIndex > 0) activeCardIndex-- else activeCardIndex = flashcards.size - 1
                            }
                        ) {
                            Text("Previous")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                showAnswer = false
                                if (activeCardIndex < flashcards.size - 1) activeCardIndex++ else activeCardIndex = 0
                            }
                        ) {
                            Text("Next Deck")
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 6. ANALYTICS & SECURITY ADMIN SCREEN (Audit logs, Access controls, Biometric simulation)
// ==========================================
@Composable
fun AnalyticsAdminScreen(
    viewModel: NoteViewModel,
    activityLogs: List<ActivityLogEntity>,
    activeNotes: List<NoteEntity>,
    tasks: List<TaskEntity>
) {
    var faceIdSecured by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "🛡️ Core Security Center & Corporate Analytics",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Configure end-to-end encryption, workspace biometric policies, and audit system-wide activity metrics.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Grid showing simple quantitative stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminCardChip("Active Pages", "${activeNotes.size} docs", Modifier.weight(1f))
            AdminCardChip("Task Planner", "${tasks.filter { it.status != "Done" }.size} to-dos", Modifier.weight(1f))
            AdminCardChip("E2E Status", "AES 256 Active", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security controls card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("security_controls"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Security Policies Configuration", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Require FaceID & Biometric Access", fontSize = 12.sp)
                    Switch(
                        checked = faceIdSecured,
                        onCheckedChange = { faceIdSecured = it },
                        modifier = Modifier.scale(0.8f).testTag("face_id_toggle")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Industrial activity audit logs
        Text(
            text = "SYSTEM-WIDE AUDIT WORKSPACE (Recent 50 operations)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(activityLogs) { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "[${log.action.uppercase()}]",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = log.details,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Verified Partner",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun AdminCardChip(label: String, valText: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(valText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
