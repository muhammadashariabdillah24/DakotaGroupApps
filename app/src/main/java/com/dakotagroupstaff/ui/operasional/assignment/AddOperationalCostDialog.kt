package com.dakotagroupstaff.ui.operasional.assignment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Toast
import com.dakotagroupstaff.R
import com.dakotagroupstaff.data.remote.response.OperationalCostItem
import com.dakotagroupstaff.databinding.DialogAddOperationalCostBinding

class AddOperationalCostDialog(
    context: Context,
    private val onSave: (itemId: String, nominal: String, keterangan: String?) -> Unit
) : Dialog(context) {
    
    private lateinit var binding: DialogAddOperationalCostBinding
    private var costItems: List<OperationalCostItem> = emptyList()
    private var selectedItemId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        binding = DialogAddOperationalCostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupListeners()
        window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
    }
    
    fun setCostItems(items: List<OperationalCostItem>) {
        costItems = items
        setupDropdown()
    }
    
    private fun setupDropdown() {
        val itemNames = costItems.map { "${it.itemId} - ${it.itemName}" }
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            itemNames
        )
        
        binding.actvItem.setAdapter(adapter)
        binding.actvItem.setOnItemClickListener { _, _, position, _ ->
            selectedItemId = costItems[position].itemId
        }
    }
    
    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                val itemId = selectedItemId ?: return@setOnClickListener
                val nominal = binding.etNominal.text.toString()
                val keterangan = binding.etKeterangan.text?.toString()
                
                onSave(itemId, nominal, keterangan)
                dismiss()
            }
        }
    }
    
    private fun validateInput(): Boolean {
        // Validate item selection
        if (selectedItemId == null) {
            Toast.makeText(context, "Pilih item biaya operasional", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Validate nominal
        val nominal = binding.etNominal.text.toString()
        if (nominal.isBlank()) {
            binding.tilNominal.error = "Nominal tidak boleh kosong"
            return false
        }
        
        val nominalValue = nominal.toDoubleOrNull()
        if (nominalValue == null || nominalValue <= 0) {
            binding.tilNominal.error = "Masukkan nominal yang valid"
            return false
        }
        
        binding.tilNominal.error = null
        return true
    }
}
