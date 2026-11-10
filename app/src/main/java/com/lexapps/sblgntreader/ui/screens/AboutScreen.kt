package com.lexapps.sblgntreader.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.core.net.toUri
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.lexapps.sblgntreader.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController, windowSizeClass: WindowSizeClass) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val unavailableMessage = stringResource(R.string.about_link_unavailable)
    val openLink: (String) -> Unit = { url ->
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            scope.launch { snackbar.showSnackbar(unavailableMessage) }
        }
    }
    val widthClass = windowSizeClass.widthSizeClass
    val horizontalPadding = when (widthClass) {
        WindowWidthSizeClass.Compact -> 16.dp
        WindowWidthSizeClass.Medium -> 32.dp
        WindowWidthSizeClass.Expanded -> 48.dp
        else -> 16.dp
    }
    val maxContentWidth = when (widthClass) {
        WindowWidthSizeClass.Compact -> null
        WindowWidthSizeClass.Medium -> 720.dp
        WindowWidthSizeClass.Expanded -> 900.dp
        else -> null
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.about_header)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val contentModifier = Modifier
                .align(Alignment.TopCenter)
                .let { base -> if (maxContentWidth != null) base.widthIn(max = maxContentWidth) else base }
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
            LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
            item {
                ElevatedCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Image(
                            painter = painterResource(R.drawable.splash),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.about_company_name),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(R.string.about_app_name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                SectionTitle(stringResource(R.string.about_section_data_sources))
            }
            item {
                ElevatedCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .fillMaxWidth()
                ) {
                    Column(Modifier.padding(horizontal = 4.dp)) {
                        LinkItem(
                            title = stringResource(R.string.about_link_sblgnt_text),
                            url = stringResource(R.string.credit_sblgnt_text_url),
                            onOpenLink = openLink
                        )
                        HorizontalDivider()
                        LinkItem(
                            title = stringResource(R.string.about_link_paragraph_divisions),
                            url = stringResource(R.string.credit_paragraph_divisions_url),
                            onOpenLink = openLink
                        )
                        HorizontalDivider()
                        LinkItem(
                            title = stringResource(R.string.about_link_morph_data),
                            url = stringResource(R.string.credit_morph_data_url),
                            onOpenLink = openLink
                        )
                        HorizontalDivider()
                        LinkItem(
                            title = stringResource(R.string.about_link_leb_reverse_interlinear),
                            url = stringResource(R.string.credit_leb_reverse_interlinear_url),
                            onOpenLink = openLink
                        )
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.about_section_contact)) }
            item {
                ElevatedCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .fillMaxWidth()
                 ) {
                    Column(Modifier.padding(horizontal = 4.dp)) {
                        EmailItem(onOpenLink = openLink)
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)
    )
}

@Composable
private fun LinkItem(title: String, url: String, onOpenLink: (String) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(url.removePrefix("https://").removePrefix("http://"), color = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenLink(url) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun EmailItem(onOpenLink: (String) -> Unit) {
    val email = stringResource(R.string.support_email)
    ListItem(
        headlineContent = { Text(stringResource(R.string.about_email_label)) },
        supportingContent = { Text(email, color = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenLink("mailto:$email") },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
