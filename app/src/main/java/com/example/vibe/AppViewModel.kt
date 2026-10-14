package com.example.vibe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AppViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    private val _posts = MutableStateFlow<List<SocialPost>>(emptyList())
    val posts: StateFlow<List<SocialPost>> = _posts

    private val _stories = MutableStateFlow<List<SocialStory>>(emptyList())
    val stories: StateFlow<List<SocialStory>> = _stories

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        auth.addAuthStateListener { _user.value = it.currentUser }
        listenToPosts()
        listenToStories()
    }

    fun clearMessage() { _message.value = null }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            runCatching {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
            }.onFailure { _message.value = it.localizedMessage ?: "Sign in failed" }
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            runCatching {
                require(name.isNotBlank()) { "Enter a display name." }
                require(password.length >= 6) { "Password must be at least 6 characters." }
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val uid = result.user?.uid ?: error("Could not create account")
                db.collection("users").document(uid).set(
                    mapOf(
                        "uid" to uid,
                        "displayName" to name.trim().take(40),
                        "email" to email.trim(),
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
            }.onFailure { _message.value = it.localizedMessage ?: "Registration failed" }
        }
    }

    fun signOut() = auth.signOut()

    private fun listenToPosts() {
        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _message.value = error.localizedMessage ?: "Could not load feed"
                    return@addSnapshotListener
                }
                _posts.value = snapshot?.documents?.map { d ->
                    SocialPost(
                        id = d.id,
                        authorId = d.getString("authorId").orEmpty(),
                        authorName = d.getString("authorName").orEmpty(),
                        text = d.getString("text").orEmpty(),
                        createdAt = d.getLong("createdAt") ?: 0L
                    )
                }.orEmpty()
            }
    }

    private fun listenToStories() {
        // Expiration is checked on the client for display. Firestore TTL can clean old docs later.
        db.collection("stories")
            .whereGreaterThan("expiresAt", System.currentTimeMillis())
            .orderBy("expiresAt", Query.Direction.ASCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _message.value = error.localizedMessage ?: "Could not load stories"
                    return@addSnapshotListener
                }
                val now = System.currentTimeMillis()
                _stories.value = snapshot?.documents?.mapNotNull { d ->
                    val expires = d.getLong("expiresAt") ?: 0L
                    if (expires <= now) null else SocialStory(
                        id = d.id,
                        authorId = d.getString("authorId").orEmpty(),
                        authorName = d.getString("authorName").orEmpty(),
                        text = d.getString("text").orEmpty(),
                        createdAt = d.getLong("createdAt") ?: 0L,
                        expiresAt = expires
                    )
                }.orEmpty()
            }
    }

    fun createPost(text: String) {
        val current = auth.currentUser ?: return
        val clean = text.trim().take(2000)
        if (clean.isBlank()) {
            _message.value = "Write something before posting."
            return
        }
        viewModelScope.launch {
            runCatching {
                val name = displayName(current.uid, current.email.orEmpty())
                db.collection("posts").add(
                    mapOf(
                        "authorId" to current.uid,
                        "authorName" to name,
                        "text" to clean,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
                _message.value = "Post published"
            }.onFailure { _message.value = it.localizedMessage ?: "Post failed" }
        }
    }

    fun createStory(text: String) {
        val current = auth.currentUser ?: return
        val clean = text.trim().take(500)
        if (clean.isBlank()) {
            _message.value = "Write a story first."
            return
        }
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val name = displayName(current.uid, current.email.orEmpty())
                db.collection("stories").add(
                    mapOf(
                        "authorId" to current.uid,
                        "authorName" to name,
                        "text" to clean,
                        "createdAt" to now,
                        "expiresAt" to now + 24L * 60L * 60L * 1000L
                    )
                ).await()
                _message.value = "Story added for 24 hours"
            }.onFailure { _message.value = it.localizedMessage ?: "Story failed" }
        }
    }

    private suspend fun displayName(uid: String, fallback: String): String {
        val snap = db.collection("users").document(uid).get().await()
        return snap.getString("displayName")?.takeIf { it.isNotBlank() }
            ?: fallback.substringBefore("@").ifBlank { "User" }
    }
}
