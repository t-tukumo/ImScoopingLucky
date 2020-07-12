package ttukumo.scoop

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import ttukumo.scoop.databinding.FragmentPageBinding


const val FRAGMENT_PARAM_PAGE = "page"

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class PageFragment : Fragment() {

    private var _binding: FragmentPageBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // View Binding
        _binding = FragmentPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val vmFactory =
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        val position = requireArguments().getInt(FRAGMENT_PARAM_PAGE)
        val vm = ViewModelProvider(requireParentFragment(), vmFactory)
            .get(position.toString(), PageViewModel::class.java)

        vm.fetchNovelItem(position)

        vm.novelModel.honbun.observe(
            viewLifecycleOwner,
            Observer { binding.textviewFirst.text = it })
        vm.novelModel.url.observe(viewLifecycleOwner, Observer { binding.pageLink.text = it })
        vm.novelModel.searchHint.observe(
            viewLifecycleOwner,
            Observer { binding.searchHint.text = it })
        vm.novelModel.writer.observe(
            viewLifecycleOwner,
            Observer { binding.writerName.text = it }
        )

        if(position == 0){
            binding.dividerLine1.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(page: Int): PageFragment {
            val args = Bundle()
            args.putInt(FRAGMENT_PARAM_PAGE, page)
            val fragment = PageFragment()
            fragment.arguments = args
            return fragment
        }
    }
}

class PageViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var novelModel: NovelModel

    fun fetchNovelItem(page: Int) {
        if (!::novelModel.isInitialized) {
            novelModel =
                if (page == 0) {
                    NovelModels.tutorialPage(getApplication())
                } else {
                    NovelModels.newNovel(getApplication())
                }
        }
    }

}
