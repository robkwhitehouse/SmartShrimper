package com.example.smartshrimper.ui.main

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.smartshrimper.R
import com.google.android.material.snackbar.Snackbar

/**
 * A placeholder fragment containing a simple view.
 */
class RevcounterFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_main, container, false)
 //       pageViewModel.text.observe(viewLifecycleOwner, Observer<String> {
 //           textView.text = it
 //       })
        val revCounter: Gauge = root.findViewById(R.id.revCounter)
        val thread = HandlerThread("GaugeDemoThread")
        thread.start()
        val handler: Handler = Handler(thread.getLooper())
        handler.postDelayed({ revCounter.moveToValue(300f) }, 2800)


        return root
    }


    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): RevcounterFragment {
            return RevcounterFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}