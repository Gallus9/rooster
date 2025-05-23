package com.example.rooster

import com.parse.ParseClassName
import com.parse.ParseObject
import com.parse.ParseUser

@ParseClassName("Order")
class Order : ParseObject() {
    var listing: ParseObject?
        get() = getParseObject("listing")
        set(value) { value?.let { put("listing", it) } }

    var buyer: ParseUser?
        get() = getParseUser("buyer
