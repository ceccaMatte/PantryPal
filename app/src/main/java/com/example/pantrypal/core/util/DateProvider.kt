package com.example.pantrypal.core.util

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface DateProvider {
    fun today(): LocalDate
}

@Singleton
class SystemDateProvider @Inject constructor() : DateProvider {
    override fun today(): LocalDate = LocalDate.now()
}
