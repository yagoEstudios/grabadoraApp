package com.yago.grabadora

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val recordFragment = RecordFragment()
    private val libraryFragment = LibraryFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, recordFragment, "rec")
                .add(R.id.container, libraryFragment, "lib")
                .hide(libraryFragment)
                .commit()
        }

        findViewById<BottomNavigationView>(R.id.bottomNav).setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_record -> { showOnly(recordFragment); true }
                R.id.nav_library -> { libraryFragment.refresh(); showOnly(libraryFragment); true }
                else -> false
            }
        }
    }

    private fun showOnly(f: Fragment) {
        val other = if (f === recordFragment) libraryFragment else recordFragment
        supportFragmentManager.beginTransaction().hide(other).show(f).commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
