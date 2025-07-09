package com.eventanimation.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.eventanimation.R

class FlashDisplayDebugFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("FlashDisplayDebug", "onCreateView called")
        
        val view = TextView(requireContext()).apply {
            text = "Flash Display Debug Mode\n\nThis is a simplified version to test navigation.\n\nThe full flash functionality will be enabled after debugging."
            textSize = 16f
            setPadding(32, 32, 32, 32)
        }
        
        view.setOnClickListener {
            Toast.makeText(context, "Debug mode - going back", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
        
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("FlashDisplayDebug", "onViewCreated called")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("FlashDisplayDebug", "onResume called")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d("FlashDisplayDebug", "onPause called")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("FlashDisplayDebug", "onDestroyView called")
    }
}