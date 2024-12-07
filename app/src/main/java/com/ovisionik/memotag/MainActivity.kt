package com.ovisionik.memotag

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ovisionik.memotag.db.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), SideMenuFragment.OnMenuItemSelectedListener {

    //FILTERED ITEMS
    private lateinit var db: DatabaseHelper

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var touchBlocker: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize components
        drawerLayout = findViewById(R.id.drawer_layout)
        touchBlocker = findViewById(R.id.touch_blocker)

//        supportFragmentManager.beginTransaction()
//            .replace(R.id.content_frame, LoadingScreenFragment.newInstance("Loading item list…")) // Replaces the entire screen content
//            .commit()

        supportFragmentManager.beginTransaction()
            .replace(R.id.side_menu_container, SideMenuFragment()) // Replaces the entire screen content
            .commit()


        lifecycleScope.launch {
            withContext(Dispatchers.IO) {

                // Initialize the database
                db = DatabaseHelper.getInstance(this@MainActivity)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_frame, ListViewFragment())
                    .addToBackStack("listview_fragment")
                    .commit()
            }
        }
        manageSideMenu()
    }

    /**
     * Sets up the side menu and handles freezing the content_frame while the menu is open.
     */
    private fun manageSideMenu() {

        // Set up DrawerLayout listener
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                touchBlocker.visibility = View.VISIBLE // Show the blocker when sliding starts
            }

            override fun onDrawerOpened(drawerView: View) {
                // Make the touch blocker visible to disable interactions
                touchBlocker.visibility = View.VISIBLE
            }

            override fun onDrawerClosed(drawerView: View) {
                // Hide the touch blocker when the drawer is closed
                touchBlocker.visibility = View.GONE
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })

        // Prevent touch events from passing through the touch blocker
        touchBlocker.setOnTouchListener { _, _ -> true }
    }

    override fun onMainContentChangeRequest(fragment: Fragment) {

        drawerLayout.closeDrawer(GravityCompat.START)

        if(!isFragmentAlreadyDisplayed(R.id.content_frame, fragment)){

            if(fragment is ListViewFragment){
                // Load database asynchronously
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        // Load the database in a background thread
                        db = DatabaseHelper.getInstance(this@MainActivity)
                        val frag = ListViewFragment()
                        supportFragmentManager.beginTransaction()
                            .setCustomAnimations(
                                R.anim.fade_and_slide_in,  // Animation for fragment entering
                                R.anim.fade_and_slide_out, // Animation for fragment exiting
                                R.anim.fade_and_slide_in,  // Animation for fragment entering (when popping back)
                                R.anim.fade_and_slide_out   // Animation for fragment exiting (when popping back)
                            )
                            .replace(R.id.content_frame, frag)
                            .addToBackStack(null) // Add to back stack for navigation
                            .commit()
                    }
                }
                return
            }

            // Replace the content in content_frame with the selected fragment
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.fade_and_slide_in,  // Animation for fragment entering
                    R.anim.fade_and_slide_out, // Animation for fragment exiting
                    R.anim.fade_and_slide_in,  // Animation for fragment entering (when popping back)
                    R.anim.fade_and_slide_out   // Animation for fragment exiting (when popping back)
                )
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null) // Add to back stack for navigation
                .commit()
        }
    }
    private fun isFragmentAlreadyDisplayed(containerId: Int, newFragment: Fragment): Boolean {
        val fragmentManager = supportFragmentManager
        val currentFragment = fragmentManager.findFragmentById(containerId)

        // Check if the container already has the desired fragment
        return currentFragment != null && currentFragment::class == newFragment::class
    }

    //TODO:
    private fun lazyLoadItemTags(){

        lifecycleScope.launch(Dispatchers.IO) {
        }
    }

//    //TODO: Remove/Adapt to load gradually
//    private fun updateRvAdapter() {
//
//        if(firstLoad)
//            return
//
//        //Update adapter
//        val dbTags = DatabaseHelper.getInstance(this).getAllTags().reversed()
//
//        //update the adapter only if the number of items are different
//        if (tagAdapter.filteredTags.hashCode() != dbTags.hashCode()) {
//
//            //Check size if it's the same only item needs to update
//            if (tagAdapter.filteredTags.size == dbTags.size) {
//
//                val filTags = tagAdapter.filteredTags
//                for (i in dbTags.indices) {
//                    if (dbTags[i].id == filTags[i].id && dbTags[i].hashCode() != filTags[i].hashCode()) {
//                        //item changed
//                        tagAdapter.filteredTags[i] = dbTags[i]
//                        recyclerViewTags.adapter?.notifyItemChanged(i)
//                    }
//                }
//            } else {
//                //We could check and update each individual item
//
//                //... or update everything in the list
//                onMainContentChangeRequest(ListViewFragment())
//            }
//        }
//    }
}

