package com.example.kuit7th_api_practice.data.repositoryimpl

import com.example.kuit7th_api_practice.data.api.PostApiService
import com.example.kuit7th_api_practice.data.dto.toDomain
import com.example.kuit7th_api_practice.data.model.request.PostCreateRequest
import com.example.kuit7th_api_practice.domain.repository.PostRepository
import com.example.kuit7th_api_practice.domain.repository.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val postService: PostApiService
) : PostRepository {
    override fun getPosts(userId: Int?): Flow<List<Post>> = flow {
        emit(postService.getPosts(userId).map { it.toDomain() })
    }

    override fun getPost(id: Int): Flow<Post> = flow {
        emit(postService.getPost(id).toDomain())
    }

    override suspend fun createPost(title: String, body: String, userId: Int): Post =
        postService.createPost(
            PostCreateRequest(
                title = title,
                body = body,
                userId = userId
            )
        ).toDomain()

    override suspend fun updatePost(id: Int, title: String, body: String, userId: Int): Post =
        postService.updatePost(
            id = id,
            request = PostCreateRequest(
                title = title,
                body = body,
                userId = userId
            )
        ).toDomain()

    override suspend fun deletePost(id: Int) {
        postService.deletePost(id)
    }
}
