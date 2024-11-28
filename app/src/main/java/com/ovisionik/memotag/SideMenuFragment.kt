package com.ovisionik.memotag

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class SideMenuFragment : Fragment() {


    interface OnMenuItemSelectedListener {
        fun onMainContentChangeRequest(fragment: Fragment)
    }

    private var listener: OnMenuItemSelectedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Ensure the host activity implements the interface
        if (context is OnMenuItemSelectedListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnMenuItemSelectedListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    // Example function to handle menu item selection
    fun onMenuItemClick(fragment: Fragment) {
        listener?.onMainContentChangeRequest(fragment)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_side_menu, container, false)

        // Example: Handling a menu item click
        val menuItemButton = view.findViewById<Button>(R.id.menu_btn_home)
        menuItemButton.setOnClickListener {
            // Notify the MainActivity to switch to a new fragment
            //onMenuItemClick(ExampleFragment())
            listener?.onMainContentChangeRequest(ListViewFragment())
        }

        return view
    }
}