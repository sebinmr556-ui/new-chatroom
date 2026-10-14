package com.example.vibe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

@Composable
fun AuthScreen(vm: AppViewModel) {
    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val message by vm.message.collectAsState()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Vibe", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Text("Share your world", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(28.dp))
        if (isRegister) {
            OutlinedTextField(name, { name = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Password (6+ characters)") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (isRegister) vm.register(name, email, password) else vm.signIn(email, password)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (isRegister) "Create account" else "Sign in") }
        TextButton(onClick = { isRegister = !isRegister }) {
            Text(if (isRegister) "Already have an account? Sign in" else "New here? Create an account")
        }
        message?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = vm::clearMessage) { Text("Dismiss") }
        }
    }
}

@Composable
fun SocialHomeScreen(vm: AppViewModel, uid: String, email: String) {
    var selectedTab by remember { mutableStateOf(0) }
    var postText by remember { mutableStateOf("") }
    var storyText by remember { mutableStateOf("") }
    var showStoryDialog by remember { mutableStateOf(false) }
    val posts by vm.posts.collectAsState()
    val stories by vm.stories.collectAsState()
    val message by vm.message.collectAsState()

    Scaffold(
        topBar = {
            TopAppBarCompat(
                title = if (selectedTab == 0) "Vibe Feed" else if (selectedTab == 1) "Create" else "Profile",
                action = { IconButton(onClick = vm::signOut) { Icon(Icons.Default.ExitToApp, contentDescription = "Sign out") } }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selectedTab == 0, { selectedTab = 0 }, icon = { Icon(Icons.Default.FavoriteBorder, null) }, label = { Text("Feed") })
                NavigationBarItem(selectedTab == 1, { selectedTab = 1 }, icon = { Icon(Icons.Default.Add, null) }, label = { Text("Create") })
                NavigationBarItem(selectedTab == 2, { selectedTab = 2 }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile") })
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                item {
                    StoriesStrip(stories = stories, onAdd = { showStoryDialog = true }, currentUid = uid)
                }
                item {
                    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("What's happening?", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = postText, onValueChange = { if (it.length <= 2000) postText = it },
                                modifier = Modifier.fillMaxWidth(), placeholder = { Text("Write a post…") }
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showStoryDialog = true }) { Text("Add story") }
                                Button(onClick = { vm.createPost(postText); postText = "" }, enabled = postText.isNotBlank()) { Text("Post") }
                            }
                        }
                    }
                }
                if (posts.isEmpty()) item { EmptyState("No posts yet", "Be the first to share something.") }
                items(posts, key = { it.id }) { PostCard(it) }
            }
            1 -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Create something", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Text("Publish a text post or a 24-hour story from the Feed tab.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { selectedTab = 0 }) { Text("Go to feed") }
            }
            else -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(88.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, modifier = Modifier.size(44.dp)) }
                }
                Spacer(Modifier.height(12.dp))
                Text(email.substringBefore("@").ifBlank { "Vibe user" }, style = MaterialTheme.typography.titleLarge)
                Text(email, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = vm::signOut) { Text("Sign out") }
            }
        }
    }

    if (showStoryDialog) {
        AlertDialog(
            onDismissRequest = { showStoryDialog = false },
            title = { Text("Add a 24-hour story") },
            text = {
                OutlinedTextField(
                    value = storyText,
                    onValueChange = { if (it.length <= 500) storyText = it },
                    label = { Text("Your story") },
                    supportingText = { Text("${storyText.length}/500 characters") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.createStory(storyText)
                    storyText = ""
                    showStoryDialog = false
                }, enabled = storyText.isNotBlank()) { Text("Publish") }
            },
            dismissButton = { TextButton(onClick = { showStoryDialog = false }) { Text("Cancel") } }
        )
    }

    message?.let {
        AlertDialog(
            onDismissRequest = vm::clearMessage,
            title = { Text("Vibe") },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = vm::clearMessage) { Text("OK") } }
        )
    }
}

@Composable
private fun TopAppBarCompat(title: String, action: @Composable () -> Unit) {
    Surface(shadowElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            action()
        }
    }
}

@Composable
fun StoriesStrip(stories: List<SocialStory>, onAdd: () -> Unit, currentUid: String) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Stories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onAdd) { Icon(Icons.Default.Add, null); Text("Add") }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StoryBubble(name = "Your story", isOwn = true, onClick = onAdd)
            stories.distinctBy { it.authorId }.take(8).forEach { story ->
                StoryBubble(name = story.authorName, isOwn = story.authorId == currentUid, onClick = { })
            }
        }
    }
}

@Composable
private fun StoryBubble(name: String, isOwn: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable(onClick = onClick)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isOwn) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(2.dp, if (isOwn) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary),
            modifier = Modifier.size(58.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isOwn) Icon(Icons.Default.Add, contentDescription = "Add story")
                else Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
fun PostCard(post: SocialPost) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(38.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(post.authorName.take(1).uppercase()) }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(post.authorName, fontWeight = FontWeight.SemiBold)
                    Text(formatTime(post.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(post.text, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = "Like")
                Spacer(Modifier.width(5.dp))
                Text("Like", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(20.dp))
                Icon(Icons.Default.Send, contentDescription = "Share")
                Spacer(Modifier.width(5.dp))
                Text("Share", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatTime(time: Long): String {
    if (time <= 0L) return "Just now"
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(time))
}
