package com.example.gymbuddy

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.gymbuddy.databinding.ItemPhotoPreviewBinding

class PhotoPreviewAdapter(
    private val onRemoveClick: (Uri) -> Unit
) : RecyclerView.Adapter<PhotoPreviewAdapter.PhotoViewHolder>() {

    private val uris = mutableListOf<Uri>()

    fun setUris(newUris: List<Uri>) {
        uris.clear()
        uris.addAll(newUris)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        return PhotoViewHolder(
            ItemPhotoPreviewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(uris[position])
    }

    override fun getItemCount() = uris.size

    inner class PhotoViewHolder(private val binding: ItemPhotoPreviewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: Uri) {
            binding.imagePreview.load(uri)
            binding.buttonRemovePhoto.setOnClickListener { onRemoveClick(uri) }
        }
    }
}
