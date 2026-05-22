package com.example.kuit7th_api_practice.domain.repository

import com.example.kuit7th_api_practice.domain.repository.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPosts(userId: Int? = null): Flow<List<Post>>
    fun getPost(id: Int): Flow<Post>
    suspend fun createPost(title: String, body: String, userId: Int): Post
    suspend fun updatePost(id: Int, title: String, body: String, userId: Int): Post
    suspend fun deletePost(id: Int)
}
