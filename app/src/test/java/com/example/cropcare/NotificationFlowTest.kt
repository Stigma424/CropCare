package com.example.cropcare

import org.junit.Assert.*
import org.junit.Test

class NotificationFlowTest {
    @Test fun duplicateDeliveryIsSuppressedButDistinctEventsRemain() {
        val prefix = java.util.UUID.randomUUID().toString()
        assertTrue(PushDeliveryCache.accept(prefix)); assertFalse(PushDeliveryCache.accept(prefix))
        assertTrue(PushDeliveryCache.accept(prefix + "-other")); assertTrue(PushDeliveryCache.accept(null))
        repeat(201) { PushDeliveryCache.accept("$prefix-$it") }
        assertTrue(PushDeliveryCache.accept(prefix))
    }
}
