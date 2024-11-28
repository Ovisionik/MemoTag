package com.ovisionik.memotag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class LoadingScreenFragment : Fragment() {

    private var message: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            message = it.getString(ARG_MESSAGE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_loading_screen, container, false)

        // Find your TextView and set the custom message
        val messageTextView: TextView = view.findViewById(R.id.loading_message)

        messageTextView.text = message ?: getString(R.string.loading_msg) // Default message if none provided

        // Inflate the layout for this fragment
        return view
    }

    companion object {
        private const val ARG_MESSAGE = "message"
        // Factory method to create a new instance with a custom message
        fun newInstance(message: String): LoadingScreenFragment {
            val fragment = LoadingScreenFragment()
            val args = Bundle()
            args.putString(ARG_MESSAGE, message)
            fragment.arguments = args
            return fragment
        }
    }
}