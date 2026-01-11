package com.example.group_assignment

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.group_assignment.model.Task
import com.example.group_assignment.repository.FileTaskRepository
import com.example.group_assignment.repository.SettingsRepository
import com.example.group_assignment.ui.TaskAdapter
import com.example.group_assignment.viewmodel.SortType
import com.example.group_assignment.viewmodel.TaskViewModel
import com.example.group_assignment.viewmodel.TaskViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

/**
 * Enhanced Main Activity with Search and Statistics
 * Role B: Main Task List UI, Task Data Class, Internal Storage Repository
 *
 * Improvements:
 * - Added real-time search functionality
 * - Display task statistics (total, completed, pending)
 * - Enhanced add task dialog with category selection
 * - Empty state handling
 * - Better user feedback and organization
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var viewModel: TaskViewModel
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var taskRepo: FileTaskRepository
    private lateinit var adapter: TaskAdapter

    // UI Components
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: Button
    private lateinit var tvTotalCount: TextView
    private lateinit var tvCompletedCount: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerView: RecyclerView

    // Store all tasks for filtering
    private var allTasks: List<Task> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "MainActivity onCreate started")

        // Initialize UI components
        initializeViews()

        // Setup Data Source & ViewModel
        taskRepo = FileTaskRepository(this)
        val factory = TaskViewModelFactory(taskRepo)
        viewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]

        // Setup Dark Mode
        settingsRepo = SettingsRepository(this)
        lifecycleScope.launch {
            settingsRepo.isDarkMode.collect { isDark ->
                if (isDark) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Setup RecyclerView
        setupRecyclerView()

        // Setup Search Functionality
        setupSearchBar()

        // Observe Data Changes
        observeTaskChanges()

        // Setup Button Listeners
        setupButtonListeners()
    }

    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        etSearch = findViewById(R.id.etSearch)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        tvTotalCount = findViewById(R.id.tvTotalCount)
        tvCompletedCount = findViewById(R.id.tvCompletedCount)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        recyclerView = findViewById(R.id.recyclerView)
    }

    /**
     * Setup RecyclerView with adapter
     */
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TaskAdapter(
            emptyList(),
            onTaskChecked = { task ->
                Log.d(TAG, "Task checked: ${task.title}")
                viewModel.updateTask(task)
            },
            onTaskLongClick = { task -> showDeleteDialog(task) }
        )
        recyclerView.adapter = adapter
    }

    /**
     * Setup search bar with real-time filtering
     */
    private fun setupSearchBar() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                filterTasks(query)

                // Show/hide clear button
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            etSearch.text.clear()
        }
    }

    /**
     * Filter tasks based on search query
     */
    private fun filterTasks(query: String) {
        val filteredTasks = if (query.isEmpty()) {
            allTasks
        } else {
            taskRepo.searchTasks(query)
        }
        adapter.updateData(filteredTasks)
        updateEmptyState(filteredTasks.isEmpty())
    }

    /**
     * Observe task changes and update UI
     */
    private fun observeTaskChanges() {
        lifecycleScope.launch {
            viewModel.tasks.collect { tasks ->
                Log.d(TAG, "Tasks updated: ${tasks.size} tasks")

                allTasks = tasks

                // If there's an active search, filter the results
                val currentQuery = etSearch.text.toString()
                if (currentQuery.isNotEmpty()) {
                    filterTasks(currentQuery)
                } else {
                    adapter.updateData(tasks)
                }

                // Update statistics
                updateStatistics()

                // Update empty state
                updateEmptyState(tasks.isEmpty())
            }
        }
    }

    /**
     * Update task statistics display
     */
    private fun updateStatistics() {
        tvTotalCount.text = taskRepo.getTotalTaskCount().toString()
        tvCompletedCount.text = taskRepo.getCompletedTaskCount().toString()
        tvPendingCount.text = taskRepo.getPendingTaskCount().toString()
    }

    /**
     * Show/hide empty state message
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    /**
     * Setup all button click listeners
     */
    private fun setupButtonListeners() {
        // FAB Add Task
        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showAddTaskDialog()
        }

        // Settings Button
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Sort Button
        val btnSort = findViewById<Button>(R.id.btnSort)
        btnSort.setOnClickListener { view ->
            showSortMenu(view, btnSort)
        }
    }

    /**
     * Show sort menu popup
     */
    private fun showSortMenu(view: View, btnSort: Button) {
        val popup = PopupMenu(this, view)
        popup.menu.add("Sort by Date")
        popup.menu.add("Sort by Title")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title) {
                "Sort by Date" -> {
                    viewModel.updateSortOrder(SortType.DATE)
                    btnSort.text = "Sort: Date ▼"
                }
                "Sort by Title" -> {
                    viewModel.updateSortOrder(SortType.TITLE)
                    btnSort.text = "Sort: Title ▼"
                }
            }
            true
        }
        popup.show()
    }

    /**
     * Show enhanced add task dialog with category
     */
    private fun showAddTaskDialog() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        // Task Title Input
        val inputTitle = EditText(this)
        inputTitle.hint = "Task Title (e.g., Buy Groceries)"
        layout.addView(inputTitle)

        // Due Date Input
        val inputDate = EditText(this)
        inputDate.hint = "Select Due Date"
        inputDate.isFocusable = false
        inputDate.isClickable = true
        layout.addView(inputDate)

        // Category Input
        val inputCategory = EditText(this)
        inputCategory.hint = "Category (e.g., Work, Personal, Shopping)"
        inputCategory.setText("General")
        layout.addView(inputCategory)

        // Date Picker Setup
        val calendar = java.util.Calendar.getInstance()
        inputDate.setOnClickListener {
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

            val datePicker = android.app.DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                inputDate.setText(formattedDate)
            }, year, month, day)

            // Only allow future dates (or today)
            datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
            datePicker.show()
        }

        // Show Dialog
        AlertDialog.Builder(this)
            .setTitle("Add New Task")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val title = inputTitle.text.toString().trim()
                val date = inputDate.text.toString().trim()
                val category = inputCategory.text.toString().trim().ifEmpty { "General" }

                Log.d(TAG, "Adding task: title=$title, date=$date, category=$category")

                if (title.isNotEmpty() && date.isNotEmpty()) {
                    // Create task with category
                    viewModel.addTask(title, date, category)
                    Toast.makeText(this, "Task added successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please fill in title and date!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show delete confirmation dialog
     */
    private fun showDeleteDialog(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task?")
            .setMessage("Are you sure you want to delete '${task.title}'?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.deleteTask(task)
                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }
}