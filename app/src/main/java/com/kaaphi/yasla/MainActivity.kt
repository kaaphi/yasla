package com.kaaphi.yasla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaaphi.yasla.model.ListItem
import com.kaaphi.yasla.model.ShoppingListState
import com.kaaphi.yasla.ui.theme.YaslaTheme
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

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
        Column {
            ShoppingList(modifier = Modifier.weight(1f))
            BottomBar()
        }
    }
}

@Composable
fun ListItemRow(item: ListItem, modifier: Modifier = Modifier, reorderModifier: Modifier = Modifier, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Checkbox(checked = item.isChecked, onCheckedChange = onCheckedChange)
        val decoration = if (item.isChecked) {
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
    modifier: Modifier = Modifier,
    viewModel: ShoppingListState = viewModel(),
) {
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        viewModel.dragItem(from.index, to.index)
    }, onDragEnd = { startIndex, endIndex ->
        viewModel.endDragItem(startIndex, endIndex)
    })

    LazyColumn(
        state = state.listState,
        modifier = modifier
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
                        .detectReorder(state),
                onCheckedChange = { isChecked ->
                    viewModel.updateItem(item.position, item.copy(isChecked = isChecked))
                })
            }
        }
    }
}

@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    viewModel: ShoppingListState = viewModel()
) {
    BottomAppBar(
        actions = {
            Button(onClick = { viewModel.deleteCheckedItems() }) {
                Text("Clear Checked Items")
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /*TODO*/ }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    )
    
}


@Preview(showBackground = true)
@Composable
fun ListItemPreview() {
    YaslaTheme {

    }
}