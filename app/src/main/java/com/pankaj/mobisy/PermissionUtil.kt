package com.pankaj.mobisy

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.ArrayList
import java.util.HashMap

class PermissionUtil(private val context: Context) {

    private var current_activity: Activity = context as Activity

    private var permissionResultCallback: PermissionResultCallback? = null

    private var permission_list = ArrayList<String>()
    private var listPermissionsNeeded = ArrayList<String>()
    private var dialog_content = ""
    private var req_code: Int = 0

    constructor(context: Context, permissionResultCallback: PermissionResultCallback) : this(context) {
        this.permissionResultCallback = permissionResultCallback
    }


    /**
     * Check the API Level & Permission
     *
     * @param permissions
     * @param dialog_content
     * @param request_code
     */

    fun check_permission(permissions: ArrayList<String>, dialog_content: String, request_code: Int) {
        this.permission_list = permissions
        this.dialog_content = dialog_content
        this.req_code = request_code

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkAndRequestPermissions(permissions, request_code)) {
                permissionResultCallback?.PermissionGranted(request_code)
                Log.i("all permissions", "granted")
                Log.i("proceed", "to callback")
            }
        } else {
            permissionResultCallback?.PermissionGranted(request_code)

            Log.i("all permissions", "granted")
            Log.i("proceed", "to callback")
        }

    }

    /**
     * Check and request the Permissions
     *
     * @param permissions
     * @param request_code
     * @return
     */

    private fun checkAndRequestPermissions(permissions: ArrayList<String>, request_code: Int): Boolean {

        if (permissions.size > 0) {
            listPermissionsNeeded = ArrayList()

            for (i in permissions.indices) {
                val hasPermission = ContextCompat.checkSelfPermission(current_activity, permissions[i])

                if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(permissions[i])
                }

            }

            if (listPermissionsNeeded.isNotEmpty()) {
                ActivityCompat.requestPermissions(current_activity, listPermissionsNeeded.toTypedArray(), request_code)
                return false
            }
        }

        return true
    }

    /**
     *
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> if (grantResults.isNotEmpty()) {
                val perms = HashMap<String, Int>()

                for (i in permissions.indices) {
                    perms[permissions[i]] = grantResults[i]
                }

                val pending_permissions = ArrayList<String>()

                for (i in listPermissionsNeeded.indices) {
                    if (perms[listPermissionsNeeded[i]] != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(current_activity, listPermissionsNeeded[i]))
                            pending_permissions.add(listPermissionsNeeded[i])
                        else {
                            Log.i("Go to settings", "and enable permissions")
                            permissionResultCallback?.NeverAskAgain(req_code)
                            Toast.makeText(current_activity, "Go to settings and enable permissions", Toast.LENGTH_LONG).show()
                            return
                        }
                    }

                }

                if (pending_permissions.size > 0) {
                    showMessageOKCancel(dialog_content,
                            DialogInterface.OnClickListener { _, which ->
                                when (which) {
                                    DialogInterface.BUTTON_POSITIVE -> check_permission(permission_list, dialog_content, req_code)
                                    DialogInterface.BUTTON_NEGATIVE -> {
                                        Log.i("permisson", "not fully given")
                                        if (permission_list.size == pending_permissions.size)
                                            permissionResultCallback?.PermissionDenied(req_code)
                                        else
                                            permissionResultCallback?.PartialPermissionGranted(req_code, pending_permissions)
                                    }
                                }
                            })

                } else {
                    Log.i("all", "permissions granted")
                    Log.i("proceed", "to next step")
                    permissionResultCallback?.PermissionGranted(req_code)

                }


            }
        }
    }

    /**
     * Explain why the app needs permissions
     *
     * @param message
     * @param okListener
     */
    private fun showMessageOKCancel(message: String, okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(current_activity)
                .setMessage(message)
                .setPositiveButton("Ok", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show()
    }

    interface PermissionResultCallback {
        fun PermissionGranted(request_code: Int)
        fun PartialPermissionGranted(request_code: Int, granted_permissions: ArrayList<String>)
        fun PermissionDenied(request_code: Int)
        fun NeverAskAgain(request_code: Int)
    }
}