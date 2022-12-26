package com.kaaphi.yasla

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.kaaphi.yasla.model.ShoppingListStateFactory
import com.kaaphi.yasla.ui.ClickableLinks
import com.kaaphi.yasla.ui.theme.YaslaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: ShoppingListState = viewModel(factory = ShoppingListStateFactory(
                LocalContext.current.applicationContext
                        as Application
            ))

            YaslaTheme {
                ListApp(viewModel)
            }
        }
    }
}

enum class ShoppingListScreen() {
    List,
    AddItem,
    About,
}

@Composable
fun ListApp(
    viewModel: ShoppingListState,
    navController: NavHostController = rememberNavController()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect("snackbar") {
        scope.launch {
            viewModel.errors.collect {
                snackbarHostState.showSnackbar(it, withDismissAction = true)
            }
        }
    }

    Box {
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
                    ShoppingList(
                        modifier = Modifier.weight(1f),
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
                        },
                        onAboutClicked = {
                            navController.navigate(ShoppingListScreen.About.name)
                        }
                    )
                }
            }

            composable(route = ShoppingListScreen.AddItem.name) {
                AddItem(onAddItemClicked = { itemName ->
                    scope.launch {
                        viewModel.addItem(itemName)
                    }
                    navController.navigate(ShoppingListScreen.List.name)
                })
            }

            composable(route = ShoppingListScreen.About.name) {
                About()
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun ListItemRow(item: ListItem, modifier: Modifier = Modifier, reorderModifier: Modifier = Modifier,
                onCheckedChange: (Boolean) -> Unit, onEditItemClicked: (ListItem) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        ListItemCheckbox(item = item, onCheckedChange = onCheckedChange, modifier = Modifier.weight(1f))

        FilledIconButton(
            modifier = Modifier.testTag("EditItem"),
            onClick = {
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
fun ListItemCheckbox(item: ListItem, modifier: Modifier = Modifier, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable {
                onCheckedChange(!item.isChecked)
            }
    ) {
        Checkbox(modifier = Modifier.testTag("CheckItem"), checked = item.isChecked, onCheckedChange = null)
        val decoration = if (item.isChecked) {
            TextDecoration.LineThrough
        } else {
            TextDecoration.None
        }
        Spacer(Modifier.size(10.dp).testTag("CheckSpacer"))
        Text("${item.quantity?.let{"$it "} ?: ""}${item.name}", textDecoration = decoration)
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
    onAboutClicked: () -> Unit,
) {
    val expanded = remember { mutableStateOf(false)}

    BottomAppBar(
        modifier = modifier,
        actions = {
            Box(
                Modifier
                    .wrapContentSize(Alignment.TopEnd)
            ) {
                IconButton(onClick = {
                    expanded.value = true
                }) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "More actions"
                    )
                }

                DropdownMenu(
                    expanded = expanded.value,
                    onDismissRequest = { expanded.value = false },
                ) {
                    DropdownMenuItem(
                        text = {Text("About")},
                        onClick = {
                            expanded.value = false
                            onAboutClicked.invoke()
                        })
                }
            }
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

@Composable
fun About(
    modifier: Modifier = Modifier
) {
    val links = ClickableLinks(
        linkStyle = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline
        ),
        uriHandler = LocalUriHandler.current
    ) { link ->
        link.appendLink("Notebook icon",
            "https://game-icons.net/1x1/delapouite/notebook.html")
        append(" by ")
        link.appendLink("Delapouite", "https://delapouite.com/")
        append(" licensed under ")
        link.appendLink("CC BY 3.0", "http://creativecommons.org/licenses/by/3.0/")
        append(" is used as the app icon.")
    }

    Column(modifier = modifier.padding(all = 20.dp)) {
        Text(text = "About", style = MaterialTheme.typography.headlineSmall)
        ClickableText(
            text = links.annotatedString,
            onClick = links::onClick,
            style = MaterialTheme.typography.bodyLarge)
    }
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
            modifier = Modifier.focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
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
                delay(100) //for bug https://issuetracker.google.com/issues/204502668
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
        About()
    }
}