package io.numbersprotocol.starlingcapture.feature.proof

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import coil.load
import io.numbersprotocol.starlingcapture.R
import io.numbersprotocol.starlingcapture.data.proof.Proof
import io.numbersprotocol.starlingcapture.data.publisher_response.PublisherResponse
import io.numbersprotocol.starlingcapture.data.publisher_response.PublisherResponseRepository
import io.numbersprotocol.starlingcapture.data.serialization.SaveResponseImageWorker
import io.numbersprotocol.starlingcapture.databinding.ItemPublisherResponseImageBinding
import io.numbersprotocol.starlingcapture.databinding.ItemPublisherResponseUrlBinding
import io.numbersprotocol.starlingcapture.databinding.ItemPublisherResponsesBinding
import io.numbersprotocol.starlingcapture.util.copyToClipboard
import io.numbersprotocol.starlingcapture.util.openLinkInBrowser
import io.numbersprotocol.starlingcapture.util.snack

class PublisherAdapter(
    private val fragment: ProofFragment,
    private val viewLifecycleOwner: LifecycleOwner,
    private val publisherResponseRepository: PublisherResponseRepository,
    private val proof: Proof
) : ListAdapter<String, PublisherAdapter.ViewHolder>(diffStringCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemPublisherResponsesBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class ViewHolder(
        private val binding: ItemPublisherResponsesBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val adapter = PublisherResponseAdapter()

        fun bind(item: String) {
            binding.publisher = item
            binding.publisherResponseRecyclerView.adapter = adapter
            publisherResponseRepository
                .getByProofAndPublisherWithLiveData(proof, item)
                .observe(viewLifecycleOwner) { adapter.submitList(it) }
            binding.executePendingBindings()
        }
    }

    inner class PublisherResponseAdapter :
        ListAdapter<PublisherResponse, PublisherResponseAdapter.ViewHolder>(
            diffPublisherResponseCallback
        ) {

        override fun getItemViewType(position: Int) = getItem(position).type.ordinal

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                PublisherResponse.Type.Url.ordinal -> UrlViewHolder(
                    ItemPublisherResponseUrlBinding.inflate(layoutInflater, parent, false)
                )
                PublisherResponse.Type.Image.ordinal -> ImageViewHolder(
                    ItemPublisherResponseImageBinding.inflate(layoutInflater, parent, false)
                )
                else -> throw IllegalStateException("Unknown view type: ${viewType}.")
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(getItem(position))

        abstract inner class ViewHolder(
            binding: ViewBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            abstract fun bind(item: PublisherResponse)
        }

        inner class UrlViewHolder(
            private val binding: ItemPublisherResponseUrlBinding
        ) : ViewHolder(binding) {

            override fun bind(item: PublisherResponse) {
                binding.response = item
                binding.copyButton.setOnClickListener {
                    it.context.copyToClipboard(item.content)
                    fragment.snack(R.string.message_copied_to_clipboard)
                }
                binding.openInBrowserButton.setOnClickListener { it.context.openLinkInBrowser(item.content) }
            }
        }

        inner class ImageViewHolder(
            private val binding: ItemPublisherResponseImageBinding
        ) : ViewHolder(binding) {

            override fun bind(item: PublisherResponse) {
                binding.response = item
                binding.responseImageView.load(item.content)
                binding.saveAsButton.setOnClickListener {
                    fragment.pickDirToSaveResponseImageAsCallback = {
                        SaveResponseImageWorker.saveImageAs(
                            binding.root.context,
                            item.content.toUri(),
                            it
                        )
                    }
                    fragment.dispatchPickDirIntent(ProofFragment.REQUEST_PICK_DIR_TO_SAVE_RESPONSE_IMAGE_AS)
                }
            }
        }
    }

    companion object {
        private val diffStringCallback = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
            override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        }
        private val diffPublisherResponseCallback =
            object : DiffUtil.ItemCallback<PublisherResponse>() {
                override fun areItemsTheSame(
                    oldItem: PublisherResponse,
                    newItem: PublisherResponse
                ) = oldItem.proofHash == newItem.proofHash
                        && oldItem.publisher == newItem.publisher
                        && oldItem.name == newItem.name

                override fun areContentsTheSame(
                    oldItem: PublisherResponse,
                    newItem: PublisherResponse
                ) = oldItem == newItem
            }
    }
}