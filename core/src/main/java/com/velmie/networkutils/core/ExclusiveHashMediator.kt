package com.velmie.networkutils.core

import androidx.lifecycle.MediatorLiveData

class ExclusiveHashMediator<T : Any> : MediatorLiveData<T>() {

    private var lastValueHashCode = 0

    override fun setValue(value: T) {
        if (value.hashCode() != lastValueHashCode) {
            lastValueHashCode = value.hashCode()
            super.setValue(value)
        }
    }
}