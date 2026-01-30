package network.bisq.mobile.domain.utils

import network.bisq.mobile.domain.utils.NetworkUtils.isPrivateIPv4
import network.bisq.mobile.domain.utils.NetworkUtils.isValidIp
import network.bisq.mobile.domain.utils.NetworkUtils.isValidIpv4
import network.bisq.mobile.domain.utils.NetworkUtils.isValidPort
import network.bisq.mobile.domain.utils.NetworkUtils.isValidTorV3Address
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkUtilsTest {
    @Test
    fun `isValidIpv4 returns true for valid IPv4`() {
        assertTrue("192.168.1.1".isValidIpv4())
        assertTrue("10.0.0.1".isValidIpv4())
        assertTrue("172.16.0.1".isValidIpv4())
        assertTrue("255.255.255.255".isValidIpv4())
        assertTrue("0.0.0.0".isValidIpv4())
    }

    @Test
    fun `isValidIpv4 returns false for invalid IPv4`() {
        assertFalse("256.1.1.1".isValidIpv4())
        assertFalse("192.168.1".isValidIpv4())
        assertFalse("192.168.1.1.1".isValidIpv4())
        assertFalse("abc.def.ghi.jkl".isValidIpv4())
        assertFalse("".isValidIpv4())
    }

    @Test
    fun `isValidIp returns true for valid IPv4`() {
        assertTrue("192.168.1.1".isValidIp())
    }

    @Test
    fun `isValidTorV3Address returns true for valid v3 onion`() {
        // V3 onion addresses are exactly 56 characters of base32 (a-z, 2-7) + .onion
        // Using a real-looking v3 onion address format
        val validOnion = "abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopqrstuvwx.onion"
        assertTrue(validOnion.isValidTorV3Address())
    }

    @Test
    fun `isValidTorV3Address returns false for invalid onion`() {
        assertFalse("short.onion".isValidTorV3Address())
        assertFalse("example.com".isValidTorV3Address())
        assertFalse("".isValidTorV3Address())
    }

    @Test
    fun `isValidPort returns true for valid ports`() {
        assertTrue("1".isValidPort())
        assertTrue("80".isValidPort())
        assertTrue("443".isValidPort())
        assertTrue("8080".isValidPort())
        assertTrue("65535".isValidPort())
    }

    @Test
    fun `isValidPort returns false for invalid ports`() {
        assertFalse("0".isValidPort())
        assertFalse("-1".isValidPort())
        assertFalse("65536".isValidPort())
        assertFalse("abc".isValidPort())
        assertFalse("".isValidPort())
    }

    @Test
    fun `isPrivateIPv4 returns true for 10 dot x addresses`() {
        assertTrue("10.0.0.1".isPrivateIPv4())
        assertTrue("10.255.255.255".isPrivateIPv4())
    }

    @Test
    fun `isPrivateIPv4 returns true for 172 dot 16-31 addresses`() {
        assertTrue("172.16.0.1".isPrivateIPv4())
        assertTrue("172.31.255.255".isPrivateIPv4())
    }

    @Test
    fun `isPrivateIPv4 returns false for 172 dot 15 and 172 dot 32`() {
        assertFalse("172.15.0.1".isPrivateIPv4())
        assertFalse("172.32.0.1".isPrivateIPv4())
    }

    @Test
    fun `isPrivateIPv4 returns true for 192 dot 168 addresses`() {
        assertTrue("192.168.0.1".isPrivateIPv4())
        assertTrue("192.168.255.255".isPrivateIPv4())
    }

    @Test
    fun `isPrivateIPv4 returns true for loopback addresses`() {
        assertTrue("127.0.0.1".isPrivateIPv4())
        assertTrue("127.255.255.255".isPrivateIPv4())
    }

    @Test
    fun `isPrivateIPv4 returns true for link-local addresses`() {
        assertTrue("169.254.0.1".isPrivateIPv4())
        assertTrue("169.254.255.255".isPrivateIPv4())
    }

    @Test
    fun `isPrivateIPv4 returns false for public addresses`() {
        assertFalse("8.8.8.8".isPrivateIPv4())
        assertFalse("1.1.1.1".isPrivateIPv4())
        assertFalse("142.250.80.46".isPrivateIPv4())
    }

    @Test
    fun `isPrivateIPv4 returns false for invalid format`() {
        assertFalse("192.168.1".isPrivateIPv4())
        assertFalse("192.168.1.1.1".isPrivateIPv4())
        assertFalse("abc.def.ghi.jkl".isPrivateIPv4())
        assertFalse("".isPrivateIPv4())
    }

    @Test
    fun `isPrivateIPv4 returns false for out of range octets`() {
        assertFalse("256.168.1.1".isPrivateIPv4())
        assertFalse("192.256.1.1".isPrivateIPv4())
    }

    @Test
    fun `isValidIp returns false for invalid IP`() {
        assertFalse("not-an-ip".isValidIp())
        assertFalse("".isValidIp())
    }

    @Test
    fun `isValidPort boundary values`() {
        assertTrue("1".isValidPort())
        assertTrue("65535".isValidPort())
        assertFalse("0".isValidPort())
        assertFalse("65536".isValidPort())
    }

    @Test
    fun `isPrivateIPv4 edge cases for 172 range`() {
        assertTrue("172.16.0.0".isPrivateIPv4())
        assertTrue("172.31.255.255".isPrivateIPv4())
        assertFalse("172.15.255.255".isPrivateIPv4())
        assertFalse("172.32.0.0".isPrivateIPv4())
    }
}
