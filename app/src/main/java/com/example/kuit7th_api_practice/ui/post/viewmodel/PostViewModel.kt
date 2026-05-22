package com.example.kuit7th_api_practice.ui.post.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kuit7th_api_practice.domain.repository.PostRepository
import com.example.kuit7th_api_practice.domain.repository.model.Post
import com.example.kuit7th_api_practice.ui.post.state.PostCreateFormState
import com.example.kuit7th_api_practice.ui.post.state.PostCreateUiState
import com.example.kuit7th_api_practice.ui.post.state.PostDetailUiState
import com.example.kuit7th_api_practice.ui.post.state.PostEditFormState
import com.example.kuit7th_api_practice.ui.post.state.PostEditUiState
import com.example.kuit7th_api_practice.ui.post.state.PostEvent
import com.example.kuit7th_api_practice.ui.post.state.PostUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PostUiState>(PostUiState.Idle)
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    private val _postDetailUiState = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Loading)
    val postDetailUiState: StateFlow<PostDetailUiState> = _postDetailUiState.asStateFlow()

    private val _postCreateUiState = MutableStateFlow<PostCreateUiState>(PostCreateUiState.Idle)
    val postCreateUiState: StateFlow<PostCreateUiState> = _postCreateUiState.asStateFlow()

    private val _postEditUiState = MutableStateFlow<PostEditUiState>(PostEditUiState.Loading)
    val postEditUiState: StateFlow<PostEditUiState> = _postEditUiState.asStateFlow()

    private val _postCreateFormState = MutableStateFlow(PostCreateFormState())
    val postCreateFormState: StateFlow<PostCreateFormState> = _postCreateFormState.asStateFlow()

    private val _postEditFormState = MutableStateFlow(PostEditFormState())
    val postEditFormState: StateFlow<PostEditFormState> = _postEditFormState.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _eventFlow = MutableSharedFlow<PostEvent>()
    val eventFlow: SharedFlow<PostEvent> = _eventFlow.asSharedFlow()

    private val createdPosts = mutableMapOf<Int, Post>()
    private val updatedPosts = mutableMapOf<Int, Post>()
    private val deletedPostIds = mutableSetOf<Int>()

    fun fetchPosts(userId: Int? = null) {
        _uiState.value = PostUiState.Loading

        viewModelScope.launch {
            postRepository.getPosts(userId)
                .map { posts -> applyLocalChanges(posts, userId) }
                .catch { error ->
                    _uiState.value = PostUiState.Error(error.message ?: "Failed to load posts.")
                }
                .collect { posts ->
                    _uiState.value = PostUiState.Success(posts)
                }
        }
    }

    fun getPostDetail(postId: Long) {
        _postDetailUiState.value = PostDetailUiState.Loading
        _postEditUiState.value = PostEditUiState.Loading

        val localPost = findLocalPost(postId.toInt())
        if (localPost != null) {
            showPostDetail(localPost)
            return
        }

        if (deletedPostIds.contains(postId.toInt())) {
            val message = "Deleted post."
            _postDetailUiState.value = PostDetailUiState.Error(message)
            _postEditUiState.value = PostEditUiState.Error(message)
            return
        }

        viewModelScope.launch {
            postRepository.getPost(postId.toInt())
                .catch { error ->
                    val message = error.message ?: "Failed to load post."
                    _postDetailUiState.value = PostDetailUiState.Error(message)
                    _postEditUiState.value = PostEditUiState.Error(message)
                }
                .collect { post ->
                    showPostDetail(post)
                }
        }
    }

    fun createPost() {
        viewModelScope.launch {
            _isUploading.value = true
            _postCreateUiState.value = PostCreateUiState.Saving

            runCatching {
                val formState = postCreateFormState.value
                postRepository.createPost(
                    title = formState.title,
                    body = formState.content,
                    userId = formState.author.toIntOrNull() ?: 1
                )
            }.onSuccess { post ->
                createdPosts[post.id] = post
                _postCreateUiState.value = PostCreateUiState.Success(post)
                _postCreateFormState.value = PostCreateFormState()
                addOrReplacePostInList(post)

                _eventFlow.emit(PostEvent.PostCreated)
            }.onFailure { error ->
                val message = error.message ?: "Failed to create post."
                _postCreateUiState.value = PostCreateUiState.Error(message)
                _eventFlow.emit(PostEvent.ShowSnackbar(message))
            }

            _isUploading.value = false
        }
    }

    fun updatePost(postId: Long) {
        viewModelScope.launch {
            _isUploading.value = true
            _postEditUiState.value = PostEditUiState.Saving

            val formState = postEditFormState.value
            val localPost = createdPosts[postId.toInt()]
            if (localPost != null) {
                val updatedPost = localPost.copy(
                    title = formState.title,
                    body = formState.body,
                    userId = formState.userId
                )
                createdPosts[updatedPost.id] = updatedPost
                _postEditUiState.value = PostEditUiState.Success(updatedPost)
                _postDetailUiState.value = PostDetailUiState.Success(updatedPost)
                addOrReplacePostInList(updatedPost)
                _isUploading.value = false
                _eventFlow.emit(PostEvent.PostUpdated)
                return@launch
            }

            runCatching {
                postRepository.updatePost(
                    id = postId.toInt(),
                    title = formState.title,
                    body = formState.body,
                    userId = formState.userId
                )
            }.onSuccess { post ->
                updatedPosts[post.id] = post
                _postEditUiState.value = PostEditUiState.Success(post)
                _postDetailUiState.value = PostDetailUiState.Success(post)
                addOrReplacePostInList(post)
                _eventFlow.emit(PostEvent.PostUpdated)
            }.onFailure { error ->
                val message = error.message ?: "Failed to update post."
                _postEditUiState.value = PostEditUiState.Error(message)
                _eventFlow.emit(PostEvent.ShowSnackbar(message))
            }

            _isUploading.value = false
        }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch {
            if (createdPosts.containsKey(postId.toInt())) {
                deletedPostIds.add(postId.toInt())
                createdPosts.remove(postId.toInt())
                removePostFromList(postId.toInt())
                _eventFlow.emit(PostEvent.PostDeleted)
                return@launch
            }

            runCatching {
                postRepository.deletePost(postId.toInt())
            }.onSuccess {
                deletedPostIds.add(postId.toInt())
                createdPosts.remove(postId.toInt())
                updatedPosts.remove(postId.toInt())
                removePostFromList(postId.toInt())
                _eventFlow.emit(PostEvent.PostDeleted)
            }.onFailure { error ->
                val message = error.message ?: "Failed to delete post."
                _postDetailUiState.value = PostDetailUiState.Error(message)
                _eventFlow.emit(PostEvent.ShowSnackbar(message))
            }
        }
    }

    fun onUpdateAuthor(author: String) {
        _postCreateFormState.value = postCreateFormState.value.copy(author = author)
    }

    fun onUpdateTitle(title: String) {
        _postCreateFormState.value = postCreateFormState.value.copy(title = title)
    }

    fun onUpdateContent(content: String) {
        _postCreateFormState.value = postCreateFormState.value.copy(content = content)
    }

    fun onUpdateSelectedImageUri(selectedImageUri: Uri?) {
        _postCreateFormState.value = postCreateFormState.value.copy(
            selectedImageUri = selectedImageUri?.toString()
        )
    }

    fun onUpdateEditTitle(title: String) {
        _postEditFormState.value = postEditFormState.value.copy(title = title)
    }

    fun onUpdateEditContent(content: String) {
        _postEditFormState.value = postEditFormState.value.copy(body = content)
    }

    fun onUpdateEditSelectedImageUri(selectedImageUri: Uri?) = Unit

    fun onClearEditImages() = Unit

    private fun initializeEditForm(
        postId: Int,
        title: String,
        body: String,
        userId: Int,
        force: Boolean = false
    ) {
        if (!force && postEditFormState.value.initializedPostId == postId) return

        _postEditFormState.value = PostEditFormState(
            title = title,
            body = body,
            userId = userId,
            initializedPostId = postId
        )
    }

    private fun showPostDetail(post: Post) {
        _postDetailUiState.value = PostDetailUiState.Success(post)
        _postEditUiState.value = PostEditUiState.Ready(post)
        initializeEditForm(
            postId = post.id,
            title = post.title,
            body = post.body,
            userId = post.userId
        )
    }

    private fun findLocalPost(postId: Int): Post? =
        updatedPosts[postId] ?: createdPosts[postId]

    private fun applyLocalChanges(posts: List<Post>, userId: Int?): List<Post> {
        val serverPosts = posts
            .filterNot { deletedPostIds.contains(it.id) }
            .map { post -> updatedPosts[post.id] ?: post }

        val localCreatedPosts = createdPosts.values
            .filterNot { deletedPostIds.contains(it.id) }
            .filter { userId == null || it.userId == userId }

        return (localCreatedPosts + serverPosts)
            .distinctBy { it.id }
            .sortedByDescending { it.id }
    }

    private fun addOrReplacePostInList(post: Post) {
        val currentState = uiState.value
        if (currentState !is PostUiState.Success) return

        val posts = currentState.posts
            .filterNot { it.id == post.id }

        _uiState.value = PostUiState.Success(
            (listOf(post) + posts)
                .filterNot { deletedPostIds.contains(it.id) }
                .sortedByDescending { it.id }
        )
    }

    private fun removePostFromList(postId: Int) {
        val currentState = uiState.value
        if (currentState is PostUiState.Success) {
            _uiState.value = PostUiState.Success(
                currentState.posts.filterNot { it.id == postId }
            )
        }
    }
}
