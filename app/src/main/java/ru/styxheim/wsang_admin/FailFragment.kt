package ru.styxheim.wsang_admin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import ru.styxheim.wsang_admin.databinding.FragmentFailBinding


class FailFragment : Fragment() {
  private var binding: FragmentFailBinding? = null
  private var message: String? = null
  private var from: Int? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.let {
      message = it.getString("Message")
      from = it.getInt("From")
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    binding = FragmentFailBinding.inflate(inflater, container, false)
    return binding?.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding!!.errorMessage.text = message
    binding!!.retryButton.setOnClickListener { _ -> findNavController().navigate(from!!) }
  }
}