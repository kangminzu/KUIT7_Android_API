package com.example.kuit7th_api_practice.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.kuit7th_api_practice.ui.post.screen.PostCreateScreen
import com.example.kuit7th_api_practice.ui.post.screen.PostDetailScreen
import com.example.kuit7th_api_practice.ui.post.screen.PostEditScreen
import com.example.kuit7th_api_practice.ui.post.screen.PostListScreen
import com.example.kuit7th_api_practice.ui.post.viewmodel.PostViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: Any = PostListRoute
) {
    val activity = LocalContext.current as ComponentActivity
    val postViewModel: PostViewModel = hiltViewModel(activity)

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<PostListRoute> {
            PostListScreen(
                onPostClick = { postId ->
                    navController.navigate(PostDetailRoute(postId))
                },
                onCreatePostClick = {
                    navController.navigate(PostCreateRoute)
                },
                viewModel = postViewModel
            )
        }

        composable<PostDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PostDetailRoute>()

            PostDetailScreen(
                postId = route.postId,
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { postId ->
                    navController.navigate(PostEditRoute(postId))
                },
                viewModel = postViewModel
            )
        }

        composable<PostCreateRoute> {
            PostCreateScreen(
                onNavigateBack = { navController.popBackStack() },
                onPostCreated = { navController.popBackStack() },
                viewModel = postViewModel
            )
        }

        composable<PostEditRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PostEditRoute>()

            PostEditScreen(
                postId = route.postId,
                onNavigateBack = { navController.popBackStack() },
                onPostUpdated = { navController.popBackStack() },
                viewModel = postViewModel
            )
        }
    }
}
