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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kaaphi.yasla.model.ListItem
import com.kaaphi.yasla.model.ShoppingListState
import com.kaaphi.yasla.ui.theme.YaslaTheme
import kotlinx.coroutines.delay
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

enum class ShoppingListScreen() {
    List,
    AddItem
}

@Composable
fun ListApp(
    viewModel: ShoppingListState = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = ShoppingListScreen.List.name
    ) {
        composable(route = ShoppingListScreen.List.name) {
            val editItem: ListItem? by viewModel.editItemState.collectAsState()
            editItem?.let { item ->
                EditQuantity(
                    item = item,
                    onChangeQuantity = { quantity ->
                        viewModel.updateItem(item) {
                            copy(quantity = quantity)
                        }
                        viewModel.editItem(null)
                    },
                    onDismiss = {
                        viewModel.editItem(null)
                    })
            }

            Column {
                ShoppingList(modifier = Modifier.weight(1f),
                    list = viewModel.list,
                    onMoveItem = viewModel::moveItem,
                    onItemCheckChange = { isChecked ->
                        viewModel.updateItem(this) {
                            copy(isChecked = isChecked)
                        }
                    },
                    onEditItemClicked = viewModel::editItem
                )
                BottomBar(
                    onClearCheckedItemsClicked = {
                        viewModel.deleteCheckedItems()
                    },
                    onAddButtonClicked = {
                        navController.navigate(ShoppingListScreen.AddItem.name)
                    })
            }
        }

        composable(route = ShoppingListScreen.AddItem.name) {
            AddItem(onAddItemClicked = { itemName ->
                viewModel.addItem(itemName)
                navController.navigate(ShoppingListScreen.List.name)
            })
        }
    }
}

@Composable
fun ListItemRow(item: ListItem, modifier: Modifier = Modifier, reorderModifier: Modifier = Modifier,
                onCheckedChange: (Boolean) -> Unit, onEditItemClicked: (ListItem) -> Unit) {
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
        Text("${item.quantity?.let{"$it "} ?: ""}${item.name}", textDecoration = decoration)
        Spacer(modifier = Modifier.weight(1f))
        FilledIconButton(onClick = {
            onEditItemClicked.invoke(item)
        }) {
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
    list: List<ListItem>,
    onMoveItem: (from: Int, to: Int) -> Unit,
    onItemCheckChange: ListItem.(Boolean)->Unit,
    onEditItemClicked: (ListItem)->Unit
) {
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        onMoveItem(from.index, to.index)
    })

    LazyColumn(
        state = state.listState,
        modifier = modifier
            .reorderable(state)
    ) {
        items(list, { it }) { item ->
            ReorderableItem(state, key = item) { isDragging ->
                val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                ListItemRow(item = item,
                    modifier = Modifier
                        .shadow(elevation.value)
                        .background(MaterialTheme.colorScheme.surface),
                    reorderModifier = Modifier
                        .detectReorder(state),
                    onCheckedChange = { isChecked ->
                        item.onItemCheckChange(isChecked)
                    },
                    onEditItemClicked = onEditItemClicked
                    )
            }
        }
    }
}

@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    onClearCheckedItemsClicked: () -> Unit,
    onAddButtonClicked: () -> Unit,
) {
    BottomAppBar(
        actions = {
            Button(onClick = onClearCheckedItemsClicked) {
                Text("Clear Checked Items")
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddButtonClicked) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItem(
    modifier: Modifier = Modifier,
    onAddItemClicked: (String) -> Unit
) {
    val text = remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    Row {
        TextField(
            value = text.value,
            onValueChange = { text.value = it },
            modifier = Modifier.focusRequester(focusRequester)
        )
        Button(onClick = {
            onAddItemClicked(text.value)
        }) {
            Text("Add Item")
        }
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditQuantity(
    modifier: Modifier = Modifier,
    item: ListItem,
    onChangeQuantity: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialValue = item.quantity ?: ""
    val text = remember { mutableStateOf(TextFieldValue(
        text = initialValue,
        selection = TextRange(0, initialValue.length)
    )) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Enter Quantity")
        },
        text = {
            TextField(
                value = text.value,
                onValueChange = { text.value = it },
                modifier = Modifier.focusRequester(focusRequester),

            )
            LaunchedEffect(focusRequester) {
                delay(10) //for bug https://issuetracker.google.com/issues/204502668
                focusRequester.requestFocus()
            }
        },
        confirmButton = {
            Button(onClick = {
                onChangeQuantity.invoke(text.value.text.ifBlank { null })
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}



@Preview(showBackground = true)
@Composable
fun ListItemPreview() {
    YaslaTheme {
        AddItem() {}
    }
}