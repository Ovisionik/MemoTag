package com.ovisionik.memotag.utils

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionManagerUtils {

    private var isRequestInProgress = false

    // Fixed Request Codes for Permissions
    private const val INTERNET_PERMISSION_CODE = 101
    private const val CAMERA_PERMISSION_CODE = 102
    private const val STORAGE_PERMISSION_CODE = 103

    fun isCameraPermissionGranted(activity: Activity) : Boolean {
        val permission = android.Manifest.permission.CAMERA

        return isPermissionGranted(activity, permission)
    }

    // Check if a specific permission is granted
    private fun isPermissionGranted(activity: Activity, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }

    // Request a specific permission
    private fun requestPermission(activity: Activity, permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }

    // Show a generic message if permission is denied
    private fun showPermissionNeededMessage(activity: Activity, permissionName: String) {
        Toast.makeText(activity, "$permissionName permission is required to continue.", Toast.LENGTH_LONG).show()
    }

    // Handle permission result
    private fun handlePermissionResult(grantResults: IntArray): Boolean {
        return grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    // Show a dialog informing the user they need to manually enable permission in the settings
    private fun showPermissionExplanationDialog(activity: Activity, permissionName: String) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Permission Required")
            .setMessage("$permissionName permission is required to proceed. Please enable it manually in the app settings.")
            .setCancelable(false)
            .setPositiveButton("Go to Settings") { _, _ ->
                // Open app settings when the user clicks "Go to Settings"
                openAppSettings(activity)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    // Function to open the app's settings page
    private fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivity(intent)
    }

    // Public function to ask for Camera permission with extra check for multiple denials
    fun askForCameraPermission(activity: Activity): Boolean {
        val permission = android.Manifest.permission.CAMERA

        if (isPermissionGranted(activity, permission)) {
            return true
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            // Permission denied once, show rationale and request permission
            Toast.makeText(activity, "Camera permission required to continue", Toast.LENGTH_SHORT).show()
            requestPermission(activity, permission, CAMERA_PERMISSION_CODE)
        } else {
            // Permission denied twice or 'Don't ask again' was checked, show an info message
            showPermissionExplanationDialog(activity, "Camera")
        }

        return false
    }

//    //Note:Should not be needed to save docs
//    // Public function to ask for Storage permission
//    fun askForStoragePermission(activity: Activity): Boolean {
//        val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
//
//        if (isPermissionGranted(activity, permission)) {
//            // If permission is already granted, return true
//            return true
//        }
//
//        // If permission is denied, but user has not checked "Don't ask again"
//        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
//            // Show rationale and request permission
//            Toast.makeText(activity, "Storage permission required to continue", Toast.LENGTH_SHORT).show()
//            requestPermission(activity, permission, STORAGE_PERMISSION_CODE)
//        } else {
//            // If user has denied permission and checked "Don't ask again", show explanation dialog
//            showPermissionExplanationDialog(activity, "Storage")
//        }
//
//        return false
//    }

    // Public function to ask for Internet permission
    fun askForInternetPermission(activity: Activity): Boolean {
        val permission = android.Manifest.permission.INTERNET

        if (isPermissionGranted(activity, permission)) {
            return true
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            // Permission denied once, show rationale and request permission
            Toast.makeText(activity, "Internet permission required to continue", Toast.LENGTH_SHORT).show()
            requestPermission(activity, permission, INTERNET_PERMISSION_CODE)
        } else {
            // Permission denied twice or 'Don't ask again' was checked, show an info message
            showPermissionExplanationDialog(activity, "Internet")
        }

        return false
    }

    // Handle results for all permissions
    fun handlePermissionsResult(
        activity: Activity,
        requestCode: Int,
        grantResults: IntArray,
        permissionName: String = "This"
    ): Boolean {
        isRequestInProgress = false
        val granted = handlePermissionResult(grantResults)
        if (!granted) {
            showPermissionNeededMessage(activity, permissionName)
        }
        return granted
    }
}
