package a98apps.pingnoads.network;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * A class that performs some subnet calculations given a network address and a subnet mask.
 * @see "http://www.faqs.org/rfcs/rfc1519.html"
 * @since 2.0
 */
class SubnetUtils {
    private static final String IP_ADDRESS = "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})";
    private static final String SLASH_FORMAT = IP_ADDRESS + "/(\\d{1,3})";
    private static final Pattern addressPattern = Pattern.compile(IP_ADDRESS);
    private static final Pattern cidrPattern = Pattern.compile(SLASH_FORMAT);
    private static final int NBITS = 32;
    private int netmask = 0;
    private int address = 0;
    private int network = 0;
    private int broadcast = 0;
    /** Whether the broadcast/network address are included in host count */
    private boolean inclusiveHostCount = false;
    /**
     * Constructor that takes a CIDR-notation string, e.g. "192.168.0.1/16"
     * @param cidrNotation A CIDR-notation string, e.g. "192.168.0.1/16"
     * @throws IllegalArgumentException if the parameter is invalid,
     * i.e. does not match n.n.n.n/m where n=1-3 decimal digits, m = 1-3 decimal digits in range 1-32
     */
    SubnetUtils(String cidrNotation) {
        calculate(cidrNotation);
    }
    /**
     * Returns <code>true</code> if the return value of {@link SubnetInfo#getAddressCount()}
     * includes the network and broadcast addresses.
     * @since 2.2
     * @return true if the hostcount includes the network and broadcast addresses
     */
    private boolean isInclusiveHostCount() {
        return inclusiveHostCount;
    }
    /**
     * Set to <code>true</code> if you want the return value of {@link SubnetInfo#getAddressCount()}
     * to include the network and broadcast addresses.
     * @since 2.2
     */
    void setInclusiveHostCount() {
        this.inclusiveHostCount = true;
    }
    /**
     * Convenience container for subnet summary information.
     *
     */
    public final class SubnetInfo {
        /* Mask to convert unsigned int to a long (i.e. keep 32 bits) */
        private static final long UNSIGNED_INT_MASK = 0x0FFFFFFFFL;
        private SubnetInfo() {}
        private int netmask()       { return netmask; }
        private int network()       { return network; }
        private int address()       { return address; }
        private int broadcast()     { return broadcast; }
        // long versions of the values (as unsigned int) which are more suitable for range checking
        private long networkLong()  { return network &  UNSIGNED_INT_MASK; }
        private long broadcastLong(){ return broadcast &  UNSIGNED_INT_MASK; }
        private int low() {
            return (isInclusiveHostCount() ? network() :
                    broadcastLong() - networkLong() > 1 ? network() + 1 : 0);
        }
        private int high() {
            return (isInclusiveHostCount() ? broadcast() :
                    broadcastLong() - networkLong() > 1 ? broadcast() -1  : 0);
        }

        String getBroadcastAddress() {
            return format(toArray(broadcast()));
        }
        String getNetworkAddress() {
            return format(toArray(network()));
        }
        String getNetmask() {
            return format(toArray(netmask()));
        }
        /**
         * Return the low address as a dotted IP address.
         * Will be zero for CIDR/31 and CIDR/32 if the inclusive flag is false.
         *
         * @return the IP address in dotted format, may be "0.0.0.0" if there is no valid address
         */
        String getLowAddress() {
            return format(toArray(low()));
        }
        /**
         * Return the high address as a dotted IP address.
         * Will be zero for CIDR/31 and CIDR/32 if the inclusive flag is false.
         *
         * @return the IP address in dotted format, may be "0.0.0.0" if there is no valid address
         */
        String getHighAddress() {
            return format(toArray(high()));
        }
        /**
         * Get the count of available addresses.
         * Will be zero for CIDR/31 and CIDR/32 if the inclusive flag is false.
         * @return the count of addresses, may be zero.
         * @throws RuntimeException if the correct count is greater than {@code Integer.MAX_VALUE}
         * @deprecated (3.4) use {@link #getAddressCountLong()} instead
         */
        @Deprecated
        public int getAddressCount() {
            long countLong = getAddressCountLong();
            if (countLong > Integer.MAX_VALUE) {
                throw new RuntimeException("Count is larger than an integer: " + countLong);
            }
            // N.B. cannot be negative
            return (int)countLong;
        }
        /**
         * Get the count of available addresses.
         * Will be zero for CIDR/31 and CIDR/32 if the inclusive flag is false.
         * @return the count of addresses, may be zero.
         * @since 3.4
         */
        long getAddressCountLong() {
            long b = broadcastLong();
            long n = networkLong();
            long count = b - n + (isInclusiveHostCount() ? 1 : -1);
            return count < 0 ? 0 : count;
        }
        String getCidrSignature() {
            return toCidrNotation(
                    format(toArray(address())),
                    format(toArray(netmask()))
            );
        }

        /**
         * {@inheritDoc}
         * @since 2.2
         */
        @Override
        public String toString() {
            return "CIDR Signature:\t[" + getCidrSignature() + "]" +
                    " Netmask: [" + getNetmask() + "]\n" +
                    "Network:\t[" + getNetworkAddress() + "]\n" +
                    "Broadcast:\t[" + getBroadcastAddress() + "]\n" +
                    "First Address:\t[" + getLowAddress() + "]\n" +
                    "Last Address:\t[" + getHighAddress() + "]\n" +
                    "# Addresses:\t[" + getAddressCount() + "]\n";
        }
    }
    /**
     * Return a {@link SubnetInfo} instance that contains subnet-specific statistics
     * @return new instance
     */
    final SubnetInfo getInfo() { return new SubnetInfo(); }
    /*
     * Initialize the internal fields from the supplied CIDR mask
     */
    private void calculate(String mask) {
        Matcher matcher = cidrPattern.matcher(mask);
        if (matcher.matches()) {
            address = matchAddress(matcher);
            /* Create a binary netmask from the number of bits specification /x */
            int cidrPart = rangeCheck(Integer.parseInt(matcher.group(5)), NBITS);
            for (int j = 0; j < cidrPart; ++j) {
                netmask |= (1 << 31 - j);
            }
            /* Calculate base network address */
            network = (address & netmask);
            /* Calculate broadcast address */
            broadcast = network | ~(netmask);
        } else {
            throw new IllegalArgumentException("Could not parse [" + mask + "]");
        }
    }
    /*
     * Convert a dotted decimal format address to a packed integer format
     */
    private int toInteger(String address) {
        Matcher matcher = addressPattern.matcher(address);
        if (matcher.matches()) {
            return matchAddress(matcher);
        } else {
            throw new IllegalArgumentException("Could not parse [" + address + "]");
        }
    }
    /*
     * Convenience method to extract the components of a dotted decimal address and
     * pack into an integer using a regex match
     */
    private int matchAddress(Matcher matcher) {
        int addr = 0;
        for (int i = 1; i <= 4; ++i) {
            int n = (rangeCheck(Integer.parseInt(matcher.group(i)), 255));
            addr |= ((n & 0xff) << 8*(4-i));
        }
        return addr;
    }
    /*
     * Convert a packed integer address into a 4-element array
     */
    private int[] toArray(int val) {
        int[] ret = new int[4];
        for (int j = 3; j >= 0; --j) {
            ret[j] |= ((val >>> 8*(3-j)) & (0xff));
        }
        return ret;
    }
    /*
     * Convert a 4-element array into dotted decimal format
     */
    private String format(int[] octets) {
        StringBuilder str = new StringBuilder();
        for (int i =0; i < octets.length; ++i){
            str.append(octets[i]);
            if (i != octets.length - 1) {
                str.append(".");
            }
        }
        return str.toString();
    }
    /*
     * Convenience function to check integer boundaries.
     * Checks if a value x is in the range [begin,end].
     * Returns x if it is in range, throws an exception otherwise.
     */
    private int rangeCheck(int value, int end) {
        if (value >= 0 && value <= end) { // (begin,end]
            return value;
        }
        throw new IllegalArgumentException("Value [" + value + "] not in range ["+ 0 +","+end+"]");
    }
    /*
     * Count the number of 1-bits in a 32-bit integer using a divide-and-conquer strategy
     * see Hacker's Delight section 5.1
     */
    private int pop(int x) {
        x = x - ((x >>> 1) & 0x55555555);
        x = (x & 0x33333333) + ((x >>> 2) & 0x33333333);
        x = (x + (x >>> 4)) & 0x0F0F0F0F;
        x = x + (x >>> 8);
        x = x + (x >>> 16);
        return x & 0x0000003F;
    }
    /* Convert two dotted decimal addresses to a single xxx.xxx.xxx.xxx/yy format
     * by counting the 1-bit population in the mask address. (It may be better to count
     * NBITS-#trailing zeroes for this case)
     */
    private String toCidrNotation(String addr, String mask) {
        return addr + "/" + pop(toInteger(mask));
    }
}