package com.example.group_assignment

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
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
import com.example.group_assignment.viewmodel.TaskViewModel
import com.example.group_assignment.viewmodel.TaskViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: TaskViewModel
    private lateinit var settingsRepo: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Setup Data Source & ViewModel
        val taskRepo = FileTaskRepository(this)
        val factory = TaskViewModelFactory(taskRepo)
        viewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]

        // 2. Setup Dark Mode (Wajib observe kat sini gak)
        settingsRepo = SettingsRepository(this)
        lifecycleScope.launch {
            settingsRepo.isDarkMode.collect { isDark ->
                if (isDark) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // 3. Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = TaskAdapter(emptyList(),
            onTaskChecked = { task -> viewModel.updateTask(task) },
            onTaskLongClick = { task -> showDeleteDialog(task) }
        )
        recyclerView.adapter = adapter

        // 4. Observe Data changes
        lifecycleScope.launch {
            viewModel.tasks.collect { tasks ->
                adapter.updateData(tasks)
            }
        }

        // 5. FAB Add Task Logic
        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showAddTaskDialog()
        }

        // 6. Settings Button Logic (Crash Fix Checked)
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // BUTTON: SORT (Sekarang berfungsi!)
        val btnSort = findViewById<Button>(R.id.btnSort)
        btnSort.setOnClickListener { view ->
            // Buat menu pop-up
            val popup = android.widget.PopupMenu(this, view)
            popup.menu.add("Sort by Date")
            popup.menu.add("Sort by Title")

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Sort by Date" -> {
                        viewModel.updateSortOrder(com.example.group_assignment.viewmodel.SortType.DATE)
                        btnSort.text = "Sort: Date ▼" // Tukar text butang biar user nampak
                    }
                    "Sort by Title" -> {
                        viewModel.updateSortOrder(com.example.group_assignment.viewmodel.SortType.TITLE)
                        btnSort.text = "Sort: Title ▼"
                    }
                }
                true
            }
            popup.show()
        }
    }

    private fun showAddTaskDialog() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        // Input Tajuk
        val inputTitle = EditText(this)
        inputTitle.hint = "Task Title (e.g. Buy Groceries)"
        layout.addView(inputTitle)

        // Input Tarikh (KITA LOCK)
        val inputDate = EditText(this)
        inputDate.hint = "Select Due Date"
        inputDate.isFocusable = false
        inputDate.isClickable = true
        layout.addView(inputDate)

        val calendar = java.util.Calendar.getInstance()

        inputDate.setOnClickListener {
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

            val datePicker = android.app.DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                // Format standard YYYY-MM-DD
                val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                inputDate.setText(formattedDate)
            }, year, month, day)

            // INI KOD PENYELAMAT MASA SILAM:
            // Set tarikh paling awal ialah "Sekarang" tolak 1 saat.
            // Jadi user cuma boleh pilih HARI INI dan MASA DEPAN sahaja.
            datePicker.datePicker.minDate = System.currentTimeMillis() - 1000

            datePicker.show()
        }

        AlertDialog.Builder(this)
            .setTitle("Add New Task")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val title = inputTitle.text.toString().trim()
                val date = inputDate.text.toString().trim()

                if (title.isNotEmpty() && date.isNotEmpty()) {
                    viewModel.addTask(title, date)
                } else {
                    Toast.makeText(this, "Please fill in both fields!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task?")
            .setMessage("Are you sure you want to delete '${task.title}'?")
            .setPositiveButton("Yes") { _, _ -> viewModel.deleteTask(task) }
            .setNegativeButton("No", null)
            .show()
    }
}