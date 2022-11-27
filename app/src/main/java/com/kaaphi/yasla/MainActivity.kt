package com.kaaphi.yasla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaaphi.yasla.data.DataSource
import com.kaaphi.yasla.model.ListItem
import com.kaaphi.yasla.model.ShoppingListState
import com.kaaphi.yasla.ui.theme.YaslaTheme
import org.burnoutcrew.reorderable.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YaslaTheme {
                ListApp()
            }
        }
    }
}

@Composable
fun ListApp() {
    YaslaTheme {
        ShoppingList()
//        VerticalReorderList()
    }
}

@Composable
fun ListItemRow(item: ListItem, modifier: Modifier = Modifier, reorderModifier: Modifier = Modifier) {
    val isChecked = remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Checkbox(checked = isChecked.value, onCheckedChange = { isChecked.value = it })
        val decoration = if (isChecked.value) {
            TextDecoration.LineThrough
        } else {
            TextDecoration.None
        }
        Text(item.name, textDecoration = decoration)
        Spacer(modifier = Modifier.weight(1f))
        FilledIconButton(onClick = { /*TODO*/ }) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        FilledIconButton(onClick = {}, modifier = reorderModifier) {
            Icon(
                painter = painterResource(R.drawable.ic_round_reorder_24),
                contentDescription = "Reorder"
            )
        }
    }
}

@Composable
fun ShoppingList(
    viewModel: ShoppingListState = viewModel()
) {
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        viewModel.moveItem(from.index, to.index)
    })

    LazyColumn(
        state = state.listState,
        modifier = Modifier
            .reorderable(state)
    ) {
        items(viewModel.list, { it }) { item ->
            ReorderableItem(state, key = item) { isDragging ->
                val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                ListItemRow(item = item,
                    modifier = Modifier
                        .shadow(elevation.value)
                        .background(MaterialTheme.colorScheme.surface),
                    reorderModifier = Modifier
                        .detectReorder(state))
            }
        }
    }
}

@Composable
fun VerticalReorderList() {
    val data = remember { mutableStateOf(List(100) { "Item $it" }) }
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        data.value = data.value.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    })
    LazyColumn(
        state = state.listState,
        modifier = Modifier
            .reorderable(state)
            .detectReorderAfterLongPress(state)
    ) {
        items(data.value, { it }) { item ->
            ReorderableItem(state, key = item) { isDragging ->
                val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                Column(
                    modifier = Modifier
                        .shadow(elevation.value)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Text(item)
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ListItemPreview() {
    YaslaTheme {

    }
}