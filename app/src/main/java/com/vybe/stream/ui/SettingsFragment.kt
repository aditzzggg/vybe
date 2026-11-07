package com.vybe.stream.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.vybe.stream.BuildConfig
import com.vybe.stream.R
import com.vybe.stream.data.PipedApi

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val regions = listOf("ID", "US", "GB", "JP", "DE", "IN")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = requireContext().getSharedPreferences("vybe_prefs", Context.MODE_PRIVATE)

        view.findViewById<TextView>(R.id.version_tv).text =
            "Vybe " + BuildConfig.VERSION_NAME + " • clean-room client, powered by community instances"

        val audioSwitch = view.findViewById<MaterialSwitch>(R.id.audio_switch)
        audioSwitch.isChecked = prefs.getBoolean("audio_only", false)
        audioSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("audio_only", checked).apply()
        }

        val regionChips = view.findViewById<ChipGroup>(R.id.region_chips)
        regions.forEach { region ->
            val chip = Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle)
            chip.text = region
            chip.isCheckable = true
            chip.chipBackgroundColor = resources.getColorStateList(R.color.chip_bg, null)
            chip.setTextColor(resources.getColor(R.color.text, null))
            chip.isChecked = region == (prefs.getString("region", "ID") ?: "ID")
            chip.setOnCheckedChangeListener { _, checked ->
                if (checked) prefs.edit().putString("region", region).apply()
            }
            regionChips.addView(chip)
        }

        val group = view.findViewById<RadioGroup>(R.id.instance_group)
        val saved = prefs.getString("instance", PipedApi.currentInstance)
        PipedApi.instances.forEach { instance ->
            val rb = android.widget.RadioButton(requireContext())
            rb.text = instance.removePrefix("https://")
            rb.setTextColor(resources.getColor(R.color.text, null))
            rb.buttonTintList = resources.getColorStateList(R.color.bottom_nav_item, null)
            rb.isChecked = instance == saved
            rb.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    prefs.edit().putString("instance", instance).apply()
                    PipedApi.currentInstance = instance
                }
            }
            group.addView(rb)
        }
    }
}
