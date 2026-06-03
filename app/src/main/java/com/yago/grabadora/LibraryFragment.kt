package com.yago.grabadora

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import kotlin.concurrent.thread

class LibraryFragment : Fragment() {

    private lateinit var list: RecyclerView
    private lateinit var empty: TextView
    private lateinit var miniPlayer: MaterialCardView
    private lateinit var mpTitle: TextView
    private lateinit var player: AudioPlayer
    private lateinit var adapter: AudioAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_library, c, false)

    override fun onViewCreated(view: View, s: Bundle?) {
        list = view.findViewById(R.id.list)
        empty = view.findViewById(R.id.empty)
        miniPlayer = view.findViewById(R.id.miniPlayer)
        mpTitle = view.findViewById(R.id.mpTitle)
        player = AudioPlayer(
            requireContext(),
            view.findViewById(R.id.mpPlayPause),
            view.findViewById(R.id.mpSeek),
            view.findViewById(R.id.mpCur),
            view.findViewById(R.id.mpDur)
        )
        adapter = AudioAdapter(emptyList(), { open(it) }, { del(it) })
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        refresh()
    }

    fun refresh() {
        if (!isAdded || !::adapter.isInitialized) return
        val ctx = requireContext().applicationContext
        thread {
            val items = AudioStore.list(ctx)
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                adapter.submit(items)
                empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun open(d: AudioItem) {
        miniPlayer.visibility = View.VISIBLE
        mpTitle.text = d.name
        player.load(d.uri)
    }

    private fun del(d: AudioItem) {
        AudioStore.delete(requireContext(), d)
        refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player.release()
    }
}
