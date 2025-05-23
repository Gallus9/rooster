package com.example.rooster

import com.parse.ParseClassName
import com.parse.ParseObject
import com.parse.ParseUser

@ParseClassName("Order")
class Order : ParseObject() {
    var listing: ParseObject?
        get() = getParseObject("listing")
        set(value) {
            value?.let { put("listing", it) }
        }

    var buyer: ParseUser?
        get() = getParseUser("buyer")
        set(value) {
            value?.let { put("buyer", it) }
        }

    var seller: ParseUser?
        get() = getParseUser("seller")
        set(value) {
            value?.let { put("seller", it) }
        }

    var status: String?
        get() = getString("status")
        set(value) {
            put("status", value ?: "pending")
        }
}
