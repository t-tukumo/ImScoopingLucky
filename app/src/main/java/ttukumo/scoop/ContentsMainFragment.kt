package ttukumo.scoop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import ttukumo.scoop.databinding.FragmentContentsMainBinding

class ContentsMainFragment : Fragment() {
    private var _binding: FragmentContentsMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // View Binding
        _binding = FragmentContentsMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.pager.adapter = ContentsPagerAdapter(this)
    }
}

class ContentsPagerAdapter(fa: Fragment) :
    FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = Int.MAX_VALUE

    override fun getItemId(position: Int): Long = position.toLong()

    override fun createFragment(position: Int): Fragment {
        return PageFragment.newInstance(position)
    }

}
