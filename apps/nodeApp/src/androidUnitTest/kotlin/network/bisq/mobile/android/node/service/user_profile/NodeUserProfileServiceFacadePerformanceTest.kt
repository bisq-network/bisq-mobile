package network.bisq.mobile.android.node.service.user_profile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.userProfileDemoObj
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis
import kotlin.test.*

/**
 * Performance tests for avatar caching functionality.
 *
 * This test simulates the avatar map behavior from NodeUserProfileServiceFacade
 * without complex dependencies, focusing on the core performance characteristics.
 */
class NodeUserProfileServiceFacadePerformanceTest {

    // Simulate the avatar map from the facade
    private val avatarMap: MutableMap<String, MockPlatformImage?> = mutableMapOf()
    private val avatarMapMutex = Mutex()

    // Simple mock for PlatformImage to avoid dependencies
    data class MockPlatformImage(val id: String, val size: Int = 1024)

    @Before
    fun setup() {
        avatarMap.clear()
    }

    @After
    fun teardown() {
        avatarMap.clear()
    }

    /**
     * Creates a test UserProfileVO with unique nym and realistic data
     */
    private fun createTestUserProfile(index: Int): UserProfileVO {
        return userProfileDemoObj.copy(
            nym = "testUser$index",
            nickName = "TestUser$index",
            userName = "testuser$index"
        )
    }

    /**
     * Simulates avatar generation (the expensive operation)
     */
    private fun generateMockAvatar(nym: String): MockPlatformImage {
        // Simulate some processing time for image generation
        Thread.sleep(1) // 1ms to simulate image generation
        return MockPlatformImage(nym, 1024)
    }

    /**
     * Simulates the avatar caching logic from the facade (without mutex for simple tests)
     */
    private fun getAvatarWithCaching(userProfile: UserProfileVO): MockPlatformImage? {
        return avatarMap[userProfile.nym] ?: run {
            val avatar = generateMockAvatar(userProfile.nym)
            avatarMap[userProfile.nym] = avatar
            avatar
        }
    }

    /**
     * Simulates the avatar caching logic with mutex (thread-safe version)
     */
    private suspend fun getAvatarWithCachingThreadSafe(userProfile: UserProfileVO): MockPlatformImage? {
        return avatarMapMutex.withLock {
            avatarMap[userProfile.nym] ?: run {
                val avatar = generateMockAvatar(userProfile.nym)
                avatarMap[userProfile.nym] = avatar
                avatar
            }
        }
    }

    @Test
    fun `performance test - 100 users sequential avatar loading`() {
        // Given
        val userCount = 100
        val users = (1..userCount).map { createTestUserProfile(it) }

        // When
        val executionTime = measureTimeMillis {
            users.forEach { user ->
                getAvatarWithCaching(user)
            }
        }

        // Then
        println("Sequential loading of $userCount avatars took: ${executionTime}ms")
        assertTrue(executionTime < 5000, "Sequential loading should complete within 5 seconds")
        assertEquals(userCount, avatarMap.size, "All avatars should be cached")

        // Verify cache hits are fast
        val cacheHitTime = measureTimeMillis {
            users.forEach { user ->
                getAvatarWithCaching(user)
            }
        }
        println("Cache hit time for $userCount users: ${cacheHitTime}ms")
        assertTrue(cacheHitTime < executionTime / 5, "Cache hits should be much faster")
    }

    @Test
    fun `performance test - 1000 users avatar cache lookup performance`() {
        // Given
        val userCount = 1000
        val users = (1..userCount).map { createTestUserProfile(it) }

        // Pre-populate cache
        users.forEach { user ->
            getAvatarWithCaching(user)
        }

        // When - Test cache lookup performance
        val lookupTime = measureTimeMillis {
            repeat(5) { // Multiple rounds to test consistency
                users.forEach { user ->
                    getAvatarWithCaching(user)
                }
            }
        }

        // Then
        val averageTimePerLookup = lookupTime.toDouble() / (userCount * 5)
        println("Cache lookup for $userCount users (5 rounds) took: ${lookupTime}ms")
        println("Average time per lookup: ${averageTimePerLookup}ms")

        assertTrue(averageTimePerLookup < 0.5, "Average cache lookup should be under 0.5ms per user")
        assertTrue(lookupTime < 2000, "Total lookup time should be under 2 seconds")
        assertEquals(userCount, avatarMap.size, "All users should be cached")
    }

    @Test
    fun `performance test - memory usage with 1000 cached avatars`() {
        // Given
        val userCount = 1000
        val users = (1..userCount).map { createTestUserProfile(it) }

        // Get initial memory
        val runtime = Runtime.getRuntime()
        runtime.gc() // Suggest garbage collection
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // When - Load all avatars into cache
        users.forEach { user ->
            getAvatarWithCaching(user)
        }

        // Force garbage collection and measure memory
        runtime.gc()
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = finalMemory - initialMemory
        val memoryPerAvatar = memoryUsed.toDouble() / userCount

        // Then
        println("Memory used for $userCount avatars: ${memoryUsed / 1024}KB")
        println("Average memory per avatar: ${memoryPerAvatar / 1024}KB")

        // Reasonable memory usage expectations
        assertTrue(memoryPerAvatar < 10 * 1024, "Memory per avatar should be reasonable")
        assertEquals(userCount, avatarMap.size, "All avatars should be cached")
    }

    @Test
    fun `performance test - offer scenario simulation with 1000 offers from 100 users`() {
        // Given - Realistic scenario: 1000 offers from 100 different users
        val userCount = 100
        val offerCount = 1000
        val users = (1..userCount).map { createTestUserProfile(it) }

        // Create offer-like access pattern (some users have multiple offers)
        val offerMakers = mutableListOf<UserProfileVO>()
        repeat(offerCount) { index ->
            // 80/20 distribution: 20% of users create 80% of offers
            val user = if (index < offerCount * 0.8) {
                users[index % (userCount / 5)] // Top 20% of users
            } else {
                users[(userCount / 5) + (index % (userCount * 4 / 5))] // Remaining 80% of users
            }
            offerMakers.add(user)
        }
        offerMakers.shuffle()

        println("Testing $offerCount offers from $userCount unique users")

        // When - Simulate loading avatars for all offer makers (as would happen in offer list)
        val loadingTime = measureTimeMillis {
            offerMakers.forEach { offer ->
                getAvatarWithCaching(offer)
            }
        }

        // Then
        val averageTimePerOffer = loadingTime.toDouble() / offerCount
        val uniqueUsers = offerMakers.map { it.nym }.toSet()

        println("Avatar loading for $offerCount offers took: ${loadingTime}ms")
        println("Average time per offer: ${averageTimePerOffer}ms")
        println("Unique users in offers: ${uniqueUsers.size}")

        assertTrue(loadingTime < 10000, "Loading avatars for 1000 offers should complete within 10 seconds")
        assertTrue(averageTimePerOffer < 10.0, "Average time per offer should be under 10ms")
        assertEquals(userCount, uniqueUsers.size, "Should have exactly $userCount unique users")
        assertEquals(userCount, avatarMap.size, "Should cache exactly $userCount unique avatars")

        // Verify cache efficiency - subsequent access should be very fast
        val cacheAccessTime = measureTimeMillis {
            offerMakers.forEach { offer ->
                getAvatarWithCaching(offer)
            }
        }

        println("Cache access for same offers took: ${cacheAccessTime}ms")
        assertTrue(cacheAccessTime < loadingTime / 10, "Cache access should be at least 10x faster")
    }

    @Test
    fun `performance test - cache efficiency with duplicate access patterns`() {
        // Given
        val baseUser = createTestUserProfile(1)
        val duplicateUsers = (1..100).map { baseUser } // Same nym for all

        // When
        val executionTime = measureTimeMillis {
            duplicateUsers.forEach { user ->
                getAvatarWithCaching(user)
            }
        }

        // Then
        println("Duplicate nym access (100 requests) took: ${executionTime}ms")
        assertTrue(executionTime < 200, "Duplicate nym access should be very fast")
        assertEquals(1, avatarMap.size, "Should only cache one avatar for duplicate nyms")

        // Verify only one avatar generation occurred (first access)
        val avatar = avatarMap[baseUser.nym]
        assertNotNull(avatar, "Avatar should be cached")
    }

    @Test
    fun `performance test - concurrent avatar loading is thread-safe`() = runBlocking (Dispatchers.Default) {
        val user = createTestUserProfile(42)
        val concurrentJobs = 200
        val time = measureTimeMillis {
            kotlinx.coroutines.coroutineScope {
                repeat(concurrentJobs) {
                    launch {
                        getAvatarWithCachingThreadSafe(user)
                    }
                }
            }
        }
        // Should complete quickly and only one avatar in cache
        assertTrue(time < 200, "Concurrent cache lookups should be fast")
        assertEquals(1, avatarMap.size, "Only one avatar should be cached for the same nym")
    }
}
