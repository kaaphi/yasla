package com.kaaphi.yasla

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditAttributes
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kaaphi.yasla.data.StoreItem
import com.kaaphi.yasla.model.ShoppingListState
import com.kaaphi.yasla.model.ShoppingListStateFactory
import com.kaaphi.yasla.ui.ClickableLinks
import com.kaaphi.yasla.ui.TextInputDialogHost
import com.kaaphi.yasla.ui.TextInputDialogState
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
                if(!viewModel.isRebalancing.value) {
                    ListApp(viewModel)
                } else {
                    Text("Loading...")
                }
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
    val justAddedItem = remember {
        mutableStateOf<StoreItem?>(null)
    }
    val textInputDialogState = remember { TextInputDialogState() }

    val showEditItemDialog = { item: StoreItem, title: String, initialValue: String,
        updateBlock: StoreItem.(String?) -> StoreItem ->
        scope.launch {
            textInputDialogState.showDialog(
                title, initialValue, onConfirm = { textResult ->
                    scope.launch {
                        viewModel.updateItem(item) {
                            updateBlock(textResult)
                        }
                    }
                }
            )
        }
    }

    LaunchedEffect("snackbar") {
        scope.launch {
            viewModel.errors.collect {
                snackbarHostState.showSnackbar(it, withDismissAction = true)
            }
        }
    }

    if(justAddedItem.value != null) {
        LaunchedEffect("justAdded") {
            delay(1000)
            justAddedItem.value = null
        }
    }

    Box {
        NavHost(
            navController = navController,
            startDestination = ShoppingListScreen.List.name
        ) {
            composable(route = ShoppingListScreen.List.name) {
                TextInputDialogHost(state = textInputDialogState)

                Column {
                    ShoppingList(
                        modifier = Modifier.weight(1f),
                        list = viewModel.list,
                        justAdded = justAddedItem.value,
                        onMoveItem = { from, to, isDone ->
                            if(!isDone) {
                                viewModel.moveItemInView(from, to)
                            } else {
                                scope.launch { viewModel.updateRankAfterMove(to) }
                            }
                        },
                        onItemCheckChange = { isChecked ->
                            val item = this
                            scope.launch {
                                viewModel.updateItem(item) {
                                    copy(isChecked = isChecked)
                                }
                            }
                        },
                        onEditQuantityClicked = { item ->
                            showEditItemDialog(item, "Enter Quantity", item.quantity ?: "") {
                                copy(quantity = it)
                            }
                        },
                        onEditItemClicked = { item ->
                            showEditItemDialog(item, "Enter Name", item.name) {
                                copy(name = it ?: "")
                            }
                        },
                        onDeleteItemClicked = { item ->
                            scope.launch {
                                val (completeDelete, undoDelete) = viewModel.deleteItem(item)

                                when(snackbarHostState.showSnackbar(
                                    message = "Item ${item.name} permanently deleted.",
                                    actionLabel = "Undo",
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Indefinite
                                )) {
                                    SnackbarResult.ActionPerformed -> {
                                        //undo the delete
                                        undoDelete()
                                    }
                                    SnackbarResult.Dismissed -> {
                                        //complete the delete
                                        completeDelete()
                                    }
                                }
                            }
                        }
                    )
                    BottomBar(
                        onClearCheckedItemsClicked = {
                            scope.launch { viewModel.deleteCheckedItems() }
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
                        justAddedItem.value = viewModel.addItem(itemName.trim())
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
fun ListItemRow(item: StoreItem, isJustAdded: Boolean, modifier: Modifier = Modifier, reorderModifier: Modifier = Modifier,
                onCheckedChange: (Boolean) -> Unit, onEditQuantityClicked: (StoreItem) -> Unit,
                onEditItemClicked: (StoreItem) -> Unit, onDeleteItemClicked: (StoreItem) -> Unit) {

    val alpha: Float by animateFloatAsState(targetValue = if(isJustAdded) 0.8F else 0F,
        animationSpec = spring(1.5f, Spring.StiffnessVeryLow)
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .let {
            if(alpha != 0F) {
                val color = MaterialTheme.colorScheme.surfaceVariant
                it.drawWithContent {
                    drawRoundRect(
                        color,
                        alpha = alpha,
                        cornerRadius = CornerRadius(40f),
                    )
                    drawContent()
                }
            } else {
                it
            }
        }
    ) {
        ListItemCheckbox(item = item, onCheckedChange = onCheckedChange,
            onEditItemClicked = onEditItemClicked, onDeleteItemClicked = onDeleteItemClicked,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp))
        FilledIconButton(
            modifier = Modifier.testTag("EditItem"),
            onClick = {
                onEditQuantityClicked(item)
            }) {
            Icon(Icons.Default.EditAttributes, contentDescription = "Edit Quantity")
        }
        FilledIconButton(onClick = {}, modifier = reorderModifier.padding(end = 10.dp)) {
            Icon(
                Icons.Default.DragIndicator,
                contentDescription = "Reorder"
            )
        }
    }
}

@Composable
fun ListItemCheckbox(item: StoreItem, modifier: Modifier = Modifier,
                     onCheckedChange: (Boolean) -> Unit,
                     onEditItemClicked: (StoreItem) -> Unit,
                     onDeleteItemClicked: (StoreItem) -> Unit,
                     ) {
    val popupMenu = remember { mutableStateOf(false)}

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
             .pointerInput("clicking") {
                detectTapGestures(
                    onLongPress = {
                        popupMenu.value = true
                    },
                    onTap = {
                        onCheckedChange(!item.isChecked)
                    }
                )
            }
    ) {
        Checkbox(modifier = Modifier.testTag("CheckItem"), checked = item.isChecked, onCheckedChange = null)
        val decoration = if (item.isChecked) {
            TextDecoration.LineThrough
        } else {
            TextDecoration.None
        }
        Spacer(
            Modifier
                .size(10.dp)
                .testTag("CheckSpacer"))
        Text("${item.quantity?.let{"$it "} ?: ""}${item.name}", textDecoration = decoration)

        DropdownMenu(
            expanded = popupMenu.value,
            onDismissRequest = { popupMenu.value = false },
        ) {
            DropdownMenuItem(
                text = {
                    Row {
                        Icon(Icons.Default.Edit, "Edit")
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Edit Item")
                    }
                },
                onClick = {
                    onEditItemClicked(item)
                    popupMenu.value= false
                })
            DropdownMenuItem(
                text = {
                    Row {
                        Icon(Icons.Default.DeleteForever, "Delete")
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Delete Item")
                    }
                },
                onClick = {
                    onDeleteItemClicked(item)
                    popupMenu.value= false
                })
        }
    }
}

@Composable
fun ShoppingList(
    modifier: Modifier = Modifier,
    list: List<StoreItem>,
    justAdded: StoreItem? = null,
    onMoveItem: (from: Int, to: Int, isDone: Boolean) -> Unit,
    onItemCheckChange: StoreItem.(Boolean)->Unit,
    onEditQuantityClicked: (StoreItem)->Unit,
    onEditItemClicked: (StoreItem) -> Unit,
    onDeleteItemClicked: (StoreItem) -> Unit,
) {
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        onMoveItem(from.index, to.index, false)
    }, onDragEnd = { from, to ->
        onMoveItem(from, to, true)
    })

    val listHeightPx = remember { mutableStateOf(0) }

    if(justAdded != null) {
        LaunchedEffect("scroll to item") {
            //scroll just-added item to the center of the list
            state.listState.animateScrollToItem(list.indexOf(justAdded), -(listHeightPx.value/2))
        }
    }

    LazyColumn(
        state = state.listState,
        modifier = modifier
            .reorderable(state)
            .onGloballyPositioned {
                listHeightPx.value = it.size.height
            }
    ) {
        items(list, { it }) { item ->
            ReorderableItem(state, key = item) { isDragging ->
                val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                ListItemRow(item = item,
                    isJustAdded = justAdded == item,
                    modifier = Modifier
                        .shadow(elevation.value)
                        .background(MaterialTheme.colorScheme.surface),
                    reorderModifier = Modifier
                        .detectReorder(state),
                    onCheckedChange = { isChecked ->
                        item.onItemCheckChange(isChecked)
                    },
                    onEditQuantityClicked = onEditQuantityClicked,
                    onEditItemClicked = onEditItemClicked,
                    onDeleteItemClicked = onDeleteItemClicked,
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

    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
        TextField(
            value = text.value,
            onValueChange = { text.value = it },
            modifier = Modifier
                .focusRequester(focusRequester)
                .weight(1f),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Go,
            ),
            singleLine = true,
            keyboardActions = KeyboardActions {
                onAddItemClicked(text.value)
            },
            colors = TextFieldDefaults.textFieldColors(
                //make background match the background of the add button
                containerColor = MaterialTheme.colorScheme.background
            ),
        )
        Button(
            onClick = {
                onAddItemClicked(text.value)
            }) {
            Text("Add Item")
        }
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}

@Preview(showBackground = true)
@Composable
fun ListItemPreview() {
    YaslaTheme {
        AddItem(onAddItemClicked = {})
    }
}