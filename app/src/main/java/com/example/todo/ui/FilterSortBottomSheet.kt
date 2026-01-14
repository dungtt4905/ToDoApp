package com.example.todo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.example.todo.R
import com.example.todo.databinding.BottomSheetFilterSortBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterSortBottomSheet(
    private val currentFilter: String,
    private val currentSortField: String,
    private val isSortAscending: Boolean,
    private val onApply: (filter: String, sortField: String, isAscending: Boolean) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFilterSortBinding? = null
    private val binding get() = _binding!!
    
    private var selectedFilter: String = currentFilter
    private var selectedSortField: String = currentSortField
    private var selectedSortAscending: Boolean = isSortAscending

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFilterSortBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupFilterDropdown()
        setupSortDropdown()
        setupSortDirection()
        setupApplyButton()
    }

    private fun setupFilterDropdown() {
        val filters = arrayOf(
            getString(R.string.filter_all),
            getString(R.string.filter_active),
            getString(R.string.filter_done)
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, filters)
        binding.actFilter.setAdapter(adapter)
        binding.actFilter.setText(selectedFilter, false)
        binding.actFilter.setOnItemClickListener { _, _, position, _ ->
            selectedFilter = filters[position]
            applyChangesImmediately()
        }
    }

    private fun setupSortDropdown() {
        val sortFields = arrayOf(
            getString(R.string.sort_field_created),
            getString(R.string.sort_field_due),
            getString(R.string.sort_field_priority)
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sortFields)
        binding.actSortField.setAdapter(adapter)
        binding.actSortField.setText(selectedSortField, false)
        binding.actSortField.setOnItemClickListener { _, _, position, _ ->
            selectedSortField = sortFields[position]
            applyChangesImmediately()
        }
    }

    private fun setupSortDirection() {
        updateSortDirectionButton()
        binding.btnSortDirectionSheet.setOnClickListener {
            selectedSortAscending = !selectedSortAscending
            updateSortDirectionButton()
            applyChangesImmediately()
        }
    }

    private fun updateSortDirectionButton() {
        val iconRes = if (selectedSortAscending) {
            android.R.drawable.arrow_up_float
        } else {
            android.R.drawable.arrow_down_float
        }
        val text = if (selectedSortAscending) "Ascending" else "Descending"
        
        binding.btnSortDirectionSheet.icon = requireContext().getDrawable(iconRes)
        binding.btnSortDirectionSheet.text = text
    }

    private fun setupApplyButton() {
        // No apply button needed - changes apply immediately
    }

    private fun applyChangesImmediately() {
        onApply(selectedFilter, selectedSortField, selectedSortAscending)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
