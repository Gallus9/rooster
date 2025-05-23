package com.example.rooster

import com.parse.ParseACL
import com.parse.ParseUser

fun setUserRoleAndAcl(
    user: ParseUser,
    role: String,
) {
    user.put("role", role)
    val acl = ParseACL(user)
    acl.setPublicReadAccess(false)
    acl.setPublicWriteAccess(false)
    acl.setReadAccess(user, true)
    acl.setWriteAccess(user, true)
    user.acl = acl
    user.saveInBackground()
}
