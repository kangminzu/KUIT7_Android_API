package com.example.kuit7th_api_practice.ui.post.state

sealed interface PostEvent {
    data object PostCreated : PostEvent
    data object PostUpdated : PostEvent
    data object PostDeleted : PostEvent
    data class ShowSnackbar(val message: String) : PostEvent
}
